package com.ab.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Windows 10 Mobile / Segoe MDL2-style clean vector glyphs.
 * Characterized by:
 * - 24x24 coordinate viewport
 * - Thin-to-medium strokes (1.5 - 2.0 dp visual thickness)
 * - Sharp geometric silhouettes
 * - Minimal detail
 * - No rounded Material bubbles or circular containers
 * - Pure white foreground design
 */
object MetroIcons {

    val Phone: ImageVector by lazy {
        ImageVector.Builder(
            name = "MetroPhone",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Modern flat handset silhouette
            path(
                fill = SolidColor(Color.White),
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(6.62f, 10.79f)
                curveTo(8.06f, 13.62f, 10.38f, 15.94f, 13.21f, 17.38f)
                lineTo(15.41f, 15.18f)
                curveTo(15.69f, 14.9f, 16.08f, 14.82f, 16.43f, 14.93f)
                curveTo(17.55f, 15.3f, 18.75f, 15.5f, 20f, 15.5f)
                curveTo(20.55f, 15.5f, 21f, 15.95f, 21f, 16.5f)
                lineTo(21f, 20f)
                curveTo(21f, 20.55f, 20.55f, 21f, 20f, 21f)
                curveTo(10.61f, 21f, 3f, 13.39f, 3f, 4f)
                curveTo(3f, 3.45f, 3.45f, 3f, 4f, 3f)
                lineTo(7.5f, 3f)
                curveTo(8.05f, 3f, 8.5f, 3.45f, 8.5f, 4f)
                curveTo(8.5f, 5.25f, 8.7f, 6.45f, 9.07f, 7.57f)
                curveTo(9.18f, 7.92f, 9.1f, 8.31f, 8.82f, 8.59f)
                lineTo(6.62f, 10.79f)
                close()
            }
        }.build()
    }

    val Messaging: ImageVector by lazy {
        ImageVector.Builder(
            name = "MetroMessaging",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Crisp Windows 10 Mobile rectangular chat bubble with corner tail
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Square,
                strokeLineJoin = StrokeJoin.Miter
            ) {
                moveTo(3f, 4f)
                lineTo(21f, 4f)
                lineTo(21f, 17f)
                lineTo(8f, 17f)
                lineTo(4f, 21f)
                lineTo(4f, 17f)
                lineTo(3f, 17f)
                close()
            }
            // Message horizontal lines
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.6f
            ) {
                moveTo(6.5f, 8.5f)
                lineTo(17.5f, 8.5f)
                moveTo(6.5f, 12.5f)
                lineTo(14.5f, 12.5f)
            }
        }.build()
    }

    val People: ImageVector by lazy {
        ImageVector.Builder(
            name = "MetroPeople",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Primary person head & shoulders
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.8f
            ) {
                // Head box
                moveTo(6.5f, 4.5f)
                lineTo(11.5f, 4.5f)
                lineTo(11.5f, 9.5f)
                lineTo(6.5f, 9.5f)
                close()
                // Shoulders
                moveTo(3f, 20f)
                lineTo(3f, 17f)
                lineTo(6.5f, 14.5f)
                lineTo(11.5f, 14.5f)
                lineTo(15f, 17f)
                lineTo(15f, 20f)
            }
            // Secondary companion figure behind
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.6f
            ) {
                // Secondary head
                moveTo(14f, 5f)
                lineTo(18f, 5f)
                lineTo(18f, 9f)
                lineTo(14f, 9f)
                close()
                // Secondary shoulder line
                moveTo(16f, 13f)
                lineTo(18.5f, 14.5f)
                lineTo(21f, 16.5f)
                lineTo(21f, 20f)
            }
        }.build()
    }

    val Calendar: ImageVector by lazy {
        ImageVector.Builder(
            name = "MetroCalendar",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Calendar outline box
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Square,
                strokeLineJoin = StrokeJoin.Miter
            ) {
                moveTo(3f, 6f)
                lineTo(21f, 6f)
                lineTo(21f, 21f)
                lineTo(3f, 21f)
                close()
            }
            // Top header bar
            path(
                fill = SolidColor(Color.White)
            ) {
                moveTo(3f, 6f)
                lineTo(21f, 6f)
                lineTo(21f, 10f)
                lineTo(3f, 10f)
                close()
            }
            // Binder pins
            path(
                fill = SolidColor(Color.White)
            ) {
                moveTo(6f, 3f)
                lineTo(8f, 3f)
                lineTo(8f, 7f)
                lineTo(6f, 7f)
                close()
                moveTo(16f, 3f)
                lineTo(18f, 3f)
                lineTo(18f, 7f)
                lineTo(16f, 7f)
                close()
            }
            // Internal day dots
            path(
                fill = SolidColor(Color.White)
            ) {
                // Dot 1
                moveTo(6.5f, 12.5f); lineTo(8.5f, 12.5f); lineTo(8.5f, 14.5f); lineTo(6.5f, 14.5f); close()
                // Dot 2
                moveTo(11f, 12.5f); lineTo(13f, 12.5f); lineTo(13f, 14.5f); lineTo(11f, 14.5f); close()
                // Dot 3
                moveTo(15.5f, 12.5f); lineTo(17.5f, 12.5f); lineTo(17.5f, 14.5f); lineTo(15.5f, 14.5f); close()
                // Dot 4
                moveTo(6.5f, 16.5f); lineTo(8.5f, 16.5f); lineTo(8.5f, 18.5f); lineTo(6.5f, 18.5f); close()
                // Dot 5
                moveTo(11f, 16.5f); lineTo(13f, 16.5f); lineTo(13f, 18.5f); lineTo(11f, 18.5f); close()
            }
        }.build()
    }

    val Camera: ImageVector by lazy {
        ImageVector.Builder(
            name = "MetroCamera",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Camera body outline
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Square,
                strokeLineJoin = StrokeJoin.Miter
            ) {
                moveTo(2f, 7f)
                lineTo(7f, 7f)
                lineTo(8.5f, 4f)
                lineTo(15.5f, 4f)
                lineTo(17f, 7f)
                lineTo(22f, 7f)
                lineTo(22f, 20f)
                lineTo(2f, 20f)
                close()
            }
            // Center lens ring
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f
            ) {
                moveTo(12f, 9.5f)
                arcTo(3.5f, 3.5f, 0f, true, true, 11.99f, 9.5f)
            }
        }.build()
    }

    val Photos: ImageVector by lazy {
        ImageVector.Builder(
            name = "MetroPhotos",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Photo frame outline
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Square,
                strokeLineJoin = StrokeJoin.Miter
            ) {
                moveTo(3f, 4f)
                lineTo(21f, 4f)
                lineTo(21f, 20f)
                lineTo(3f, 20f)
                close()
            }
            // Sun square
            path(fill = SolidColor(Color.White)) {
                moveTo(6.5f, 7f)
                lineTo(9.5f, 7f)
                lineTo(9.5f, 10f)
                lineTo(6.5f, 10f)
                close()
            }
            // Mountains line
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.8f
            ) {
                moveTo(3f, 17f)
                lineTo(9f, 11f)
                lineTo(14f, 16f)
                lineTo(17f, 13f)
                lineTo(21f, 17f)
            }
        }.build()
    }

    val Settings: ImageVector by lazy {
        ImageVector.Builder(
            name = "MetroSettings",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Windows 10 Mobile gear icon with square teeth
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Square,
                strokeLineJoin = StrokeJoin.Miter
            ) {
                moveTo(10f, 2f)
                lineTo(14f, 2f)
                lineTo(14.5f, 4.5f)
                lineTo(16.5f, 5.5f)
                lineTo(19f, 4f)
                lineTo(21f, 6.5f)
                lineTo(19.5f, 9f)
                lineTo(20f, 11f)
                lineTo(22.5f, 11.5f)
                lineTo(22.5f, 14f)
                lineTo(20f, 14.5f)
                lineTo(19.5f, 16.5f)
                lineTo(21f, 19f)
                lineTo(19f, 21f)
                lineTo(16.5f, 19.5f)
                lineTo(14.5f, 20f)
                lineTo(14f, 22.5f)
                lineTo(10f, 22.5f)
                lineTo(9.5f, 20f)
                lineTo(7.5f, 19.5f)
                lineTo(5f, 21f)
                lineTo(3f, 18.5f)
                lineTo(4.5f, 16f)
                lineTo(4f, 14f)
                lineTo(1.5f, 13.5f)
                lineTo(1.5f, 11f)
                lineTo(4f, 10.5f)
                lineTo(4.5f, 8.5f)
                lineTo(3f, 6f)
                lineTo(5f, 4f)
                lineTo(7.5f, 5.5f)
                lineTo(9.5f, 4.5f)
                close()
            }
            // Inner circle
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f
            ) {
                moveTo(12f, 9.5f)
                arcTo(2.5f, 2.5f, 0f, true, true, 11.99f, 9.5f)
            }
        }.build()
    }

    val Calculator: ImageVector by lazy {
        ImageVector.Builder(
            name = "MetroCalculator",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Outer rectangle
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Square,
                strokeLineJoin = StrokeJoin.Miter
            ) {
                moveTo(4f, 2f)
                lineTo(20f, 2f)
                lineTo(20f, 22f)
                lineTo(4f, 22f)
                close()
            }
            // Screen display
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.5f
            ) {
                moveTo(6.5f, 5f)
                lineTo(17.5f, 5f)
                lineTo(17.5f, 8.5f)
                lineTo(6.5f, 8.5f)
                close()
            }
            // Keypad grid
            path(fill = SolidColor(Color.White)) {
                moveTo(6.5f, 11f); lineTo(8.5f, 11f); lineTo(8.5f, 13f); lineTo(6.5f, 13f); close()
                moveTo(11f, 11f); lineTo(13f, 11f); lineTo(13f, 13f); lineTo(11f, 13f); close()
                moveTo(15.5f, 11f); lineTo(17.5f, 11f); lineTo(17.5f, 13f); lineTo(15.5f, 13f); close()
                moveTo(6.5f, 14.5f); lineTo(8.5f, 14.5f); lineTo(8.5f, 16.5f); lineTo(6.5f, 16.5f); close()
                moveTo(11f, 14.5f); lineTo(13f, 14.5f); lineTo(13f, 16.5f); lineTo(11f, 16.5f); close()
                moveTo(15.5f, 14.5f); lineTo(17.5f, 14.5f); lineTo(17.5f, 16.5f); lineTo(15.5f, 16.5f); close()
                moveTo(6.5f, 18f); lineTo(8.5f, 18f); lineTo(8.5f, 20f); lineTo(6.5f, 20f); close()
                moveTo(11f, 18f); lineTo(13f, 18f); lineTo(13f, 20f); lineTo(11f, 20f); close()
                moveTo(15.5f, 18f); lineTo(17.5f, 18f); lineTo(17.5f, 20f); lineTo(15.5f, 20f); close()
            }
        }.build()
    }

    val Clock: ImageVector by lazy {
        ImageVector.Builder(
            name = "MetroClock",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Clock circle outline
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f
            ) {
                moveTo(12f, 3f)
                arcTo(9f, 9f, 0f, true, true, 11.99f, 3f)
            }
            // Clock hands at 3:00
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Square,
                strokeLineJoin = StrokeJoin.Miter
            ) {
                moveTo(12f, 7f)
                lineTo(12f, 12f)
                lineTo(16f, 12f)
            }
        }.build()
    }

    val Mail: ImageVector by lazy {
        ImageVector.Builder(
            name = "MetroMail",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Envelope body
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Square,
                strokeLineJoin = StrokeJoin.Miter
            ) {
                moveTo(2f, 5f)
                lineTo(22f, 5f)
                lineTo(22f, 19f)
                lineTo(2f, 19f)
                close()
            }
            // Envelope fold line
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.8f
            ) {
                moveTo(2f, 6.5f)
                lineTo(12f, 13.5f)
                lineTo(22f, 6.5f)
            }
        }.build()
    }

    val Browser: ImageVector by lazy {
        ImageVector.Builder(
            name = "MetroBrowser",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Global wireframe sphere
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f
            ) {
                moveTo(12f, 2f)
                arcTo(10f, 10f, 0f, true, true, 11.99f, 2f)
            }
            // Horizontal equator
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.6f
            ) {
                moveTo(2.5f, 12f)
                lineTo(21.5f, 12f)
            }
            // Longitudinal ellipse
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.6f
            ) {
                moveTo(12f, 2f)
                arcTo(5f, 10f, 0f, true, true, 11.99f, 2f)
            }
        }.build()
    }

    val Maps: ImageVector by lazy {
        ImageVector.Builder(
            name = "MetroMaps",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Folded map panels
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Square,
                strokeLineJoin = StrokeJoin.Miter
            ) {
                moveTo(3f, 5f)
                lineTo(9f, 2.5f)
                lineTo(15f, 5f)
                lineTo(21f, 2.5f)
                lineTo(21f, 19f)
                lineTo(15f, 21.5f)
                lineTo(9f, 19f)
                lineTo(3f, 21.5f)
                close()
            }
            // Internal panel folds
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.8f
            ) {
                moveTo(9f, 2.5f)
                lineTo(9f, 19f)
                moveTo(15f, 5f)
                lineTo(15f, 21.5f)
            }
        }.build()
    }

    val Files: ImageVector by lazy {
        ImageVector.Builder(
            name = "MetroFiles",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // File folder silhouette
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Square,
                strokeLineJoin = StrokeJoin.Miter
            ) {
                moveTo(2f, 4f)
                lineTo(9f, 4f)
                lineTo(11f, 7f)
                lineTo(22f, 7f)
                lineTo(22f, 20f)
                lineTo(2f, 20f)
                close()
            }
        }.build()
    }

    val Store: ImageVector by lazy {
        ImageVector.Builder(
            name = "MetroStore",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Shopping bag outline
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Square,
                strokeLineJoin = StrokeJoin.Miter
            ) {
                moveTo(3f, 7f)
                lineTo(21f, 7f)
                lineTo(19f, 21f)
                lineTo(5f, 21f)
                close()
            }
            // Bag handle arch
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f
            ) {
                moveTo(8.5f, 7f)
                lineTo(8.5f, 4f)
                lineTo(15.5f, 4f)
                lineTo(15.5f, 7f)
            }
        }.build()
    }

    val Music: ImageVector by lazy {
        ImageVector.Builder(
            name = "MetroMusic",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Clean double beamed musical note
            path(fill = SolidColor(Color.White)) {
                moveTo(4f, 14f); lineTo(9f, 14f); lineTo(9f, 19f); lineTo(4f, 19f); close()
                moveTo(14f, 12f); lineTo(19f, 12f); lineTo(19f, 17f); lineTo(14f, 17f); close()
            }
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Square,
                strokeLineJoin = StrokeJoin.Miter
            ) {
                moveTo(9f, 15f)
                lineTo(9f, 5f)
                lineTo(19f, 3f)
                lineTo(19f, 13f)
            }
        }.build()
    }

    val Video: ImageVector by lazy {
        ImageVector.Builder(
            name = "MetroVideo",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Video camera / film player outline
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Square,
                strokeLineJoin = StrokeJoin.Miter
            ) {
                moveTo(3f, 5f)
                lineTo(16f, 5f)
                lineTo(16f, 19f)
                lineTo(3f, 19f)
                close()
            }
            // Projector snout
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Square,
                strokeLineJoin = StrokeJoin.Miter
            ) {
                moveTo(16f, 9f)
                lineTo(22f, 6f)
                lineTo(22f, 18f)
                lineTo(16f, 15f)
            }
        }.build()
    }

    val Search: ImageVector by lazy {
        ImageVector.Builder(
            name = "MetroSearch",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f
            ) {
                moveTo(10f, 3f)
                arcTo(7f, 7f, 0f, true, true, 9.99f, 3f)
            }
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.4f,
                strokeLineCap = StrokeCap.Square
            ) {
                moveTo(15.5f, 15.5f)
                lineTo(21.5f, 21.5f)
            }
        }.build()
    }

    val Pin: ImageVector by lazy {
        ImageVector.Builder(
            name = "MetroPin",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Square,
                strokeLineJoin = StrokeJoin.Miter
            ) {
                moveTo(16f, 3f)
                lineTo(21f, 8f)
                lineTo(19f, 10f)
                lineTo(17f, 9f)
                lineTo(13f, 13f)
                lineTo(13f, 17f)
                lineTo(11f, 19f)
                lineTo(7f, 15f)
                lineTo(9f, 13f)
                lineTo(13f, 13f)
                lineTo(14f, 9f)
                lineTo(13f, 7f)
                close()
                moveTo(9f, 15f)
                lineTo(3f, 21f)
            }
        }.build()
    }

    val Unpin: ImageVector by lazy {
        ImageVector.Builder(
            name = "MetroUnpin",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Square
            ) {
                moveTo(16f, 3f)
                lineTo(21f, 8f)
                lineTo(19f, 10f)
                lineTo(17f, 9f)
                lineTo(14f, 12f)
                moveTo(11f, 15f)
                lineTo(7f, 15f)
                lineTo(9f, 13f)
                moveTo(9f, 15f)
                lineTo(3f, 21f)
                // Diagonal slash
                moveTo(2f, 2f)
                lineTo(22f, 22f)
            }
        }.build()
    }

    val Resize: ImageVector by lazy {
        ImageVector.Builder(
            name = "MetroResize",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Square,
                strokeLineJoin = StrokeJoin.Miter
            ) {
                // Top-left arrow
                moveTo(10f, 4f)
                lineTo(4f, 4f)
                lineTo(4f, 10f)
                moveTo(4f, 4f)
                lineTo(10f, 10f)

                // Bottom-right arrow
                moveTo(14f, 20f)
                lineTo(20f, 20f)
                lineTo(20f, 14f)
                moveTo(20f, 20f)
                lineTo(14f, 14f)
            }
        }.build()
    }

    val Ellipsis: ImageVector by lazy {
        ImageVector.Builder(
            name = "MetroEllipsis",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(4f, 10.5f); lineTo(7f, 10.5f); lineTo(7f, 13.5f); lineTo(4f, 13.5f); close()
                moveTo(10.5f, 10.5f); lineTo(13.5f, 10.5f); lineTo(13.5f, 13.5f); lineTo(10.5f, 13.5f); close()
                moveTo(17f, 10.5f); lineTo(20f, 10.5f); lineTo(20f, 13.5f); lineTo(17f, 13.5f); close()
            }
        }.build()
    }

    val Back: ImageVector by lazy {
        ImageVector.Builder(
            name = "MetroBack",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Square,
                strokeLineJoin = StrokeJoin.Miter
            ) {
                moveTo(15f, 4f)
                lineTo(7f, 12f)
                lineTo(15f, 20f)
            }
        }.build()
    }

    val Forward: ImageVector by lazy {
        ImageVector.Builder(
            name = "MetroForward",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Square,
                strokeLineJoin = StrokeJoin.Miter
            ) {
                moveTo(9f, 4f)
                lineTo(17f, 12f)
                lineTo(9f, 20f)
            }
        }.build()
    }

    val Add: ImageVector by lazy {
        ImageVector.Builder(
            name = "MetroAdd",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Square
            ) {
                moveTo(12f, 4f)
                lineTo(12f, 20f)
                moveTo(4f, 12f)
                lineTo(20f, 12f)
            }
        }.build()
    }

    val Check: ImageVector by lazy {
        ImageVector.Builder(
            name = "MetroCheck",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.4f,
                strokeLineCap = StrokeCap.Square,
                strokeLineJoin = StrokeJoin.Miter
            ) {
                moveTo(4f, 12.5f)
                lineTo(9.5f, 18f)
                lineTo(20f, 6.5f)
            }
        }.build()
    }

    val Close: ImageVector by lazy {
        ImageVector.Builder(
            name = "MetroClose",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Square
            ) {
                moveTo(5f, 5f)
                lineTo(19f, 19f)
                moveTo(19f, 5f)
                lineTo(5f, 19f)
            }
        }.build()
    }

    val GenericApp: ImageVector by lazy {
        ImageVector.Builder(
            name = "MetroGenericApp",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Windows 10 Mobile style 4-pane grid app silhouette
            path(fill = SolidColor(Color.White)) {
                moveTo(4f, 4f); lineTo(11f, 4f); lineTo(11f, 11f); lineTo(4f, 11f); close()
                moveTo(13f, 4f); lineTo(20f, 4f); lineTo(20f, 11f); lineTo(13f, 11f); close()
                moveTo(4f, 13f); lineTo(11f, 13f); lineTo(11f, 20f); lineTo(4f, 20f); close()
                moveTo(13f, 13f); lineTo(20f, 13f); lineTo(20f, 20f); lineTo(13f, 20f); close()
            }
        }.build()
    }

    val Play: ImageVector by lazy {
        ImageVector.Builder(
            name = "MetroPlay",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(8f, 5f)
                lineTo(19f, 12f)
                lineTo(8f, 19f)
                close()
            }
        }.build()
    }

    val Pause: ImageVector by lazy {
        ImageVector.Builder(
            name = "MetroPause",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                // Left bar
                moveTo(6f, 5f); lineTo(10f, 5f); lineTo(10f, 19f); lineTo(6f, 19f); close()
                // Right bar
                moveTo(14f, 5f); lineTo(18f, 5f); lineTo(18f, 19f); lineTo(14f, 19f); close()
            }
        }.build()
    }

    val SkipNext: ImageVector by lazy {
        ImageVector.Builder(
            name = "MetroSkipNext",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                // Triangle
                moveTo(6f, 5f); lineTo(16f, 12f); lineTo(6f, 19f); close()
                // End bar
                moveTo(17f, 5f); lineTo(19.5f, 5f); lineTo(19.5f, 19f); lineTo(17f, 19f); close()
            }
        }.build()
    }

    val SkipPrevious: ImageVector by lazy {
        ImageVector.Builder(
            name = "MetroSkipPrevious",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                // Start bar
                moveTo(4.5f, 5f); lineTo(7f, 5f); lineTo(7f, 19f); lineTo(4.5f, 19f); close()
                // Left Triangle
                moveTo(18f, 5f); lineTo(8f, 12f); lineTo(18f, 19f); close()
            }
        }.build()
    }
}
