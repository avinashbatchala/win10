package com.ab.livetile.providers

import android.content.Context
import android.util.Log
import com.ab.livetile.api.LiveTileProvider
import com.ab.livetile.model.LiveTileFace
import com.ab.livetile.model.LiveTileState
import com.ab.livetile.model.LiveTileTemplate
import com.ab.model.TileSize
import com.ab.ui.icons.MetroIcons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/**
 * Launcher-owned weather Live Tile.
 *
 * Data is fully automatic and keyless:
 *  1. Approximate the device's location from its public IP (ipwho.is, then geojs fallback).
 *  2. Fetch current conditions + today's high/low from Open-Meteo.
 *
 * No runtime location permission is required, and no API key is bundled. Matches any
 * installed package whose name contains "weather" (e.g. a third-party weather app) as well
 * as the launcher-owned "Weather" system tile.
 */
class WeatherLiveTileProvider : LiveTileProvider {

    companion object {
        const val WEATHER_PACKAGE = "livetile.demo.weather"
        private const val TAG = "WeatherTile"
        private const val LOCATION_TTL_MS = 6 * 60 * 60 * 1000L
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

    @Volatile
    private var cachedLatitude: Double? = null

    @Volatile
    private var cachedLongitude: Double? = null

    @Volatile
    private var cachedCity: String? = null

    @Volatile
    private var locationFetchedAt: Long = 0L

    override fun matchesComponent(packageName: String, activityName: String?): Boolean {
        val pkg = packageName.lowercase()
        return pkg == WEATHER_PACKAGE || pkg.contains("weather")
    }

    override suspend fun getLiveTileState(context: Context, tileSize: TileSize): LiveTileState? =
        withContext(Dispatchers.IO) {
            try {
                val location = resolveLocation() ?: return@withContext null
                val snapshot = fetchWeather(location.first, location.second) ?: return@withContext null
                buildState(snapshot, tileSize)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to build weather tile", e)
                null
            }
        }

    private data class WeatherSnapshot(
        val temperatureC: Double,
        val weatherCode: Int,
        val humidity: Int,
        val windKmh: Double,
        val maxC: Double,
        val minC: Double,
        val city: String?
    )

    private fun resolveLocation(): Pair<Double, Double>? {
        val now = System.currentTimeMillis()
        val lat = cachedLatitude
        val lon = cachedLongitude
        if (lat != null && lon != null && now - locationFetchedAt < LOCATION_TTL_MS) {
            return lat to lon
        }

        // Primary: ipwho.is (free, keyless)
        fetchJson("https://ipwho.is/")?.let { body ->
            try {
                val json = JSONObject(body)
                if (json.optBoolean("success", true)) {
                    val latitude = json.optDouble("latitude", Double.NaN)
                    val longitude = json.optDouble("longitude", Double.NaN)
                    if (!latitude.isNaN() && !longitude.isNaN()) {
                        cacheLocation(latitude, longitude, json.optString("city").ifBlank { null })
                        return latitude to longitude
                    }
                }
            } catch (_: Exception) {
            }
        }

        // Fallback: geojs (free, keyless)
        fetchJson("https://get.geojs.io/v1/ip/geo.json")?.let { body ->
            try {
                val json = JSONObject(body)
                val latitude = json.optString("latitude").toDoubleOrNull()
                val longitude = json.optString("longitude").toDoubleOrNull()
                if (latitude != null && longitude != null) {
                    cacheLocation(latitude, longitude, json.optString("city").ifBlank { null })
                    return latitude to longitude
                }
            } catch (_: Exception) {
            }
        }

        return null
    }

    private fun cacheLocation(latitude: Double, longitude: Double, city: String?) {
        cachedLatitude = latitude
        cachedLongitude = longitude
        cachedCity = city
        locationFetchedAt = System.currentTimeMillis()
    }

    private fun fetchWeather(latitude: Double, longitude: Double): WeatherSnapshot? {
        val url = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=$latitude&longitude=$longitude" +
            "&current=temperature_2m,weather_code,relative_humidity_2m,wind_speed_10m" +
            "&daily=temperature_2m_max,temperature_2m_min&timezone=auto&forecast_days=1"
        val body = fetchJson(url) ?: return null
        return try {
            val json = JSONObject(body)
            val current = json.getJSONObject("current")
            val daily = json.getJSONObject("daily")
            WeatherSnapshot(
                temperatureC = current.optDouble("temperature_2m", Double.NaN),
                weatherCode = current.optInt("weather_code", 0),
                humidity = current.optInt("relative_humidity_2m", 0),
                windKmh = current.optDouble("wind_speed_10m", 0.0),
                maxC = daily.optJSONArray("temperature_2m_max")?.optDouble(0, Double.NaN) ?: Double.NaN,
                minC = daily.optJSONArray("temperature_2m_min")?.optDouble(0, Double.NaN) ?: Double.NaN,
                city = cachedCity
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

    private fun buildState(snapshot: WeatherSnapshot, tileSize: TileSize): LiveTileState {
        val (description, icon) = describe(snapshot.weatherCode)
        val temperature = if (snapshot.temperatureC.isNaN()) "--" else "${snapshot.temperatureC.roundToInt()}°"
        val high = if (snapshot.maxC.isNaN()) "--" else "${snapshot.maxC.roundToInt()}°"
        val low = if (snapshot.minC.isNaN()) "--" else "${snapshot.minC.roundToInt()}°"
        val label = snapshot.city ?: "Weather"

        val currentFace = when (tileSize) {
            TileSize.SMALL -> LiveTileFace(
                template = LiveTileTemplate.PRIMARY_TEXT,
                primaryText = temperature,
                iconVector = icon,
                accessibilityDescription = "$description, $temperature"
            )
            TileSize.MEDIUM -> LiveTileFace(
                template = LiveTileTemplate.PRIMARY_TEXT,
                primaryText = temperature,
                secondaryText = description,
                iconVector = icon,
                labelOverride = label,
                accessibilityDescription = "$description, $temperature in $label"
            )
            TileSize.WIDE, TileSize.LARGE -> LiveTileFace(
                template = LiveTileTemplate.TEXT_LINES,
                primaryText = temperature,
                secondaryText = description,
                textLines = listOf(
                    description,
                    "High $high  Low $low",
                    "Humidity ${snapshot.humidity}%",
                    "Wind ${snapshot.windKmh.roundToInt()} km/h"
                ),
                iconVector = icon,
                labelOverride = label,
                accessibilityDescription = "$description, $temperature in $label"
            )
        }

        // Second face for the Windows-style flip, showing today's range.
        val forecastFace = LiveTileFace(
            template = if (tileSize == TileSize.SMALL) {
                LiveTileTemplate.PRIMARY_TEXT
            } else {
                LiveTileTemplate.TEXT_LINES
            },
            primaryText = high,
            secondaryText = "High",
            textLines = listOf("Low $low", description),
            iconVector = icon,
            labelOverride = label,
            accessibilityDescription = "High $high, low $low, $description"
        )

        val faces = if (tileSize == TileSize.SMALL) listOf(currentFace) else listOf(currentFace, forecastFace)
        return LiveTileState(
            providerId = providerId,
            faces = faces,
            validityDurationMs = refreshIntervalMs
        )
    }

    private fun describe(code: Int): Pair<String, androidx.compose.ui.graphics.vector.ImageVector> = when (code) {
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
