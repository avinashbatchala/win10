package com.ab.livetile.providers

import android.content.Context
import com.ab.livetile.api.LiveTileProvider
import com.ab.livetile.model.LiveTileFace
import com.ab.model.TileModel
import com.ab.livetile.model.LiveTileState
import com.ab.livetile.model.LiveTileTemplate
import com.ab.model.TileSize
import com.ab.ui.icons.MetroIcons
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ClockLiveTileProvider : LiveTileProvider {

    override val providerId: String = "livetile.system.clock"
    override val displayName: String = "Clock Live Tile"
    override val refreshIntervalMs: Long = 60_000L

    override fun matchesComponent(packageName: String, activityName: String?): Boolean {
        val pkg = packageName.lowercase()
        val act = activityName?.lowercase() ?: ""
        return pkg == "livetile.demo.clock" ||
                pkg.contains("deskclock") ||
                pkg.contains("clock") ||
                act.contains("deskclock") ||
                act.contains("clock")
    }

    override suspend fun getLiveTileState(context: Context, tileSize: TileSize, tile: TileModel?): LiveTileState? {
        val now = Date()
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val timeStr = timeFormat.format(now)

        val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
        val dayStr = dayFormat.format(now)

        val dateFormat = SimpleDateFormat("MMMM d", Locale.getDefault())
        val dateStr = dateFormat.format(now)

        val face = when (tileSize) {
            TileSize.SMALL -> {
                LiveTileFace(
                    template = LiveTileTemplate.PRIMARY_TEXT,
                    primaryText = timeStr,
                    accessibilityDescription = "Time $timeStr"
                )
            }
            TileSize.MEDIUM -> {
                LiveTileFace(
                    template = LiveTileTemplate.PRIMARY_TEXT,
                    primaryText = timeStr,
                    secondaryText = dayStr,
                    iconVector = MetroIcons.Clock,
                    accessibilityDescription = "Time $timeStr, $dayStr"
                )
            }
            TileSize.WIDE -> {
                LiveTileFace(
                    template = LiveTileTemplate.PRIMARY_TEXT,
                    primaryText = timeStr,
                    secondaryText = dayStr,
                    tertiaryText = dateStr,
                    iconVector = MetroIcons.Clock,
                    accessibilityDescription = "Time $timeStr, $dayStr, $dateStr"
                )
            }
            TileSize.LARGE -> {
                LiveTileFace(
                    template = LiveTileTemplate.TEXT_LINES,
                    primaryText = timeStr,
                    secondaryText = "$dayStr, $dateStr",
                    textLines = listOf(timeStr, dayStr, dateStr),
                    iconVector = MetroIcons.Clock,
                    accessibilityDescription = "Time $timeStr, $dayStr, $dateStr"
                )
            }
        }

        return LiveTileState(
            providerId = providerId,
            faces = listOf(face),
            validityDurationMs = 60_000L
        )
    }
}
