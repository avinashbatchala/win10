package com.ab.livetile.providers

import android.content.Context
import com.ab.livetile.api.LiveTileProvider
import com.ab.livetile.model.LiveTileFace
import com.ab.livetile.model.LiveTileState
import com.ab.livetile.model.LiveTileTemplate
import com.ab.media.MediaSessionRepository
import com.ab.model.TileSize
import com.ab.ui.icons.MetroIcons

/**
 * Generic Windows 10 Mobile Live Tile provider for Android MediaSessions.
 * Supports:
 * 1. App-specific media tiles (e.g. ViMusic, LibreTube, Spotify, VLC)
 * 2. Generic launcher-owned "Now Playing" tile
 */
class MediaLiveTileProvider(
    private val mediaRepository: MediaSessionRepository,
    private val isArtworkEnabled: () -> Boolean = { true }
) : LiveTileProvider {

    companion object {
        const val NOW_PLAYING_PACKAGE = "livetile.nowplaying"
    }

    override val providerId: String = "livetile.system.media"
    override val displayName: String = "Music & Media Live Tile"
    override val refreshIntervalMs: Long = 10_000L

    override fun matchesComponent(packageName: String, activityName: String?): Boolean {
        if (packageName == NOW_PLAYING_PACKAGE) return true
        // Matches any app currently reporting an active media session
        return mediaRepository.activeSessionsByPackage.value.containsKey(packageName)
    }

    override suspend fun getLiveTileState(context: Context, tileSize: TileSize): LiveTileState? {
        // Will be called with the specific tile's package
        return null
    }

    /**
     * Resolves live state for a specific package.
     */
    fun getMediaTileState(packageName: String, tileSize: TileSize, fallbackLabel: String): LiveTileState? {
        val isNowPlayingTile = packageName == NOW_PLAYING_PACKAGE
        val session = if (isNowPlayingTile) {
            mediaRepository.primarySession.value
        } else {
            mediaRepository.activeSessionsByPackage.value[packageName]
        }

        if (session == null) {
            if (isNowPlayingTile) {
                // When idle, Now Playing tile displays clean static music icon + label
                val idleFace = LiveTileFace(
                    template = LiveTileTemplate.ICONIC,
                    iconVector = MetroIcons.Music,
                    primaryText = "Now Playing",
                    labelOverride = "Now Playing",
                    accessibilityDescription = "Now Playing, No active media playback"
                )
                return LiveTileState(
                    providerId = providerId,
                    faces = listOf(idleFace),
                    validityDurationMs = 60_000L
                )
            }
            // App-specific tiles: return null so they gracefully revert to normal static tile
            return null
        }

        val displayTitle = session.title
        val displaySubtitle = session.displaySubtitle
        val accessDesc = buildString {
            if (session.isPlaying) append("Playing ") else append("Paused ")
            append(displayTitle)
            if (!displaySubtitle.isNullOrBlank()) {
                append(" by ").append(displaySubtitle)
            }
        }

        val face = LiveTileFace(
            template = LiveTileTemplate.MEDIA,
            primaryText = displayTitle,
            secondaryText = displaySubtitle,
            tertiaryText = session.album,
            imageBitmap = if (isArtworkEnabled()) session.artworkBitmap else null,
            iconVector = MetroIcons.Music,
            mediaState = session,
            labelOverride = if (isNowPlayingTile) "Now Playing" else fallbackLabel,
            accessibilityDescription = accessDesc
        )

        return LiveTileState(
            providerId = "$providerId.${session.packageName}",
            faces = listOf(face),
            validityDurationMs = 5_000L
        )
    }
}
