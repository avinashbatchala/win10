package com.ab.livetile.providers

import android.content.Context
import android.util.Log
import androidx.compose.ui.graphics.vector.ImageVector
import com.ab.livetile.api.LiveTileProvider
import com.ab.livetile.model.LiveTileFace
import com.ab.livetile.model.LiveTileState
import com.ab.livetile.model.LiveTileTemplate
import com.ab.livetile.model.WeatherDay
import com.ab.livetile.model.WeatherTileData
import com.ab.model.TileModel
import com.ab.model.TileSize
import com.ab.ui.icons.MetroIcons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/**
 * Launcher-owned Windows 10 Mobile weather Live Tile.
 *
 * When the tile carries a pinned city (latitude/longitude/timezone from MetroWeather) that
 * city's weather is fetched directly from Open-Meteo, so a pinned city is never mixed with an
 * IP-detected one. The generic launcher-owned Weather tile (no coordinates) falls back to
 * approximating the device location from its public IP.
 *
 * Faces follow the WP10 MSN Weather tile: current conditions, flipping to a short forecast
 * (three days on wide/large tiles).
 */
class WeatherLiveTileProvider : LiveTileProvider {

    companion object {
        const val WEATHER_PACKAGE = "livetile.demo.weather"
        private const val TAG = "WeatherTile"
        private const val LOCATION_TTL_MS = 6 * 60 * 60 * 1000L
        private const val WEATHER_TTL_MS = 30 * 60 * 1000L

        // Keyless fallback so the generic Weather tile is never blank when IP geolocation fails.
        private val FALLBACK_LOCATION = Triple(47.674, -122.1215, "Redmond")
    }

    override val providerId: String = "livetile.system.weather"
    override val displayName: String = "Weather Live Tile"
    override val refreshIntervalMs: Long = 30 * 60 * 1000L

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    private data class DaySnapshot(
        val date: String,
        val code: Int,
        val maxC: Double,
        val minC: Double,
        val precipChance: Int?
    )

    private data class WeatherSnapshot(
        val temperatureC: Double,
        val weatherCode: Int,
        val humidity: Int,
        val windKmh: Double,
        val maxC: Double,
        val minC: Double,
        val city: String?,
        val days: List<DaySnapshot>
    )

    private data class Cached(val snapshot: WeatherSnapshot, val fetchedAt: Long)

    // Per-tile cache: a re-pinned city (new tile id) never sees the previous city's data.
    private val cache = ConcurrentHashMap<String, Cached>()

    @Volatile
    private var ipLatitude: Double? = null

    @Volatile
    private var ipLongitude: Double? = null

    @Volatile
    private var ipCity: String? = null

    @Volatile
    private var ipFetchedAt: Long = 0L

    override fun matchesComponent(packageName: String, activityName: String?): Boolean {
        val pkg = packageName.lowercase()
        return pkg == WEATHER_PACKAGE || pkg.contains("weather")
    }

    override suspend fun getLiveTileState(
        context: Context,
        tileSize: TileSize,
        tile: TileModel?
    ): LiveTileState? = withContext(Dispatchers.IO) {
        try {
            val key = tile?.id ?: "default"
            val cached = cache[key]
            val now = System.currentTimeMillis()
            val snapshot = if (cached != null && now - cached.fetchedAt < WEATHER_TTL_MS) {
                cached.snapshot
            } else {
                val coords = resolveCoordinates(tile) ?: return@withContext null
                val fresh = fetchWeather(coords.first, coords.second, coords.third) ?: return@withContext null
                cache[key] = Cached(fresh, now)
                fresh
            }
            buildState(snapshot, tileSize, tile)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to build weather tile", e)
            null
        }
    }

    /**
     * Resolves the coordinates to use for a tile: the pinned city when present, otherwise the
     * IP-approximated device location. Returns (lat, lon, timezoneCity).
     */
    private fun resolveCoordinates(tile: TileModel?): Triple<Double, Double, String?>? {
        val lat = tile?.weatherLat
        val lon = tile?.weatherLon
        if (lat != null && lon != null) {
            return Triple(lat, lon, null)
        }
        val ip = resolveIpLocation() ?: return FALLBACK_LOCATION
        return Triple(ip.first, ip.second, ipCity)
    }

    private fun resolveIpLocation(): Pair<Double, Double>? {
        val now = System.currentTimeMillis()
        val lat = ipLatitude
        val lon = ipLongitude
        if (lat != null && lon != null && now - ipFetchedAt < LOCATION_TTL_MS) {
            return lat to lon
        }

        fetchJson("https://ipwho.is/")?.let { body ->
            try {
                val json = JSONObject(body)
                if (json.optBoolean("success", true)) {
                    val latitude = json.optDouble("latitude", Double.NaN)
                    val longitude = json.optDouble("longitude", Double.NaN)
                    if (!latitude.isNaN() && !longitude.isNaN()) {
                        cacheIp(latitude, longitude, json.optString("city").ifBlank { null })
                        return latitude to longitude
                    }
                }
            } catch (_: Exception) {
            }
        }

        fetchJson("https://get.geojs.io/v1/ip/geo.json")?.let { body ->
            try {
                val json = JSONObject(body)
                val latitude = json.optString("latitude").toDoubleOrNull()
                val longitude = json.optString("longitude").toDoubleOrNull()
                if (latitude != null && longitude != null) {
                    cacheIp(latitude, longitude, json.optString("city").ifBlank { null })
                    return latitude to longitude
                }
            } catch (_: Exception) {
            }
        }

        return null
    }

    private fun cacheIp(latitude: Double, longitude: Double, city: String?) {
        ipLatitude = latitude
        ipLongitude = longitude
        ipCity = city
        ipFetchedAt = System.currentTimeMillis()
    }

    private fun fetchWeather(latitude: Double, longitude: Double, timezone: String?): WeatherSnapshot? {
        val tz = timezone?.takeIf { it.isNotBlank() } ?: "auto"
        val url = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=$latitude&longitude=$longitude" +
            "&current=temperature_2m,weather_code,relative_humidity_2m,wind_speed_10m" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max" +
            "&timezone=$tz&forecast_days=3"
        val body = fetchJson(url) ?: return null
        return try {
            val json = JSONObject(body)
            val current = json.getJSONObject("current")
            val daily = json.getJSONObject("daily")

            val times = daily.optJSONArray("time")
            val codes = daily.optJSONArray("weather_code")
            val maxes = daily.optJSONArray("temperature_2m_max")
            val mins = daily.optJSONArray("temperature_2m_min")
            val precips = daily.optJSONArray("precipitation_probability_max")

            val days = buildList {
                val count = times?.length() ?: 0
                for (i in 0 until count) {
                    val date = times?.optString(i).orEmpty()
                    add(
                        DaySnapshot(
                            date = date,
                            code = codes?.optInt(i, 0) ?: 0,
                            maxC = maxes?.optDouble(i, Double.NaN) ?: Double.NaN,
                            minC = mins?.optDouble(i, Double.NaN) ?: Double.NaN,
                            precipChance = precips?.optInt(i, 0)
                        )
                    )
                }
            }

            WeatherSnapshot(
                temperatureC = current.optDouble("temperature_2m", Double.NaN),
                weatherCode = current.optInt("weather_code", 0),
                humidity = current.optInt("relative_humidity_2m", 0),
                windKmh = current.optDouble("wind_speed_10m", 0.0),
                maxC = maxes?.optDouble(0, Double.NaN) ?: Double.NaN,
                minC = mins?.optDouble(0, Double.NaN) ?: Double.NaN,
                city = null,
                days = days
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse weather response", e)
            null
        }
    }

    private fun fetchJson(url: String): String? {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Win10StartLauncher/1.0")
                .build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) null else response.body?.string()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Network request failed: $url", e)
            null
        }
    }

    private fun buildState(snapshot: WeatherSnapshot, tileSize: TileSize, tile: TileModel?): LiveTileState {
        val (description, icon) = describe(snapshot.weatherCode)
        val temperature = if (snapshot.temperatureC.isNaN()) "--" else "${snapshot.temperatureC.roundToInt()}°"
        val high = if (snapshot.maxC.isNaN()) "--" else "${snapshot.maxC.roundToInt()}°"
        val low = if (snapshot.minC.isNaN()) "--" else "${snapshot.minC.roundToInt()}°"

        // For a pinned tile the tile's own label is the city; only the generic launcher tile
        // (no coordinates) uses the IP-detected city.
        val isPinnedCity = tile?.weatherLat != null && tile.weatherLon != null
        val labelOverride = if (isPinnedCity) null else ipCity
        val cityForAccessibility = tile?.label ?: ipCity ?: "Weather"

        val days = snapshot.days.map { day ->
            val (dayDesc, dayIcon) = describe(day.code)
            WeatherDay(
                label = weekdayLabel(day.date),
                icon = dayIcon,
                highText = if (day.maxC.isNaN()) "--" else "${day.maxC.roundToInt()}°",
                lowText = if (day.minC.isNaN()) "--" else "${day.minC.roundToInt()}°",
                precipChance = day.precipChance
            )
        }

        val currentData = WeatherTileData(
            temperatureText = temperature,
            conditionText = description,
            icon = icon,
            highText = high,
            lowText = low,
            days = days
        )

        val currentFace = LiveTileFace(
            template = LiveTileTemplate.WEATHER,
            weather = currentData,
            labelOverride = labelOverride,
            accessibilityDescription = "$description, $temperature in $cityForAccessibility"
        )

        val forecastFace = LiveTileFace(
            template = LiveTileTemplate.WEATHER,
            weather = WeatherTileData(
                temperatureText = high,
                conditionText = "Low $low",
                icon = icon,
                highText = high,
                lowText = low,
                days = days
            ),
            labelOverride = labelOverride,
            accessibilityDescription = "High $high, low $low, $description"
        )

        val faces = if (tileSize == TileSize.SMALL) listOf(currentFace) else listOf(currentFace, forecastFace)
        return LiveTileState(
            providerId = providerId,
            faces = faces,
            validityDurationMs = refreshIntervalMs
        )
    }

    private fun weekdayLabel(date: String): String {
        if (date.isBlank()) return ""
        return try {
            val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date) ?: return date
            SimpleDateFormat("EEE", Locale.getDefault()).format(parsed)
        } catch (_: Exception) {
            date
        }
    }

    private fun describe(code: Int): Pair<String, ImageVector> = when (code) {
        0 -> "Clear" to MetroIcons.WeatherSun
        1 -> "Mainly clear" to MetroIcons.WeatherSun
        2 -> "Partly cloudy" to MetroIcons.WeatherCloud
        3 -> "Overcast" to MetroIcons.WeatherCloud
        45, 48 -> "Fog" to MetroIcons.WeatherFog
        51, 53, 55 -> "Drizzle" to MetroIcons.WeatherRain
        56, 57 -> "Freezing drizzle" to MetroIcons.WeatherRain
        61 -> "Light rain" to MetroIcons.WeatherRain
        63 -> "Rain" to MetroIcons.WeatherRain
        65 -> "Heavy rain" to MetroIcons.WeatherRain
        66, 67 -> "Freezing rain" to MetroIcons.WeatherRain
        71 -> "Light snow" to MetroIcons.WeatherSnow
        73 -> "Snow" to MetroIcons.WeatherSnow
        75 -> "Heavy snow" to MetroIcons.WeatherSnow
        77 -> "Snow grains" to MetroIcons.WeatherSnow
        80 -> "Rain showers" to MetroIcons.WeatherRain
        81 -> "Rain showers" to MetroIcons.WeatherRain
        82 -> "Violent showers" to MetroIcons.WeatherRain
        85, 86 -> "Snow showers" to MetroIcons.WeatherSnow
        95 -> "Thunderstorm" to MetroIcons.WeatherStorm
        96, 99 -> "Thunderstorm, hail" to MetroIcons.WeatherStorm
        else -> "Weather" to MetroIcons.WeatherCloud
    }
}
