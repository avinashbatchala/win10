package com.ab.media

import android.media.session.PlaybackState
import androidx.compose.ui.graphics.ImageBitmap

/**
 * Normalized immutable representation of an active Android MediaSession.
 * Designed for Windows 10 Mobile Live Tile rendering and transport control.
 */
data class MediaSessionUiState(
    val packageName: String,
    val title: String,
    val artist: String? = null,
    val album: String? = null,
    val artworkBitmap: ImageBitmap? = null,
    val playbackState: Int = PlaybackState.STATE_NONE,
    val isPlaying: Boolean = playbackState == PlaybackState.STATE_PLAYING,
    val supportedActions: Long = 0L,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val lastPositionUpdateTime: Long = 0L,
    val updateTime: Long = System.currentTimeMillis()
) {
    val canPlay: Boolean
        get() = (supportedActions and PlaybackState.ACTION_PLAY) != 0L ||
                (supportedActions and PlaybackState.ACTION_PLAY_PAUSE) != 0L

    val canPause: Boolean
        get() = (supportedActions and PlaybackState.ACTION_PAUSE) != 0L ||
                (supportedActions and PlaybackState.ACTION_PLAY_PAUSE) != 0L

    val canSkipToNext: Boolean
        get() = (supportedActions and PlaybackState.ACTION_SKIP_TO_NEXT) != 0L

    val canSkipToPrevious: Boolean
        get() = (supportedActions and PlaybackState.ACTION_SKIP_TO_PREVIOUS) != 0L

    val canSeek: Boolean
        get() = (supportedActions and PlaybackState.ACTION_SEEK_TO) != 0L && durationMs > 0

    val displaySubtitle: String
        get() = when {
            !artist.isNullOrBlank() && !album.isNullOrBlank() -> "$artist • $album"
            !artist.isNullOrBlank() -> artist
            !album.isNullOrBlank() -> album
            else -> ""
        }
}
