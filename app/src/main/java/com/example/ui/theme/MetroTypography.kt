package com.example.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

/**
 * Windows 10 Mobile Metro typography system.
 * Based on Segoe UI metric characteristics (light display headers,
 * clean normal-weight compact labels, and bold weight contrast).
 */
val SegoeUiFontFamily = FontFamily(
    Font(R.font.open_sans, FontWeight.Light),
    Font(R.font.open_sans, FontWeight.Normal),
    Font(R.font.open_sans, FontWeight.Medium),
    Font(R.font.open_sans, FontWeight.SemiBold),
    Font(R.font.open_sans, FontWeight.Bold)
)

object MetroTypography {
    val startTitle = TextStyle(
        fontFamily = SegoeUiFontFamily,
        fontWeight = FontWeight.Light,
        fontSize = 42.sp,
        lineHeight = 48.sp,
        letterSpacing = (-0.5).sp,
        color = MetroColors.TextWhite
    )

    val pageHeader = TextStyle(
        fontFamily = SegoeUiFontFamily,
        fontWeight = FontWeight.Light,
        fontSize = 38.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.4).sp,
        color = MetroColors.TextWhite
    )

    val tileLabel = TextStyle(
        fontFamily = SegoeUiFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.5.sp,
        lineHeight = 15.sp,
        letterSpacing = (-0.1).sp,
        color = MetroColors.TextWhite
    )

    val tileLabelLarge = TextStyle(
        fontFamily = SegoeUiFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.5.sp,
        lineHeight = 18.sp,
        letterSpacing = (-0.1).sp,
        color = MetroColors.TextWhite
    )

    val appListGroupHeader = TextStyle(
        fontFamily = SegoeUiFontFamily,
        fontWeight = FontWeight.Light,
        fontSize = 32.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.2).sp,
        color = MetroColors.TextWhite
    )

    val appListItem = TextStyle(
        fontFamily = SegoeUiFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 17.5.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.15).sp,
        color = MetroColors.TextWhite
    )

    val jumpLetter = TextStyle(
        fontFamily = SegoeUiFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 28.sp,
        color = MetroColors.TextWhite
    )

    val searchHint = TextStyle(
        fontFamily = SegoeUiFontFamily,
        fontWeight = FontWeight.Light,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        color = MetroColors.TextDim
    )

    val searchInput = TextStyle(
        fontFamily = SegoeUiFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        color = MetroColors.TextWhite
    )

    val contextMenuTitle = TextStyle(
        fontFamily = SegoeUiFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        color = MetroColors.TextWhite
    )

    val contextMenuItem = TextStyle(
        fontFamily = SegoeUiFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        color = MetroColors.TextWhite
    )

    val editBadge = TextStyle(
        fontFamily = SegoeUiFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        color = MetroColors.TextWhite
    )

    val buttonLabel = TextStyle(
        fontFamily = SegoeUiFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        letterSpacing = 0.5.sp,
        color = MetroColors.TextWhite
    )
}
