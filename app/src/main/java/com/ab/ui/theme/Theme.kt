package com.ab.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalMetroAccentColor = compositionLocalOf { MetroColors.Blue }
val LocalMetroDarkTheme = compositionLocalOf { true }
val LocalMetroBackground = compositionLocalOf { MetroColors.BackgroundBlack }
val LocalMetroForeground = compositionLocalOf { MetroColors.TextWhite }
val LocalMetroSurface = compositionLocalOf { MetroColors.SurfaceDark }
val LocalMetroSubtleText = compositionLocalOf { MetroColors.TextDim }

@Composable
fun MetroTheme(
    accentColor: Color = MetroColors.Blue,
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val bgColor = if (darkTheme) MetroColors.BackgroundBlack else MetroColors.BackgroundWhite
    val fgColor = if (darkTheme) MetroColors.TextWhite else MetroColors.TextBlack
    val surfaceColor = if (darkTheme) MetroColors.SurfaceDark else MetroColors.SurfaceLight
    val subtleColor = if (darkTheme) MetroColors.TextDim else MetroColors.TextSubtle

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = accentColor,
            onPrimary = Color.White,
            background = bgColor,
            onBackground = fgColor,
            surface = surfaceColor,
            onSurface = fgColor
        )
    } else {
        lightColorScheme(
            primary = accentColor,
            onPrimary = Color.White,
            background = bgColor,
            onBackground = fgColor,
            surface = surfaceColor,
            onSurface = fgColor
        )
    }

    CompositionLocalProvider(
        LocalMetroAccentColor provides accentColor,
        LocalMetroDarkTheme provides darkTheme,
        LocalMetroBackground provides bgColor,
        LocalMetroForeground provides fgColor,
        LocalMetroSurface provides surfaceColor,
        LocalMetroSubtleText provides subtleColor
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
