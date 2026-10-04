package com.ab.ui.screens

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.ab.ui.settings.AppDetailsScreen
import com.ab.ui.settings.HiddenAppsScreen
import com.ab.ui.settings.LauncherSettingsScreen
import com.ab.ui.settings.ManageAppsScreen
import com.ab.ui.settings.SettingsDestination
import com.ab.ui.settings.SettingsRootScreen
import com.ab.ui.viewmodel.LauncherViewModel
import kotlinx.coroutines.launch

@Composable
fun MainLauncherScreen(
    viewModel: LauncherViewModel,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 2 })

    val tiles by viewModel.pinnedTiles.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val isEditMode by viewModel.isEditMode.collectAsState()
    val selectedTileId by viewModel.selectedTileId.collectAsState()
    val isSettingsOpen by viewModel.isSettingsOpen.collectAsState()
    val settingsStack by viewModel.settingsBackStack.collectAsState()
    val requestScrollToStart by viewModel.requestScrollToStart.collectAsState()
    val currentSettingsDestination = settingsStack.lastOrNull()
    // Keep the last destination during the exit animation so Settings does not blank out.
    var lastSettingsDestination by remember { mutableStateOf<SettingsDestination?>(null) }
    if (currentSettingsDestination != null) lastSettingsDestination = currentSettingsDestination
    val shownSettingsDestination = currentSettingsDestination ?: lastSettingsDestination

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
            .background(com.ab.ui.theme.LocalMetroBackground.current)
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

        // Windows 10 Mobile-style Settings host
        AnimatedVisibility(
            visible = isSettingsOpen && currentSettingsDestination != null,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            when (val dest = shownSettingsDestination) {
                null -> Box(modifier = Modifier.fillMaxSize())
                SettingsDestination.Root -> SettingsRootScreen(
                    onOpenPivot = { pivot ->
                        viewModel.openSettingsDestination(SettingsDestination.Launcher(pivot))
                    },
                    onOpenDestination = { destination ->
                        viewModel.openSettingsDestination(destination)
                    },
                    onBack = { viewModel.navigateSettingsBack() }
                )
                is SettingsDestination.Launcher -> LauncherSettingsScreen(
                    viewModel = viewModel,
                    initialPivot = dest.pivot,
                    scrollToSettingId = dest.scrollToSettingId,
                    onBack = { viewModel.navigateSettingsBack() }
                )
                SettingsDestination.HiddenApps -> HiddenAppsScreen(
                    vm = viewModel,
                    onBack = { viewModel.navigateSettingsBack() },
                    onAppClick = { pkg ->
                        viewModel.openSettingsDestination(SettingsDestination.AppDetails(pkg))
                    }
                )
                SettingsDestination.ManageApps -> ManageAppsScreen(
                    vm = viewModel,
                    onBack = { viewModel.navigateSettingsBack() },
                    onAppClick = { pkg ->
                        viewModel.openSettingsDestination(SettingsDestination.AppDetails(pkg))
                    }
                )
                is SettingsDestination.AppDetails -> AppDetailsScreen(
                    vm = viewModel,
                    packageName = dest.packageName,
                    onBack = { viewModel.navigateSettingsBack() }
                )
            }
        }
    }
}
