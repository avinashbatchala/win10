package com.ab.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.ab.model.BackgroundStyle
import com.ab.model.LauncherSettings
import com.ab.model.TileModel
import com.ab.ui.components.StartGrid
import com.ab.ui.icons.MetroIcons
import com.ab.ui.theme.LocalMetroBackground
import com.ab.ui.theme.MetroColors
import com.ab.ui.theme.MetroDimensions
import com.ab.ui.viewmodel.LauncherViewModel

@Composable
fun StartScreen(
    viewModel: LauncherViewModel,
    tiles: List<TileModel>,
    settings: LauncherSettings,
    isEditMode: Boolean,
    selectedTileId: String?,
    onNavigateToApps: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = Color(settings.accentColor)
    val wallpaperBitmap by viewModel.wallpaperBitmap.collectAsState()
    // Reactive live tile state so song changes / face flips update without navigation.
    val liveTileStates by viewModel.visibleLiveTileStates.collectAsState()
    val bgColor = LocalMetroBackground.current

    var scrollOffset by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val screenHeightPx = with(density) {
        LocalConfiguration.current.screenHeightDp.dp.toPx()
    }
    // Bounded parallax: the wallpaper trails the tiles but never slides far enough to
    // reveal an edge (the image is scaled 1.2x to provide overscan).
    val parallax = (-scrollOffset * 0.3f)
        .coerceIn(-screenHeightPx * 0.09f, screenHeightPx * 0.09f)

    Box(
        modifier = modifier
            .testTag("start_screen")
            .fillMaxSize()
            .background(bgColor)
    ) {
        // Full-screen Start wallpaper (only for the Full screen background style).
        if (wallpaperBitmap != null && settings.backgroundStyle == BackgroundStyle.FULL_SCREEN) {
            Image(
                bitmap = wallpaperBitmap!!,
                contentDescription = "Start wallpaper background",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
                    .graphicsLayer {
                        translationY = parallax
                        scaleX = 1.2f
                        scaleY = 1.2f
                    }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Start Grid containing tiles. Settings is reached by long-pressing empty space.
            // (The old top bar with the "Start" title and gear icon was removed.)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                StartGrid(
                    tiles = tiles,
                    accentColor = accentColor,
                    showMoreTiles = settings.showMoreTiles,
                    tileTransparency = settings.tileTransparency,
                    isEditMode = isEditMode,
                    selectedTileId = selectedTileId,
                    draggedTileId = viewModel.draggedTileId.collectAsState().value,
                    dragOffset = viewModel.dragOffset.collectAsState().value,
                    getLauncherIcon = { pkg, act -> viewModel.resolveLauncherIcon(pkg, act) },
                    liveTileStates = liveTileStates,
                    mediaActionDispatcher = viewModel.mediaActionDispatcher,
                    showAppNames = settings.showAppNames,
                    mediaShowControls = settings.mediaShowControls,
                    mediaShowProgress = settings.mediaShowProgress,
                    backgroundStyle = settings.backgroundStyle,
                    backgroundBitmap = wallpaperBitmap,
                    onScrollOffset = { scrollOffset = it },
                    onTileClick = { tile ->
                        if (isEditMode) {
                            viewModel.onTileClickedInEdit(tile.id)
                        } else {
                            when (tile.packageName) {
                                "livetile.nowplaying" -> {
                                    val primaryPkg = viewModel.mediaSessionRepository.primarySession.value?.packageName
                                    if (primaryPkg != null) {
                                        viewModel.launchApp(viewModel.getApplication(), primaryPkg, "", tile.label)
                                    }
                                }
                                com.ab.ui.settings.SystemTiles.WEATHER_PACKAGE -> {
                                    viewModel.launchWeatherApp(viewModel.getApplication(), tile.label)
                                }
                                else -> {
                                    viewModel.launchApp(viewModel.getApplication(), tile.packageName, tile.activityName, tile.label)
                                }
                            }
                        }
                    },
                    onTileLongClick = { tileId ->
                        viewModel.onTileLongClicked(tileId)
                    },
                    onTileResize = { tileId ->
                        viewModel.resizeSelectedTile(tileId)
                    },
                    onTileUnpin = { tileId ->
                        viewModel.unpinTile(tileId)
                    },
                    onTileDragStart = { tileId ->
                        viewModel.onTileDragStart(tileId)
                    },
                    onTileDrag = { delta ->
                        viewModel.onTileDrag(delta)
                    },
                    onTileDragEnd = { col, row ->
                        viewModel.onTileDragEnd(col, row)
                    },
                    onExitEditMode = {
                        viewModel.exitEditMode()
                    },
                    onEmptyAreaLongClick = {
                        viewModel.openSettings()
                    }
                )
            }

            // Bottom bar: flat swipe hint to the Apps list (no Material ripple button).
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = MetroDimensions.startHorizontalInset,
                        vertical = 8.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f))

                Box(
                    modifier = Modifier
                        .testTag("navigate_to_apps_button")
                        .size(44.dp)
                        .clickable { onNavigateToApps() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = MetroIcons.Forward,
                        contentDescription = "All Apps",
                        tint = if (wallpaperBitmap != null) Color.White.copy(alpha = 0.8f) else MetroColors.TextDim,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
