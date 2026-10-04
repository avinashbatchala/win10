package com.ab.model

import androidx.compose.ui.graphics.ImageBitmap

data class AppInfo(
    val packageName: String,
    val activityName: String,
    val label: String,
    val iconBitmap: ImageBitmap? = null,
    val firstLetter: Char,
    val canUninstall: Boolean = false,
    val resolvedIcon: ResolvedLauncherIcon? = null
) {
    val key: String get() = "$packageName/$activityName"
}
