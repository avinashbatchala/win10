package com.ab.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalMetroAccentColor = compositionLocalOf { MetroColors.LumiaBlue }

private val MetroDarkColorScheme = darkColorScheme(
    primary = MetroColors.LumiaBlue,
    onPrimary = Color.White,
    background = MetroColors.BackgroundBlack,
    onBackground = Color.White,
    surface = MetroColors.BackgroundBlack,
    onSurface = Color.White
)

@Composable
fun MetroTheme(
    accentColor: Color = MetroColors.LumiaBlue,
    content: @Composable () -> Unit
) {
    val dynamicScheme = MetroDarkColorScheme.copy(
        primary = accentColor
    )

    CompositionLocalProvider(
        LocalMetroAccentColor provides accentColor
    ) {
        MaterialTheme(
            colorScheme = dynamicScheme,
            content = content
        )
    }
}
