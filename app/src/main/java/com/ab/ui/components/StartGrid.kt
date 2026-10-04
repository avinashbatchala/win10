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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.ab.model.TileModel
import com.ab.ui.theme.MetroDimensions
import com.ab.ui.theme.MetroMotion
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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
    liveTileStates: Map<String, com.ab.livetile.model.LiveTileState> = emptyMap(),
    mediaActionDispatcher: com.ab.media.MediaActionDispatcher? = null,
    showAppNames: Boolean = true,
    mediaShowControls: Boolean = true,
    mediaShowProgress: Boolean = true,
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

    // Measured height of the visible scroll viewport in pixels. Updated by layout
    // so drag auto-scroll adapts to any screen size/density instead of assuming one.
    var viewportHeightPx by remember { mutableIntStateOf(0) }

    // Single auto-scroll job so rapid drag events don't spawn a scroll coroutine each frame.
    var autoScrollJob by remember { mutableStateOf<Job?>(null) }
    var autoScrollDirection by remember { mutableFloatStateOf(0f) }

    fun setAutoScroll(direction: Float) {
        if (autoScrollDirection == direction) return
        autoScrollDirection = direction
        autoScrollJob?.cancel()
        autoScrollJob = null
        if (direction == 0f) return
        autoScrollJob = scope.launch {
            while (isActive) {
                scrollState.scrollBy(direction * 24f)
                delay(16L)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { autoScrollJob?.cancel() }
    }

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
        // Generous editable tail: a larger static buffer plus, while editing, half a
        // viewport so the last tile can be dragged/scrolled well past the fold.
        val tailBuffer = (step * 2) + 64.dp
        val editScrollBuffer = if (isEditMode && maxHeight.value.isFinite()) maxHeight * 0.5f else 0.dp
        val totalGridHeight = topInset + (step * maxRow) + MetroDimensions.startBottomInset +
            tailBuffer + editScrollBuffer

        // Outer viewport owns the scroll and fills the available space. The inner
        // content box owns the computed height so offset-positioned tiles are reachable.
        Box(
            modifier = Modifier
                .testTag("start_grid_scrollable")
                .fillMaxSize()
                .onSizeChanged { viewportHeightPx = it.height }
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
            Box(
                modifier = Modifier
                    .testTag("start_grid_content")
                    .fillMaxWidth()
                    .height(totalGridHeight)
            ) {
            for (tile in tiles) {
                // Key by tile id so per-tile remembered state (live face, press, animations)
                // follows the tile when the list is reordered by resize/move.
                key(tile.id) {
                val isSelected = isEditMode && selectedTileId == tile.id
                val isDragging = draggedTileId == tile.id
                val icon = getLauncherIcon(tile.packageName, tile.activityName)
                val liveState = (liveTileStates[tile.componentKey] ?: liveTileStates[tile.packageName])
                    ?.takeIf { it.isAvailable && it.faces.isNotEmpty() }

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
                                // Accumulated drag for this gesture, so edge math doesn't
                                // depend on a stale captured dragOffset mid-gesture.
                                var accumulatedDrag = Offset.Zero
                                detectDragGestures(
                                    onDragStart = {
                                        accumulatedDrag = Offset.Zero
                                        onTileDragStart(tile.id)
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        onTileDrag(dragAmount)
                                        accumulatedDrag += dragAmount

                                        // Auto-scroll near vertical edges using the measured
                                        // viewport height (pixels throughout).
                                        val edgeThresholdPx = with(density) { 96.dp.toPx() }
                                        val currentTouchY = baseYPx + accumulatedDrag.y
                                        val viewportTop = scrollState.value.toFloat()
                                        val viewportBottom = viewportTop + viewportHeightPx

                                        val scrollDirection = when {
                                            currentTouchY - viewportTop < edgeThresholdPx -> -1f
                                            viewportBottom - currentTouchY < edgeThresholdPx -> 1f
                                            else -> 0f
                                        }
                                        setAutoScroll(scrollDirection)
                                    },
                                    onDragEnd = {
                                        setAutoScroll(0f)
                                        val finalXPx = baseXPx + accumulatedDrag.x
                                        val finalYPx = baseYPx + accumulatedDrag.y

                                        val targetCol = ((finalXPx - with(density) { startInset.toPx() } + (stepPx / 2f)) / stepPx)
                                            .toInt()
                                            .coerceIn(0, totalCols - tile.effectiveCols)

                                        val targetRow = ((finalYPx - with(density) { topInset.toPx() } + (stepPx / 2f)) / stepPx)
                                            .toInt()
                                            .coerceAtLeast(0)

                                        onTileDragEnd(targetCol, targetRow)
                                    },
                                    onDragCancel = {
                                        setAutoScroll(0f)
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
                        showAppNames = showAppNames,
                        mediaShowControls = mediaShowControls,
                        mediaShowProgress = mediaShowProgress,
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
    }
}
