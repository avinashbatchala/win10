package com.ab.livetile.providers

import android.content.Context
import android.net.Uri
import com.ab.livetile.api.LiveTileProvider
import com.ab.livetile.model.LiveClock
import com.ab.livetile.model.LiveClockKind
import com.ab.livetile.model.LiveTileFace
import com.ab.livetile.model.LiveTileState
import com.ab.livetile.model.LiveTileTemplate
import com.ab.model.TileModel
import com.ab.model.TileSize
import com.ab.ui.icons.MetroIcons
import com.metro.livetile.contract.ClockRunState
import com.metro.livetile.contract.ClockTileState
import com.metro.livetile.contract.MetroLiveTileProtocol
import com.metro.livetile.contract.toClockTileState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Reads MetroClock's signature-protected live tile state. */
object MetroClockTileClient {
    val URI: Uri = Uri.parse("content://${MetroLiveTileProtocol.CLOCK_AUTHORITY}")
    const val PROVIDER_ID = "livetile.metro.clock"

    fun query(context: Context): ClockTileState? = try {
        context.contentResolver
            .call(URI, MetroLiveTileProtocol.METHOD_GET_CLOCK_STATE, null, null)
            ?.toClockTileState()
    } catch (_: Exception) {
        null
    }
}

/**
 * Turns MetroClock's domain state into generic Metro tile faces. Only state transitions are
 * fetched over IPC; active timer/stopwatch countdowns are interpolated locally by the renderer
 * via [LiveClock].
 */
class MetroClockLiveTileProvider : LiveTileProvider {

    override val providerId: String = MetroClockTileClient.PROVIDER_ID
    override val displayName: String = "MetroClock"
    override val refreshIntervalMs: Long = 60_000L

    override fun matchesComponent(packageName: String, activityName: String?): Boolean =
        packageName.contains("metroclock", ignoreCase = true)

    override suspend fun getLiveTileState(
        context: Context,
        tileSize: TileSize,
        tile: TileModel?
    ): LiveTileState? {
        val state = MetroClockTileClient.query(context) ?: return null
        return mapToState(state, tileSize, tile?.label ?: "Clock")
    }

    private fun mapToState(state: ClockTileState, size: TileSize, label: String): LiveTileState {
        val now = Date(state.currentEpochMillis.takeIf { it > 0L } ?: System.currentTimeMillis())
        val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
        val timeStr = timeFmt.format(now)
        val dayFmt = SimpleDateFormat("EEEE", Locale.getDefault())
        val dayStr = dayFmt.format(now)
        val dayDateFmt = SimpleDateFormat("EEEE d", Locale.getDefault())
        val dayDateStr = dayDateFmt.format(now)
        val longDateFmt = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())
        val longDateStr = longDateFmt.format(now)

        val alarm = state.nextAlarm?.takeIf { it.enabled }
        val alarmStr = alarm?.let { "next alarm ${timeFmt.format(Date(it.triggerEpochMillis))}" }

        val timer = state.timer
        val stopwatch = state.stopwatch

        val face = when {
            timer != null -> {
                val live = LiveClock(
                    kind = LiveClockKind.COUNTDOWN,
                    endElapsedRealtime = timer.endElapsedRealtime,
                    paused = timer.state == ClockRunState.PAUSED,
                    pausedRemainingMillis = timer.remainingWhenPausedMillis
                )
                val initial = formatInitialRemaining(timer.state, timer.endElapsedRealtime, timer.remainingWhenPausedMillis)
                LiveTileFace(
                    template = LiveTileTemplate.PRIMARY_TEXT,
                    primaryText = initial,
                    secondaryText = "TIMER",
                    tertiaryText = timer.label,
                    iconVector = MetroIcons.Clock,
                    liveClock = live,
                    accessibilityDescription = "Timer $initial"
                )
            }
            stopwatch != null -> {
                val live = LiveClock(
                    kind = LiveClockKind.STOPWATCH,
                    startElapsedRealtime = stopwatch.startElapsedRealtime,
                    accumulatedMillis = stopwatch.accumulatedElapsedMillis,
                    paused = stopwatch.state == ClockRunState.PAUSED
                )
                LiveTileFace(
                    template = LiveTileTemplate.PRIMARY_TEXT,
                    primaryText = "00:00.0",
                    secondaryText = "STOPWATCH",
                    iconVector = MetroIcons.Clock,
                    liveClock = live,
                    accessibilityDescription = "Stopwatch"
                )
            }
            else -> when (size) {
                TileSize.SMALL -> LiveTileFace(
                    template = LiveTileTemplate.PRIMARY_TEXT,
                    primaryText = timeStr,
                    iconVector = MetroIcons.Clock,
                    accessibilityDescription = "Time $timeStr"
                )
                TileSize.MEDIUM -> LiveTileFace(
                    template = LiveTileTemplate.PRIMARY_TEXT,
                    primaryText = timeStr,
                    secondaryText = dayDateStr,
                    iconVector = MetroIcons.Clock,
                    accessibilityDescription = "Time $timeStr, $dayDateStr"
                )
                TileSize.WIDE -> LiveTileFace(
                    template = LiveTileTemplate.PRIMARY_TEXT,
                    primaryText = timeStr,
                    secondaryText = longDateStr,
                    tertiaryText = alarmStr,
                    iconVector = MetroIcons.Clock,
                    accessibilityDescription = "Time $timeStr, $longDateStr${alarmStr?.let { ", $it" } ?: ""}"
                )
                TileSize.LARGE -> LiveTileFace(
                    template = LiveTileTemplate.PRIMARY_TEXT,
                    primaryText = timeStr,
                    secondaryText = longDateStr,
                    tertiaryText = alarmStr ?: dayStr,
                    iconVector = MetroIcons.Clock,
                    accessibilityDescription = "Time $timeStr, $longDateStr${alarmStr?.let { ", $it" } ?: ""}"
                )
            }
        }

        return LiveTileState(
            providerId = providerId,
            faces = listOf(face),
            validityDurationMs = 60_000L
        )
    }

    private fun formatInitialRemaining(state: ClockRunState, endElapsed: Long, pausedRemaining: Long): String {
        val ms = if (state == ClockRunState.PAUSED) {
            pausedRemaining
        } else {
            (endElapsed - android.os.SystemClock.elapsedRealtime()).coerceAtLeast(0L)
        }
        val totalSeconds = ms / 1000L
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }
}
