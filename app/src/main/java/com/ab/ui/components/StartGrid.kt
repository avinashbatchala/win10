package com.ab.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.ab.model.TileModel
import com.ab.ui.theme.MetroDimensions
import com.ab.ui.theme.MetroMotion
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun StartGrid(
    tiles: List<TileModel>,
    accentColor: Color,
    showMoreTiles: Boolean,
    tileTransparency: Float,
    isEditMode: Boolean,
    selectedTileId: String?,
    draggedTileId: String?,
    dragOffset: Offset,
    getLauncherIcon: (packageName: String, activityName: String?) -> com.ab.model.ResolvedLauncherIcon,
    getLiveTileState: (packageName: String, activityName: String?) -> com.ab.livetile.model.LiveTileState?,
    mediaActionDispatcher: com.ab.media.MediaActionDispatcher? = null,
    onTileClick: (TileModel) -> Unit,
    onTileLongClick: (String) -> Unit,
    onTileResize: (String) -> Unit,
    onTileUnpin: (String) -> Unit,
    onTileDragStart: (String) -> Unit,
    onTileDrag: (Offset) -> Unit,
    onTileDragEnd: (Int, Int) -> Unit,
    onExitEditMode: () -> Unit,
    onEmptyAreaLongClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val totalCols = if (showMoreTiles) 8 else 6
    val gap = MetroDimensions.tileGap
    val startInset = MetroDimensions.startHorizontalInset
    val topInset = MetroDimensions.startTopInset

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("start_grid_container")
    ) {
        val availableWidth = maxWidth - (startInset * 2)
        val cellWidth = (availableWidth - (gap * (totalCols - 1))) / totalCols
        val step = cellWidth + gap

        val maxRow = (tiles.maxOfOrNull { it.row + it.effectiveRows } ?: 0).coerceAtLeast(8)
        val totalGridHeight = topInset + (step * maxRow) + MetroDimensions.startBottomInset + 100.dp

        Box(
            modifier = Modifier
                .testTag("start_grid_scrollable")
                .fillMaxWidth()
                .height(totalGridHeight)
                .verticalScroll(scrollState)
                .pointerInput(isEditMode) {
                    if (isEditMode) {
                        detectTapGestures(
                            onTap = { onExitEditMode() }
                        )
                    } else {
                        detectTapGestures(
                            onLongPress = { onEmptyAreaLongClick() }
                        )
                    }
                }
        ) {
            for (tile in tiles) {
                val isSelected = isEditMode && selectedTileId == tile.id
                val isDragging = draggedTileId == tile.id
                val icon = getLauncherIcon(tile.packageName, tile.activityName)
                val liveState = getLiveTileState(tile.packageName, tile.activityName)

                val tileWidth = (cellWidth * tile.effectiveCols) + (gap * (tile.effectiveCols - 1))
                val tileHeight = (cellWidth * tile.effectiveRows) + (gap * (tile.effectiveRows - 1))

                val baseX = startInset + (step * tile.col)
                val baseY = topInset + (step * tile.row)

                val baseXPx = with(density) { baseX.toPx() }
                val baseYPx = with(density) { baseY.toPx() }
                val stepPx = with(density) { step.toPx() }

                Box(
                    modifier = Modifier
                        .offset(x = baseX, y = baseY)
                        .zIndex(if (isDragging) 10f else if (isSelected) 5f else 1f)
                        .then(
                            if (isDragging) {
                                Modifier.offset {
                                    IntOffset(
                                        dragOffset.x.roundToInt(),
                                        dragOffset.y.roundToInt()
                                    )
                                }
                            } else {
                                Modifier
                            }
                        )
                        .pointerInput(isEditMode, tile.id) {
                            if (isEditMode) {
                                detectDragGestures(
                                    onDragStart = {
                                        onTileDragStart(tile.id)
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        onTileDrag(dragAmount)

                                        // Auto-scroll logic when dragged near vertical edges
                                        val currentTouchY = baseYPx + dragOffset.y
                                        val viewportTop = scrollState.value.toFloat()
                                        val viewportBottom = viewportTop + 1600f

                                        if (currentTouchY - viewportTop < 120f) {
                                            scope.launch {
                                                scrollState.scrollBy(-30f)
                                            }
                                        } else if (viewportBottom - currentTouchY < 120f) {
                                            scope.launch {
                                                scrollState.scrollBy(30f)
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        val finalXPx = baseXPx + dragOffset.x
                                        val finalYPx = baseYPx + dragOffset.y

                                        val targetCol = ((finalXPx - with(density) { startInset.toPx() } + (stepPx / 2f)) / stepPx)
                                            .toInt()
                                            .coerceIn(0, totalCols - tile.effectiveCols)

                                        val targetRow = ((finalYPx - with(density) { topInset.toPx() } + (stepPx / 2f)) / stepPx)
                                            .toInt()
                                            .coerceAtLeast(0)

                                        onTileDragEnd(targetCol, targetRow)
                                    },
                                    onDragCancel = {
                                        onTileDragEnd(tile.col, tile.row)
                                    }
                                )
                            }
                        }
                        .then(
                            if (!isDragging) {
                                val animAlpha by animateFloatAsState(
                                    targetValue = 1f,
                                    animationSpec = tween(MetroMotion.DURATION_NORMAL),
                                    label = "tile_enter"
                                )
                                Modifier.graphicsLayer {
                                    alpha = animAlpha
                                }
                            } else {
                                Modifier
                            }
                        )
                ) {
                    StartTile(
                        tile = tile,
                        widthDp = tileWidth,
                        heightDp = tileHeight,
                        resolvedIcon = icon,
                        accentColor = accentColor,
                        tileTransparency = tileTransparency,
                        liveTileState = liveState,
                        mediaActionDispatcher = mediaActionDispatcher,
                        isEditMode = isEditMode,
                        isSelected = isSelected,
                        isDragging = isDragging,
                        onClick = {
                            if (isEditMode) {
                                onTileClick(tile)
                            } else {
                                onTileClick(tile)
                            }
                        },
                        onLongClick = {
                            onTileLongClick(tile.id)
                        },
                        onResize = {
                            onTileResize(tile.id)
                        },
                        onUnpin = {
                            onTileUnpin(tile.id)
                        }
                    )
                }
            }
        }
    }
}
