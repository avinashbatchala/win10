package com.ab.livetile.model

/** Which local clock a face should interpolate in the launcher (no per-second IPC). */
enum class LiveClockKind {
    COUNTDOWN,
    STOPWATCH
}

/**
 * Monotonic timing metadata attached to a live tile face. The launcher renders the visible
 * value locally from [android.os.SystemClock.elapsedRealtime]; the owning app only tells us
 * when state *transitions* happen.
 */
data class LiveClock(
    val kind: LiveClockKind,
    val endElapsedRealtime: Long = 0L,
    val startElapsedRealtime: Long = 0L,
    val accumulatedMillis: Long = 0L,
    val paused: Boolean = false,
    val pausedRemainingMillis: Long = 0L
) {
    fun remainingMillis(nowElapsed: Long): Long =
        if (paused) pausedRemainingMillis.coerceAtLeast(0L)
        else (endElapsedRealtime - nowElapsed).coerceAtLeast(0L)

    fun elapsedMillis(nowElapsed: Long): Long =
        if (paused) accumulatedMillis.coerceAtLeast(0L)
        else (accumulatedMillis + (nowElapsed - startElapsedRealtime)).coerceAtLeast(0L)
}
