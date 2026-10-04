package com.ab.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween

object MetroMotion {
    // Windows 10 Mobile Turnstile motion curves
    val MetroEaseOut = CubicBezierEasing(0.1f, 0.9f, 0.2f, 1.0f)
    val MetroEaseIn = CubicBezierEasing(0.7f, 0.0f, 1.0f, 0.5f)
    val MetroExponential = CubicBezierEasing(0.16f, 1.0f, 0.3f, 1.0f)

    // Standard timing
    const val DURATION_FAST = 180
    const val DURATION_NORMAL = 280
    const val DURATION_TURNSTILE = 320

    val turnstileOutSpec = tween<Float>(
        durationMillis = DURATION_TURNSTILE,
        easing = MetroEaseIn
    )

    val turnstileInSpec = tween<Float>(
        durationMillis = DURATION_NORMAL,
        easing = MetroEaseOut
    )

    val quickFadeSpec = tween<Float>(
        durationMillis = DURATION_FAST,
        easing = MetroEaseOut
    )
}
