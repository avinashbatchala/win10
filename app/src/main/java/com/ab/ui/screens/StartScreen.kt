package com.ab.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.ab.model.LauncherSettings
import com.ab.model.TileModel
import com.ab.ui.components.StartGrid
import com.ab.ui.icons.MetroIcons
import com.ab.ui.theme.LocalMetroBackground
import com.ab.ui.theme.LocalMetroForeground
import com.ab.ui.theme.MetroColors
import com.ab.ui.theme.MetroDimensions
import com.ab.ui.theme.MetroTypography
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
    val bgColor = LocalMetroBackground.current
    val fgColor = if (wallpaperBitmap != null) Color.White else LocalMetroForeground.current

    Box(
        modifier = modifier
            .testTag("start_screen")
            .fillMaxSize()
            .background(bgColor)
    ) {
        // Full Start-screen wallpaper background if selected
        if (wallpaperBitmap != null) {
            Image(
                bitmap = wallpaperBitmap!!,
                contentDescription = "Start wallpaper background",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top bar with Start title and Personalization trigger
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MetroDimensions.startHorizontalInset, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Start",
                    style = MetroTypography.startTitle.copy(color = fgColor),
                    modifier = Modifier.weight(1f)
                )

                // Personalization Settings icon with Metro glyph
                IconButton(
                    onClick = { viewModel.openSettings() },
                    modifier = Modifier.testTag("start_settings_button")
                ) {
                    Icon(
                        imageVector = MetroIcons.Settings,
                        contentDescription = "Personalization Settings",
                        tint = if (wallpaperBitmap != null) Color.White.copy(alpha = 0.8f) else MetroColors.TextDim,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Start Grid containing tiles
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
                    getLiveTileState = { pkg, act -> viewModel.getLiveTileState(pkg, act) },
                    mediaActionDispatcher = viewModel.mediaActionDispatcher,
                    showAppNames = settings.showAppNames,
                    mediaShowControls = settings.mediaShowControls,
                    mediaShowProgress = settings.mediaShowProgress,
                    onTileClick = { tile ->
                        if (isEditMode) {
                            viewModel.onTileClickedInEdit(tile.id)
                        } else {
                            if (tile.packageName == "livetile.nowplaying") {
                                val primaryPkg = viewModel.mediaSessionRepository.primarySession.value?.packageName
                                if (primaryPkg != null) {
                                    viewModel.launchApp(viewModel.getApplication(), primaryPkg, "", tile.label)
                                }
                            } else {
                                viewModel.launchApp(viewModel.getApplication(), tile.packageName, tile.activityName, tile.label)
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

            // Bottom bar: subtle swipe hint or arrow to Apps list
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

                IconButton(
                    onClick = onNavigateToApps,
                    modifier = Modifier.testTag("navigate_to_apps_button")
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
