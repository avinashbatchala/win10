package com.ab.model

/** Windows 10 Mobile Start background semantics. */
enum class BackgroundStyle {
    NONE,
    FULL_SCREEN,
    TILE_PICTURE
}

/** Live Tile face-flip cadence. Raw millisecond values are never surfaced in the UI. */
enum class LiveTileAnimationFrequency(val delayMs: Long) {
    LOW(8_000L),
    NORMAL(4_000L),
    HIGH(2_000L)
}

/** Global icon appearance preference applied to app lists and Start tiles. */
enum class AppIconPreference {
    AUTOMATIC,
    ORIGINAL_ICON,
    MONOCHROME
}

/** Launcher window orientation. Landscape is not offered because the layout is portrait-only. */
enum class LauncherOrientation {
    PORTRAIT,
    FOLLOW_SYSTEM
}

data class LauncherSettings(
    val accentColor: Long = 0xFF0078D7L, // Windows Lumia Blue
    val darkTheme: Boolean = true,
    val showMoreTiles: Boolean = false, // 6 columns (default) vs 8 columns
    val tileTransparency: Float = 0.0f, // 0.0f = 0% transparent (opaque), 1.0f = 100% transparent
    val backgroundImageUri: String? = null,
    val backgroundStyle: BackgroundStyle = BackgroundStyle.FULL_SCREEN,
    val isDefaultLauncher: Boolean = false,
    val showMediaLiveTiles: Boolean = true,

    // Live Tiles
    val liveTilesEnabled: Boolean = true,
    val animateLiveTiles: Boolean = true,
    val liveTileAnimationFrequency: LiveTileAnimationFrequency = LiveTileAnimationFrequency.NORMAL,
    val pauseLiveTilesWhenHidden: Boolean = true,
    val pauseLiveTilesInBatterySaver: Boolean = true,

    // Tiles / Start
    val defaultTileSize: TileSize = TileSize.MEDIUM,
    val showAppNames: Boolean = true,

    // Apps
    val appIconPreference: AppIconPreference = AppIconPreference.AUTOMATIC,
    val showAlphabetJumpList: Boolean = true,
    val hiddenApps: Set<String> = emptySet(),

    // Media
    val mediaShowArtwork: Boolean = true,
    val mediaShowControls: Boolean = true,
    val mediaShowProgress: Boolean = true,

    // System
    val launcherOrientation: LauncherOrientation = LauncherOrientation.PORTRAIT
)
