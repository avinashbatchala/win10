package com.ab.model

data class LauncherSettings(
    val accentColor: Long = 0xFF0078D7L, // Windows Lumia Blue
    val darkTheme: Boolean = true,
    val showMoreTiles: Boolean = false, // 6 columns (default) vs 8 columns
    val tileTransparency: Float = 0.0f, // 0.0 = fully opaque accent, 0.4 = semi-transparent
    val isDefaultLauncher: Boolean = false
)
