package com.ab.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ab.model.ResolvedLauncherIcon
import com.ab.model.TileModel
import com.ab.model.TileSize
import com.ab.ui.theme.MetroDimensions
import com.ab.ui.theme.MetroTypography

@Composable
fun StartTile(
    tile: TileModel,
    widthDp: Dp,
    heightDp: Dp,
    resolvedIcon: ResolvedLauncherIcon?,
    accentColor: Color,
    isEditMode: Boolean,
    isSelected: Boolean,
    isDragging: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onResize: () -> Unit,
    onUnpin: () -> Unit,
    tileTransparency: Float = 0.0f,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    // Metro 3D perspective tilt animation on press
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed && !isEditMode) 0.94f else if (isEditMode && !isSelected) 0.94f else 1.0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "tile_scale"
    )

    val editAlpha by animateFloatAsState(
        targetValue = (if (isEditMode && !isSelected) 0.80f else 1.0f) * (if (!tile.isAvailable) 0.65f else 1.0f),
        animationSpec = spring(),
        label = "tile_alpha"
    )

    val baseColor = if (tile.customColor != null) Color(tile.customColor) else accentColor
    val bgAlpha = (1.0f - tileTransparency).coerceIn(0.0f, 1.0f)
    val tileBgColor = baseColor.copy(alpha = bgAlpha)
    val displayLabel = (tile.customLabel ?: tile.label) + (if (!tile.isAvailable) " (Unavailable)" else "")

    Box(
        modifier = modifier
            .testTag("tile_${tile.packageName}")
            .size(widthDp, heightDp)
            .scale(pressScale)
            .alpha(editAlpha)
            .graphicsLayer {
                shadowElevation = if (isDragging) 16.dp.toPx() else 0f
            }
            .background(tileBgColor, RectangleShape)
            .then(
                if (isEditMode && isSelected) {
                    Modifier.border(2.dp, Color.White, RectangleShape)
                } else {
                    Modifier
                }
            )
            .pointerInput(isEditMode) {
                if (isEditMode) {
                    detectTapGestures(
                        onTap = { onClick() }
                    )
                } else {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            tryAwaitRelease()
                            isPressed = false
                        },
                        onTap = { onClick() },
                        onLongPress = { onLongClick() }
                    )
                }
            }
    ) {
        // Tile internal content
        when (tile.size) {
            TileSize.SMALL -> {
                // Icon centered, no label
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (resolvedIcon != null) {
                        LauncherIconView(
                            icon = resolvedIcon,
                            contentDescription = tile.label,
                            tint = Color.White,
                            modifier = Modifier.size(MetroDimensions.tileIconSizeSmall)
                        )
                    }
                }
            }
            TileSize.MEDIUM -> {
                // Icon in primary optical area, label in lower-left
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(MetroDimensions.tileContentPadding)
                ) {
                    // Icon optically centered slightly above middle
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (resolvedIcon != null) {
                            LauncherIconView(
                                icon = resolvedIcon,
                                contentDescription = tile.label,
                                tint = Color.White,
                                modifier = Modifier.size(MetroDimensions.tileIconSizeMedium)
                            )
                        }
                    }

                    // Label in lower-left
                    Text(
                        text = displayLabel,
                        style = MetroTypography.tileLabel,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.align(Alignment.BottomStart)
                    )
                }
            }
            TileSize.WIDE -> {
                // Icon in left-center area, label in lower-left
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(MetroDimensions.tileContentPadding)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (resolvedIcon != null) {
                            LauncherIconView(
                                icon = resolvedIcon,
                                contentDescription = tile.label,
                                tint = Color.White,
                                modifier = Modifier.size(MetroDimensions.tileIconSizeWide)
                            )
                        }
                    }

                    Text(
                        text = displayLabel,
                        style = MetroTypography.tileLabel,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.align(Alignment.BottomStart)
                    )
                }
            }
            TileSize.LARGE -> {
                // Large icon in upper area, label in lower-left
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(MetroDimensions.tileContentPadding * 1.5f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (resolvedIcon != null) {
                            LauncherIconView(
                                icon = resolvedIcon,
                                contentDescription = tile.label,
                                tint = Color.White,
                                modifier = Modifier.size(MetroDimensions.tileIconSizeLarge)
                            )
                        }
                    }

                    Text(
                        text = displayLabel,
                        style = MetroTypography.tileLabelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.align(Alignment.BottomStart)
                    )
                }
            }
        }

        // Edit mode controls with authentic Metro glyphs
        if (isEditMode && isSelected) {
            // Unpin button (top-right)
            TileUnpinButton(
                onUnpin = onUnpin,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
            )

            // Resize button (bottom-right)
            TileResizeButton(
                onResize = onResize,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
            )
        }
    }
}
