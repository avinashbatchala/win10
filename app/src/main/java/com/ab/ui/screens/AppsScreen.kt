package com.ab.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.ab.model.AppInfo
import com.ab.ui.components.AlphabetJumpList
import com.ab.ui.components.AppListRow
import com.ab.ui.theme.MetroColors
import com.ab.ui.theme.MetroDimensions
import com.ab.ui.theme.MetroTypography
import com.ab.ui.viewmodel.LauncherViewModel
import kotlinx.coroutines.launch

@Composable
fun AppsScreen(
    viewModel: LauncherViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val accentColor = Color(settings.accentColor)
    val context = LocalContext.current

    val searchQuery by viewModel.searchQuery.collectAsState()
    val isJumpListOpen by viewModel.isJumpListOpen.collectAsState()
    val activeLetters by viewModel.activeLetters.collectAsState()
    val filteredApps by viewModel.filteredApps.collectAsState()

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Group apps by letter when not searching
    val isSearching = searchQuery.isNotBlank()
    val groupedApps: Map<Char, List<AppInfo>> = if (!isSearching) {
        filteredApps.groupBy { it.firstLetter }
    } else {
        emptyMap()
    }

    // Precalculate item index for each letter header
    val letterIndexMap = mutableMapOf<Char, Int>()
    var runningIndex = 0
    for ((letter, apps) in groupedApps) {
        letterIndexMap[letter] = runningIndex
        runningIndex += 1 + apps.size // 1 for header + items
    }

    val isDark = com.ab.ui.theme.LocalMetroDarkTheme.current
    val bgColor = com.ab.ui.theme.LocalMetroBackground.current
    val fgColor = com.ab.ui.theme.LocalMetroForeground.current

    Box(
        modifier = modifier
            .testTag("apps_screen")
            .fillMaxSize()
            .background(bgColor)
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Windows-style Search Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = MetroDimensions.appListHorizontalInset,
                        vertical = 12.dp
                    )
            ) {
                Row(
                    modifier = Modifier
                        .testTag("apps_search_box")
                        .fillMaxWidth()
                        .height(MetroDimensions.searchBoxHeight)
                        .background(if (isDark) Color.Black else Color.White, RectangleShape)
                        .border(
                            MetroDimensions.searchBoxBorderWidth,
                            if (isDark) MetroColors.SearchBorderDark else MetroColors.SearchBorderLight,
                            RectangleShape
                        )
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = com.ab.ui.icons.MetroIcons.Search,
                        contentDescription = "Search",
                        tint = if (isDark) MetroColors.TextDim else MetroColors.TextSubtle,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search",
                                style = MetroTypography.searchHint.copy(
                                    color = if (isDark) MetroColors.TextDim else MetroColors.TextSubtle
                                )
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            singleLine = true,
                            textStyle = MetroTypography.searchInput.copy(color = fgColor),
                            cursorBrush = SolidColor(fgColor),
                            modifier = Modifier
                                .testTag("apps_search_input")
                                .fillMaxWidth()
                        )
                    }

                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = com.ab.ui.icons.MetroIcons.Close,
                            contentDescription = "Clear search",
                            tint = fgColor,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable { viewModel.setSearchQuery("") }
                        )
                    }
                }
            }

            // App List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .testTag("apps_lazy_column")
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (isSearching) {
                    // Flat search results
                    items(
                        items = filteredApps,
                        key = { it.key }
                    ) { app ->
                        AppListRow(
                            app = app,
                            isPinned = viewModel.isAppPinned(app.packageName),
                            accentColor = accentColor,
                            onClick = {
                                viewModel.launchApp(viewModel.getApplication(), app.packageName, app.activityName, app.label)
                            },
                            onPinToStart = {
                                viewModel.pinApp(app)
                            },
                            onUnpinFromStart = {
                                viewModel.unpinAppByPackage(app.packageName)
                            },
                            onUninstall = {
                                viewModel.uninstallApp(context, app.packageName)
                            }
                        )
                    }
                } else {
                    // Alphabetical grouped list
                    for ((letter, apps) in groupedApps) {
                        // Section Header
                        item(key = "header_$letter") {
                            Box(
                                modifier = Modifier
                                    .testTag("group_header_$letter")
                                    .padding(
                                        start = MetroDimensions.appListHorizontalInset,
                                        top = 16.dp,
                                        bottom = 8.dp
                                    )
                                    .size(MetroDimensions.appListHeaderBoxSize)
                                    .background(accentColor, RectangleShape)
                                    .clickable(enabled = settings.showAlphabetJumpList) {
                                        viewModel.openJumpList()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = letter.toString(),
                                    style = MetroTypography.appListGroupHeader
                                )
                            }
                        }

                        // App items under this header
                        items(
                            items = apps,
                            key = { it.key }
                        ) { app ->
                            AppListRow(
                                app = app,
                                isPinned = viewModel.isAppPinned(app.packageName),
                                accentColor = accentColor,
                                onClick = {
                                    viewModel.launchApp(viewModel.getApplication(), app.packageName, app.activityName, app.label)
                                },
                                onPinToStart = {
                                    viewModel.pinApp(app)
                                },
                                onUnpinFromStart = {
                                    viewModel.unpinAppByPackage(app.packageName)
                                },
                                onUninstall = {
                                    viewModel.uninstallApp(context, app.packageName)
                                }
                            )
                        }
                    }
                }

                // Bottom padding
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }

        // Jump List Overlay
        if (settings.showAlphabetJumpList) {
            AlphabetJumpList(
                isOpen = isJumpListOpen,
                activeLetters = activeLetters,
                accentColor = accentColor,
                onLetterSelected = { selectedLetter ->
                    val targetIndex = letterIndexMap[selectedLetter]
                    if (targetIndex != null) {
                        scope.launch {
                            listState.scrollToItem(targetIndex)
                        }
                    }
                },
                onDismiss = {
                    viewModel.closeJumpList()
                }
            )
        }
    }
}
