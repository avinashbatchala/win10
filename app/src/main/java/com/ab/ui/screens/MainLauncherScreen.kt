package com.ab.ui.screens

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
    val requestScrollToStart by viewModel.requestScrollToStart.collectAsState()

    // Handle scroll to Start request (e.g. from Home button / intent)
    LaunchedEffect(requestScrollToStart) {
        if (requestScrollToStart) {
            if (pagerState.currentPage != 0) {
                pagerState.scrollToPage(0)
            }
            viewModel.onScrollToStartHandled()
        }
    }

    // Universal Back handling in strict priority order:
    // 1. Modal/Settings/Dialog
    // 2. Tile edit mode
    // 3. Search query/active
    // 4. Alphabet jump overlay
    // 5. Navigate from Apps to Start
    // 6. Root Start screen: consume Back and remain idle (no relaunch, no loop)
    BackHandler(enabled = true) {
        Log.d("LauncherBack", "Back gesture received. Current page: ${pagerState.currentPage}")
        if (viewModel.handleInternalBack()) {
            return@BackHandler
        }
        if (pagerState.currentPage != 0) {
            Log.d("LauncherNav", "Back navigating from Apps page to Start page.")
            scope.launch {
                pagerState.animateScrollToPage(0)
            }
            return@BackHandler
        }
        Log.d("LauncherBack", "Root Start screen reached: Back gesture consumed, remaining idle.")
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
