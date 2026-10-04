package com.ab.ui.theme

import androidx.compose.ui.graphics.Color

data class MetroAccent(
    val name: String,
    val color: Color
)

object MetroColors {
    // Pure black background matching Windows 10 Mobile OLED Start
    val BackgroundBlack = Color(0xFF000000)
    val BackgroundWhite = Color(0xFFFFFFFF)

    val SurfaceDark = Color(0xFF1F1F1F)
    val SurfaceLight = Color(0xFFF2F2F2)

    val TextWhite = Color(0xFFFFFFFF)
    val TextBlack = Color(0xFF000000)
    val TextDim = Color(0xFF999999)
    val TextSubtle = Color(0xFF666666)

    // Jump List subdued / inactive cell background & text
    val JumpInactiveDark = Color(0xFF1F1F1F)
    val JumpInactiveLight = Color(0xFFE5E5E5)
    val JumpInactiveTextDark = Color(0xFF555555)
    val JumpInactiveTextLight = Color(0xFFAAAAAA)

    // Edit mode
    val EditOverlay = Color(0x33000000)
    val EditControlBackground = Color(0xFF1E1E1E)
    val EditControlBorder = Color(0xFFFFFFFF)

    // Search bar
    val SearchBorderDark = Color(0xFF888888)
    val SearchBorderLight = Color(0xFFCCCCCC)

    // Curated Windows 10 Mobile Accent Palette
    val Blue = Color(0xFF0078D7)
    val Cobalt = Color(0xFF004E8C)
    val Cyan = Color(0xFF00B7C3)
    val Teal = Color(0xFF008299)
    val Green = Color(0xFF339933)
    val Emerald = Color(0xFF107C10)
    val Lime = Color(0xFF84BD00)
    val Amber = Color(0xFFFFB900)
    val Orange = Color(0xFFFF8C00)
    val Crimson = Color(0xFFA80000)
    val Red = Color(0xFFE81123)
    val Magenta = Color(0xFFD80073)
    val Purple = Color(0xFF6B007B)
    val Violet = Color(0xFF744DA9)

    // Backward-compatibility alias
    val LumiaBlue = Blue

    val WindowsAccents = listOf(
        MetroAccent("Blue", Blue),
        MetroAccent("Cobalt", Cobalt),
        MetroAccent("Cyan", Cyan),
        MetroAccent("Teal", Teal),
        MetroAccent("Emerald", Emerald),
        MetroAccent("Green", Green),
        MetroAccent("Lime", Lime),
        MetroAccent("Amber", Amber),
        MetroAccent("Orange", Orange),
        MetroAccent("Red", Red),
        MetroAccent("Crimson", Crimson),
        MetroAccent("Magenta", Magenta),
        MetroAccent("Purple", Purple),
        MetroAccent("Violet", Violet)
    )
}
