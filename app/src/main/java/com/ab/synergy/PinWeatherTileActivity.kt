package com.ab.synergy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.ab.data.LauncherPreferences
import com.ab.model.TileModel
import com.ab.model.TileSize
import com.ab.ui.settings.SystemTiles
import com.ab.ui.viewmodel.GridManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Entry point used by the MetroWeather companion app's "pin to start" action.
 *
 * Runs as a transparent, no-UI activity so it can safely write a weather tile through the
 * same DataStore the launcher uses, whether or not the launcher process is running.
 * The caller must hold [PERMISSION_PIN_WEATHER_TILE] (declared as a normal permission).
 */
class PinWeatherTileActivity : ComponentActivity() {

    companion object {
        const val ACTION_PIN_WEATHER_TILE = "com.ab.action.PIN_WEATHER_TILE"
        const val PERMISSION_PIN_WEATHER_TILE = "com.ab.permission.PIN_WEATHER_TILE"
        const val EXTRA_PLACE_LABEL = "place_label"
        const val EXTRA_SIZE = "size"
        const val EXTRA_LATITUDE = "lat"
        const val EXTRA_LONGITUDE = "lon"
        const val EXTRA_TIMEZONE = "tz"
        const val EXTRA_LOCATION_ID = "lid"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val placeLabel = intent.getStringExtra(EXTRA_PLACE_LABEL)
        val requestedSize = intent.getStringExtra(EXTRA_SIZE)
        val latitude = if (intent.hasExtra(EXTRA_LATITUDE)) intent.getDoubleExtra(EXTRA_LATITUDE, Double.NaN) else null
        val longitude = if (intent.hasExtra(EXTRA_LONGITUDE)) intent.getDoubleExtra(EXTRA_LONGITUDE, Double.NaN) else null
        val timezone = intent.getStringExtra(EXTRA_TIMEZONE)
        val locationId = intent.getStringExtra(EXTRA_LOCATION_ID)

        lifecycleScope.launch {
            try {
                val prefs = LauncherPreferences(applicationContext)
                val current = prefs.pinnedTilesFlow.first().orEmpty().toMutableList()
                android.util.Log.d("PinWeatherTile", "pin request place=$placeLabel size=$requestedSize existing=${current.size}")

                val alreadyPinned = current.any { it.packageName == SystemTiles.WEATHER_PACKAGE }
                if (!alreadyPinned) {
                    val size = when (requestedSize) {
                        "wide" -> TileSize.WIDE
                        "small" -> TileSize.SMALL
                        else -> TileSize.MEDIUM
                    }
                    val totalColumns = if (prefs.settingsFlow.first().showMoreTiles) 8 else 6
                    val (col, row) = GridManager.findFirstAvailablePosition(
                        cols = size.cols,
                        rows = size.rows,
                        tiles = current,
                        totalColumns = totalColumns
                    )
                    current.add(
                        TileModel(
                            id = UUID.randomUUID().toString(),
                            packageName = SystemTiles.WEATHER_PACKAGE,
                            label = placeLabel?.takeIf { it.isNotBlank() } ?: "Weather",
                            size = size,
                            col = col,
                            row = row,
                            order = (current.maxOfOrNull { it.order } ?: 0) + 1,
                            weatherLat = latitude?.takeIf { !it.isNaN() },
                            weatherLon = longitude?.takeIf { !it.isNaN() },
                            weatherTimezone = timezone,
                            weatherLocationId = locationId
                        )
                    )
                    prefs.savePinnedTiles(current)
                    android.util.Log.d("PinWeatherTile", "pinned weather tile at ($col,$row) size=$size; total=${current.size}")
                } else {
                    android.util.Log.d("PinWeatherTile", "weather tile already pinned")
                }
            } catch (e: Exception) {
                android.util.Log.e("PinWeatherTile", "pin failed", e)
            } finally {
                finish()
            }
        }
    }
}
