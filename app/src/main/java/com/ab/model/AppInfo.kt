package com.ab.model

import androidx.compose.ui.graphics.ImageBitmap

data class AppInfo(
    val packageName: String,
    val activityName: String,
    val label: String,
    val iconBitmap: ImageBitmap?,
    val firstLetter: Char
) {
    val key: String get() = "$packageName/$activityName"
}
