package com.ab.ui.theme

import androidx.compose.ui.graphics.Color

object MetroColors {
    // Pure black background matching Windows 10 Mobile OLED Start
    val BackgroundBlack = Color(0xFF000000)
    val TextWhite = Color(0xFFFFFFFF)
    val TextDim = Color(0xFF999999)
    val TextSubtle = Color(0xFF666666)

    // Jump List subdued / inactive cell background & text
    val JumpInactiveBackground = Color(0xFF1F1F1F)
    val JumpInactiveText = Color(0xFF555555)

    // Edit mode
    val EditOverlay = Color(0x33000000)
    val EditControlBackground = Color(0xFF1E1E1E)
    val EditControlBorder = Color(0xFFFFFFFF)

    // Search bar
    val SearchBorder = Color(0xFF888888)
    val SearchBackground = Color(0xFF000000)

    // Authentic Windows 10 Mobile Accent Palette
    val LumiaBlue = Color(0xFF0078D7)
    val Cobalt = Color(0xFF004E8C)
    val Cyan = Color(0xFF00B7C3)
    val Teal = Color(0xFF008299)
    val Emerald = Color(0xFF107C10)
    val Lime = Color(0xFF10893E)
    val Green = Color(0xFF339933)
    val Mango = Color(0xFFFFB900)
    val Orange = Color(0xFFFF8C00)
    val RedOrange = Color(0xFFD24726)
    val Crimson = Color(0xFFA80000)
    val Red = Color(0xFFE81123)
    val Magenta = Color(0xFFD80073)
    val Purple = Color(0xFF6B007B)
    val Lavender = Color(0xFF744DA9)
    val Steel = Color(0xFF68768A)
    val Charcoal = Color(0xFF595959)

    val WindowsAccents = listOf(
        LumiaBlue,
        Cobalt,
        Cyan,
        Teal,
        Emerald,
        Lime,
        Green,
        Mango,
        Orange,
        RedOrange,
        Crimson,
        Red,
        Magenta,
        Purple,
        Lavender,
        Steel,
        Charcoal
    )
}
