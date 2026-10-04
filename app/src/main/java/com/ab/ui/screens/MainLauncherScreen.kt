package com.ab.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.ab.ui.components.SettingsSheet
import com.ab.ui.theme.MetroColors
import com.ab.ui.viewmodel.LauncherViewModel
import kotlinx.coroutines.launch

@Composable
fun MainLauncherScreen(
    viewModel: LauncherViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 2 })

    val tiles by viewModel.pinnedTiles.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val isEditMode by viewModel.isEditMode.collectAsState()
    val selectedTileId by viewModel.selectedTileId.collectAsState()
    val isSettingsOpen by viewModel.isSettingsOpen.collectAsState()

    // Back button handling: if on Apps page, glide back to Start
    BackHandler(enabled = pagerState.currentPage == 1 && !isSettingsOpen) {
        scope.launch {
            pagerState.animateScrollToPage(0)
        }
    }

    Box(
        modifier = modifier
            .testTag("main_launcher_container")
            .fillMaxSize()
            .background(MetroColors.BackgroundBlack)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> StartScreen(
                    viewModel = viewModel,
                    tiles = tiles,
                    settings = settings,
                    isEditMode = isEditMode,
                    selectedTileId = selectedTileId,
                    onNavigateToApps = {
                        scope.launch {
                            pagerState.animateScrollToPage(1)
                        }
                    }
                )
                1 -> AppsScreen(
                    viewModel = viewModel
                )
            }
        }

        // Settings sheet overlay
        SettingsSheet(
            isOpen = isSettingsOpen,
            settings = settings,
            onAccentColorSelected = { colorLong ->
                viewModel.setAccentColor(colorLong)
            },
            onToggleShowMoreTiles = {
                viewModel.toggleShowMoreTiles()
            },
            onRequestSetDefault = {
                viewModel.requestSetDefaultLauncher(context)
            },
            onClose = {
                viewModel.closeSettings()
            }
        )
    }
}
