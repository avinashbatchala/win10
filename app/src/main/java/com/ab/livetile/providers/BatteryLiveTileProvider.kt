package com.ab.livetile.providers

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.ab.livetile.api.LiveTileProvider
import com.ab.livetile.model.LiveTileFace
import com.ab.livetile.model.LiveTileState
import com.ab.livetile.model.LiveTileTemplate
import com.ab.model.TileSize
import com.ab.ui.icons.MetroIcons

class BatteryLiveTileProvider : LiveTileProvider {

    override val providerId: String = "livetile.system.battery"
    override val displayName: String = "Battery Live Tile"
    override val refreshIntervalMs: Long = 60_000L

    override fun matchesComponent(packageName: String, activityName: String?): Boolean {
        val pkg = packageName.lowercase()
        return pkg == "livetile.demo.battery"
    }

    override suspend fun getLiveTileState(context: Context, tileSize: TileSize): LiveTileState? {
        val intent = try {
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        } catch (_: Exception) {
            null
        }

        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 100
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
        val pct = if (scale > 0) ((level / scale.toFloat()) * 100).toInt() else level

        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val statusText = if (isCharging) "Charging" else "On battery"

        // Face 1: Large battery percentage count
        val face1 = LiveTileFace(
            template = LiveTileTemplate.COUNT,
            primaryText = "$pct%",
            secondaryText = "remaining",
            iconVector = MetroIcons.Settings,
            accessibilityDescription = "Battery $pct percent, $statusText"
        )

        // Face 2: Charging state text
        val face2 = LiveTileFace(
            template = LiveTileTemplate.PRIMARY_TEXT,
            primaryText = statusText,
            secondaryText = "$pct% charged",
            iconVector = MetroIcons.Settings,
            accessibilityDescription = "Battery status: $statusText, $pct percent"
        )

        return LiveTileState(
            providerId = providerId,
            faces = listOf(face1, face2),
            validityDurationMs = 60_000L
        )
    }
}
