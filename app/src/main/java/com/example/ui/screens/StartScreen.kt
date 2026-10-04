package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.LauncherSettings
import com.example.model.TileModel
import com.example.ui.components.StartGrid
import com.example.ui.theme.MetroColors
import com.example.ui.theme.MetroDimensions
import com.example.ui.theme.MetroTypography
import com.example.ui.viewmodel.LauncherViewModel

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

    BackHandler(enabled = isEditMode) {
        viewModel.exitEditMode()
    }

    Box(
        modifier = modifier
            .testTag("start_screen")
            .fillMaxSize()
            .background(MetroColors.BackgroundBlack)
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Subtle top bar with title and quick settings
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MetroDimensions.startHorizontalInset, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Start",
                    style = MetroTypography.startTitle,
                    modifier = Modifier.weight(1f)
                )

                // Settings icon
                IconButton(
                    onClick = { viewModel.openSettings() },
                    modifier = Modifier.testTag("start_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Personalization Settings",
                        tint = MetroColors.TextDim,
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
                    isEditMode = isEditMode,
                    selectedTileId = selectedTileId,
                    draggedTileId = viewModel.draggedTileId.value,
                    dragOffset = viewModel.dragOffset.value,
                    getAppIcon = { pkg -> viewModel.getAppIcon(pkg) },
                    onTileClick = { tile ->
                        if (isEditMode) {
                            viewModel.onTileClickedInEdit(tile.id)
                        } else {
                            viewModel.launchApp(viewModel.getApplication(), tile.packageName, tile.activityName)
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
                    }
                )
            }
        }

        // Windows-style arrow affordance near bottom-right to glide to all apps
        AnimatedVisibility(
            visible = !isEditMode,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 18.dp)
        ) {
            Box(
                modifier = Modifier
                    .testTag("nav_to_apps_button")
                    .size(38.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, Color(0x66FFFFFF), CircleShape)
                    .clickable { onNavigateToApps() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "All Apps",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
