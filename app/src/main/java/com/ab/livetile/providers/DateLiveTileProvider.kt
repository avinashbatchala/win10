package com.ab.livetile.providers

import android.content.Context
import com.ab.livetile.api.LiveTileProvider
import com.ab.livetile.model.LiveTileFace
import com.ab.livetile.model.LiveTileState
import com.ab.livetile.model.LiveTileTemplate
import com.ab.model.TileSize
import com.ab.ui.icons.MetroIcons
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DateLiveTileProvider : LiveTileProvider {

    override val providerId: String = "livetile.system.date"
    override val displayName: String = "Calendar Date Live Tile"
    override val refreshIntervalMs: Long = 300_000L // 5 minutes

    override fun matchesComponent(packageName: String, activityName: String?): Boolean {
        val pkg = packageName.lowercase()
        val act = activityName?.lowercase() ?: ""
        return pkg == "livetile.demo.date" ||
                pkg.contains("calendar") ||
                act.contains("calendar")
    }

    override suspend fun getLiveTileState(context: Context, tileSize: TileSize): LiveTileState? {
        val now = Date()
        val dayNumFormat = SimpleDateFormat("d", Locale.getDefault())
        val dayNumStr = dayNumFormat.format(now)

        val weekdayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
        val weekdayStr = weekdayFormat.format(now)

        val monthFormat = SimpleDateFormat("MMMM", Locale.getDefault())
        val monthStr = monthFormat.format(now)

        val face = when (tileSize) {
            TileSize.SMALL -> {
                LiveTileFace(
                    template = LiveTileTemplate.DATE,
                    primaryText = dayNumStr,
                    accessibilityDescription = "Date $dayNumStr"
                )
            }
            TileSize.MEDIUM -> {
                LiveTileFace(
                    template = LiveTileTemplate.DATE,
                    primaryText = dayNumStr,
                    secondaryText = weekdayStr,
                    tertiaryText = monthStr,
                    iconVector = MetroIcons.Calendar,
                    accessibilityDescription = "$weekdayStr, $monthStr $dayNumStr"
                )
            }
            TileSize.WIDE -> {
                LiveTileFace(
                    template = LiveTileTemplate.DATE,
                    primaryText = dayNumStr,
                    secondaryText = weekdayStr,
                    tertiaryText = monthStr,
                    textLines = listOf("No upcoming events"),
                    iconVector = MetroIcons.Calendar,
                    accessibilityDescription = "$weekdayStr, $monthStr $dayNumStr, No upcoming events"
                )
            }
            TileSize.LARGE -> {
                LiveTileFace(
                    template = LiveTileTemplate.DATE,
                    primaryText = dayNumStr,
                    secondaryText = weekdayStr,
                    tertiaryText = monthStr,
                    textLines = listOf("Today's schedule", "No upcoming appointments"),
                    iconVector = MetroIcons.Calendar,
                    accessibilityDescription = "$weekdayStr, $monthStr $dayNumStr, No upcoming appointments"
                )
            }
        }

        return LiveTileState(
            providerId = providerId,
            faces = listOf(face),
            validityDurationMs = 300_000L
        )
    }
}
