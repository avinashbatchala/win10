package com.ab.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ab.livetile.model.LiveTileState
import com.ab.livetile.ui.LiveTileRenderer
import com.ab.model.ResolvedLauncherIcon
import com.ab.model.TileModel
import com.ab.model.TileSize
import com.ab.ui.theme.MetroDimensions
import com.ab.ui.theme.MetroTypography

/**
 * A portion of the Start "Tile picture" background shown inside a single tile. The image
 * is laid out across the whole grid ([width] x [height]) and windowed by the tile's
 * position ([offsetX]/[offsetY]) so the tile mosaic reads as one continuous picture.
 */
data class TilePicture(
    val bitmap: ImageBitmap,
    val offsetX: Dp,
    val offsetY: Dp,
    val width: Dp,
    val height: Dp
)

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
    liveTileState: LiveTileState? = null,
    mediaActionDispatcher: com.ab.media.MediaActionDispatcher? = null,
    showAppNames: Boolean = true,
    mediaShowControls: Boolean = true,
    mediaShowProgress: Boolean = true,
    tilePicture: TilePicture? = null,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    // Entering/leaving edit mode restarts the pointer input, cancelling any in-flight
    // press before its release handler can run. That would leave isPressed stuck true
    // and keep a deselected tile shrunk, so reset it whenever edit mode changes.
    LaunchedEffect(isEditMode) { isPressed = false }

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

    // Third-party apps use their icon's brand colour (Windows 10 Mobile style); Metro/system
    // glyphs and monochrome icons fall back to the accent colour. Explicit custom colour wins.
    val brandColor = resolvedIcon?.brandColor
    val baseColor = when {
        tile.customColor != null -> Color(tile.customColor)
        brandColor != null -> Color(brandColor)
        else -> accentColor
    }
    val bgAlpha = (1.0f - tileTransparency).coerceIn(0.0f, 1.0f)
    val tileBgColor = if (tilePicture != null) baseColor else baseColor.copy(alpha = bgAlpha)
    val displayLabel = (tile.customLabel ?: tile.label) + (if (!tile.isAvailable) " (Unavailable)" else "")

    // Edit controls shrink with the tile so the unpin + resize buttons never overlap on 1x1.
    val tileMinDp = minOf(widthDp, heightDp).value
    val editButtonSize = TileEditMetrics.buttonSizeDp(tileMinDp).dp
    val editIconSize = TileEditMetrics.iconSizeDp(tileMinDp).dp
    val editPadding = TileEditMetrics.paddingDp(tileMinDp).dp

    Box(
        modifier = modifier
            .testTag("tile_${tile.packageName}")
            .size(widthDp, heightDp)
            .scale(pressScale)
            .alpha(editAlpha)
            .background(tileBgColor, RectangleShape)
            .clipToBounds()
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
        // "Tile picture" background: window the full-grid image into this tile.
        if (tilePicture != null) {
            Image(
                bitmap = tilePicture.bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .offset(x = -tilePicture.offsetX, y = -tilePicture.offsetY)
                    .size(tilePicture.width, tilePicture.height)
            )
        }

        // Live Tile content or Static Tile fallback
        if (liveTileState != null && liveTileState.activeFace != null) {
            LiveTileRenderer(
                liveState = liveTileState,
                tileSize = tile.size,
                defaultLabel = displayLabel,
                dispatcher = mediaActionDispatcher,
                mediaShowControls = mediaShowControls,
                mediaShowProgress = mediaShowProgress
            )
        } else {
            // Static Tile Fallback: Always displays pure icon + label cleanly
            when (tile.size) {
                TileSize.SMALL -> {
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
                                    modifier = Modifier.size(MetroDimensions.tileIconSizeMedium)
                                )
                            }
                        }

                        if (showAppNames) {
                            TileLabel(text = displayLabel, modifier = Modifier.align(Alignment.BottomStart))
                        }
                    }
                }
                TileSize.WIDE -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(MetroDimensions.tileContentPadding)
                    ) {
                        // Windows 10 Mobile wide tile: icon on the left, label bottom-left.
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = 8.dp),
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

                        if (showAppNames) {
                            TileLabel(text = displayLabel, modifier = Modifier.align(Alignment.BottomStart))
                        }
                    }
                }
                TileSize.LARGE -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(MetroDimensions.tileContentPadding * 1.5f)
                    ) {
                        // Windows 10 Mobile large tile: icon top-left, label bottom-left.
                        if (resolvedIcon != null) {
                            LauncherIconView(
                                icon = resolvedIcon,
                                contentDescription = tile.label,
                                tint = Color.White,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .size(MetroDimensions.tileIconSizeLarge)
                            )
                        }

                        if (showAppNames) {
                            TileLabel(
                                text = displayLabel,
                                large = true,
                                modifier = Modifier.align(Alignment.BottomStart)
                            )
                        }
                    }
                }
            }
        }

        // Edit mode controls with authentic Metro glyphs, scaled to the tile size.
        if (isEditMode && isSelected) {
            TileUnpinButton(
                onUnpin = onUnpin,
                buttonSize = editButtonSize,
                iconSize = editIconSize,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(editPadding)
            )

            TileResizeButton(
                onResize = onResize,
                buttonSize = editButtonSize,
                iconSize = editIconSize,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(editPadding)
            )
        }
    }
}

@Composable
private fun TileLabel(text: String, large: Boolean = false, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = if (large) MetroTypography.tileLabelLarge else MetroTypography.tileLabel,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}
