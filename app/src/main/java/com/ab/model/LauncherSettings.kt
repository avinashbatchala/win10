package com.ab.model

data class LauncherSettings(
    val accentColor: Long = 0xFF0078D7L, // Windows Lumia Blue
    val darkTheme: Boolean = true,
    val showMoreTiles: Boolean = false, // 6 columns (default) vs 8 columns
    val tileTransparency: Float = 0.0f, // 0.0f = 0% transparent (opaque), 1.0f = 100% transparent
    val backgroundImageUri: String? = null,
    val isDefaultLauncher: Boolean = false,
    val showMediaLiveTiles: Boolean = true
)
