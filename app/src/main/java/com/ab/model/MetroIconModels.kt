package com.ab.model

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector

enum class IconRenderMode {
    METRO_GLYPH,
    ANDROID_MONOCHROME,
    ANDROID_ORIGINAL
}

sealed class ResolvedLauncherIcon {
    data class VectorGlyph(
        val imageVector: ImageVector,
        val semanticId: String
    ) : ResolvedLauncherIcon()

    data class MonochromeBitmap(
        val bitmap: ImageBitmap
    ) : ResolvedLauncherIcon()

    data class OriginalBitmap(
        val bitmap: ImageBitmap
    ) : ResolvedLauncherIcon()
}
