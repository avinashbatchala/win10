package com.ab.livetile.model

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Immutable structured presentation of a single face of a Live Tile.
 * Multi-face Live Tiles periodically transition between faces.
 */
data class LiveTileFace(
    val template: LiveTileTemplate,
    val primaryText: String? = null,
    val secondaryText: String? = null,
    val tertiaryText: String? = null,
    val textLines: List<String> = emptyList(),
    val badgeCount: Int? = null,
    val badgeText: String? = null,
    val iconVector: ImageVector? = null,
    val imageBitmap: ImageBitmap? = null,
    val imageUri: String? = null,
    val labelOverride: String? = null,
    val mediaState: com.ab.media.MediaSessionUiState? = null,
    val weather: WeatherTileData? = null,
    /** When set, the launcher interpolates the primary text locally (timer/stopwatch). */
    val liveClock: LiveClock? = null,
    val accessibilityDescription: String
)
