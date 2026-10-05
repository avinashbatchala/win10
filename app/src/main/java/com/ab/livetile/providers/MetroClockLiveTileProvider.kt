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
import com.metro.livetile.contract.ClockTimerState
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
 * via [LiveClock]. Multiple active timers/stopwatches become multiple faces, which the Live
 * Tile engine shuffles between.
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
        return mapToState(state, tileSize)
    }

    private fun mapToState(state: ClockTileState, size: TileSize): LiveTileState {
        val faces = mutableListOf<LiveTileFace>()

        state.activeTimers.forEach { timer ->
            val live = LiveClock(
                kind = LiveClockKind.COUNTDOWN,
                endElapsedRealtime = timer.endElapsedRealtime,
                paused = timer.state == ClockRunState.PAUSED,
                pausedRemainingMillis = timer.remainingWhenPausedMillis
            )
            val initial = initialRemaining(timer)
            faces += LiveTileFace(
                template = LiveTileTemplate.PRIMARY_TEXT,
                primaryText = initial,
                secondaryText = timerSubtitle(timer),
                iconVector = MetroIcons.Clock,
                liveClock = live,
                accessibilityDescription = "Timer $initial"
            )
        }

        state.activeStopwatches.forEach { sw ->
            faces += LiveTileFace(
                template = LiveTileTemplate.PRIMARY_TEXT,
                primaryText = "00:00.0",
                secondaryText = "STOPWATCH",
                iconVector = MetroIcons.Clock,
                liveClock = LiveClock(
                    kind = LiveClockKind.STOPWATCH,
                    startElapsedRealtime = sw.startElapsedRealtime,
                    accumulatedMillis = sw.accumulatedElapsedMillis,
                    paused = sw.state == ClockRunState.PAUSED
                ),
                accessibilityDescription = "Stopwatch"
            )
        }

        if (faces.isEmpty()) {
            faces += clockFace(state, size)
        }

        return LiveTileState(
            providerId = providerId,
            faces = faces,
            validityDurationMs = 60_000L
        )
    }

    private fun timerSubtitle(timer: ClockTimerState): String =
        timer.label?.takeIf { it.isNotBlank() }?.let { "TIMER · $it" } ?: "TIMER"

    private fun clockFace(state: ClockTileState, size: TileSize): LiveTileFace {
        val now = Date(state.currentEpochMillis.takeIf { it > 0L } ?: System.currentTimeMillis())
        val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
        val timeStr = timeFmt.format(now)
        val dayStr = SimpleDateFormat("EEEE", Locale.getDefault()).format(now)
        val dayDateStr = SimpleDateFormat("EEEE d", Locale.getDefault()).format(now)
        val longDateStr = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(now)

        val alarm = state.nextAlarm?.takeIf { it.enabled }
        val alarmStr = alarm?.let { "next alarm ${timeFmt.format(Date(it.triggerEpochMillis))}" }

        return when (size) {
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

    private fun initialRemaining(timer: ClockTimerState): String {
        val ms = if (timer.state == ClockRunState.PAUSED) {
            timer.remainingWhenPausedMillis
        } else {
            (timer.endElapsedRealtime - android.os.SystemClock.elapsedRealtime()).coerceAtLeast(0L)
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
