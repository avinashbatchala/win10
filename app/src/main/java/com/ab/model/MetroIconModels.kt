package com.ab.model

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector

enum class IconRenderMode {
    METRO_GLYPH,
    ANDROID_MONOCHROME,
    ANDROID_ORIGINAL
}

sealed class ResolvedLauncherIcon {

    /**
     * Dominant/brand colour of the app icon, used as the tile and app-list background for
     * third-party apps (Windows 10 Mobile style). Null means "use the theme accent"
     * (system/first-party apps and icons with no usable brand colour).
     */
    open val brandColor: Long? = null

    data class VectorGlyph(
        val imageVector: ImageVector,
        val semanticId: String,
        override val brandColor: Long? = null
    ) : ResolvedLauncherIcon()

    data class MonochromeBitmap(
        val bitmap: ImageBitmap,
        override val brandColor: Long? = null
    ) : ResolvedLauncherIcon()

    data class OriginalBitmap(
        val bitmap: ImageBitmap,
        override val brandColor: Long? = null
    ) : ResolvedLauncherIcon()
}
