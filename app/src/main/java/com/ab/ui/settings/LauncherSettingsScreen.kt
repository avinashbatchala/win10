package com.ab.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ab.model.BackgroundStyle
import com.ab.model.LauncherSettings
import com.ab.model.TileSize
import com.ab.ui.icons.MetroIcons
import com.ab.ui.settings.components.MetroAccentGrid
import com.ab.ui.settings.components.MetroButton
import com.ab.ui.settings.components.MetroRadioGroup
import com.ab.ui.settings.components.MetroSectionHeader
import com.ab.ui.settings.components.MetroSettingRow
import com.ab.ui.settings.components.MetroSlider
import com.ab.ui.settings.components.MetroToggle
import com.ab.ui.settings.components.SettingsPageInset
import com.ab.ui.theme.LocalMetroAccentColor
import com.ab.ui.theme.LocalMetroBackground
import com.ab.ui.theme.LocalMetroDarkTheme
import com.ab.ui.theme.LocalMetroForeground
import com.ab.ui.theme.LocalMetroSubtleText
import com.ab.ui.theme.MetroTypography
import com.ab.ui.viewmodel.LauncherViewModel
import kotlinx.coroutines.launch

/** A single item in a pivot page. */
sealed interface PivotEntry {
    val id: String

    class Header(override val id: String, val title: String) : PivotEntry
    class Setting(override val id: String, val content: @Composable () -> Unit) : PivotEntry
}

/**
 * Windows 10 Mobile launcher Settings shell: a small "SETTINGS" label, large horizontally
 * arranged pivot headings and swipeable pivot pages. Each page keeps its own vertical scroll.
 */
@Composable
fun LauncherSettingsScreen(
    viewModel: LauncherViewModel,
    initialPivot: SettingsPivot,
    scrollToSettingId: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pivots = SettingsPivot.entries
    val settings by viewModel.settings.collectAsState()
    val accent = LocalMetroAccentColor.current
    val bg = LocalMetroBackground.current
    val fg = LocalMetroForeground.current
    val subtle = LocalMetroSubtleText.current

    val pagerState = rememberPagerState(
        initialPage = initialPivot.ordinal.coerceIn(0, pivots.size - 1),
        pageCount = { pivots.size }
    )
    val listStates = remember { List(pivots.size) { LazyListState() } }
    val scope = rememberCoroutineScope()

    LaunchedEffect(initialPivot) {
        val target = initialPivot.ordinal.coerceIn(0, pivots.size - 1)
        if (pagerState.currentPage != target) pagerState.animateScrollToPage(target)
    }

    Column(
        modifier = modifier
            .testTag("launcher_settings")
            .fillMaxSize()
            .background(bg)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = SettingsPageInset - 8.dp, top = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .testTag("launcher_settings_back")
                    .size(44.dp)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = MetroIcons.Back,
                    contentDescription = "Back to Settings",
                    tint = fg,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = "SETTINGS",
                style = MetroTypography.settingsSection.copy(color = subtle)
            )
        }

        PivotHeader(
            titles = pivots.map { it.title },
            pagerState = pagerState,
            accent = accent,
            fg = fg,
            subtle = subtle,
            onSelect = { index -> scope.launch { pagerState.animateScrollToPage(index) } }
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { page ->
            val pivot = pivots[page]
            val highlight = if (pivot == initialPivot) scrollToSettingId else null
            when (pivot) {
                SettingsPivot.START -> StartPivotView(viewModel, settings, listStates[page], highlight)
                SettingsPivot.TILES -> TilesPivotView(viewModel, settings, listStates[page], highlight)
                SettingsPivot.APPS -> AppsPivotView(viewModel, settings, listStates[page], highlight)
                SettingsPivot.MEDIA -> MediaPivotView(viewModel, settings, listStates[page], highlight)
                SettingsPivot.SYSTEM -> SystemPivotView(viewModel, settings, listStates[page], highlight)
                SettingsPivot.ABOUT -> AboutPivotView(viewModel, settings, listStates[page], highlight)
            }
        }
    }
}

/**
 * Custom pivot headings. The whole strip translates in step with the pager so it reads as one
 * continuous horizontally arranged surface, with following headings visible to the right.
 */
@Composable
private fun PivotHeader(
    titles: List<String>,
    pagerState: androidx.compose.foundation.pager.PagerState,
    accent: Color,
    fg: Color,
    subtle: Color,
    onSelect: (Int) -> Unit
) {
    val headerWidths = remember { mutableStateListOf(*Array(titles.size) { 0 }) }

    // Cumulative offset to align each heading to the left edge. Each heading's measured
    // width already includes its trailing gap (end padding), so do NOT add the gap again.
    val offsets = IntArray(titles.size)
    var acc = 0
    for (i in titles.indices) {
        offsets[i] = acc
        acc += headerWidths.getOrElse(i) { 0 }
    }
    val endOffset = acc

    val page = pagerState.currentPage
    val fraction = pagerState.currentPageOffsetFraction
    val currentStart = offsets.getOrElse(page) { 0 }.toFloat()
    val nextStart = if (page + 1 < titles.size) offsets[page + 1].toFloat() else endOffset.toFloat()
    val translation = currentStart + fraction * (nextStart - currentStart)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = SettingsPageInset)
            .clipToBounds()
    ) {
        Row(
            modifier = Modifier
                .wrapContentWidth(align = Alignment.Start, unbounded = true)
                .graphicsLayer { translationX = -translation },
            verticalAlignment = Alignment.CenterVertically
        ) {
            titles.forEachIndexed { index, title ->
                val active = index == page
                Text(
                    text = title,
                    style = MetroTypography.pageHeader.copy(
                        fontSize = 30.sp,
                        color = if (active) fg else subtle
                    ),
                    modifier = Modifier
                        .testTag("pivot_header_$title")
                        .onGloballyPositioned { headerWidths[index] = it.size.width }
                        .clickable { onSelect(index) }
                        .padding(
                            end = 28.dp,
                            top = 4.dp,
                            bottom = 10.dp
                        )
                )
            }
        }
    }
}

/** Renders a pivot page as an independent vertical LazyColumn and handles search scroll-to. */
@Composable
internal fun PivotPage(
    entries: List<PivotEntry>,
    listState: LazyListState,
    highlightId: String?,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(highlightId) {
        if (highlightId != null) {
            val index = entries.indexOfFirst { it.id == highlightId }
            if (index >= 0) listState.animateScrollToItem(index)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = SettingsPageInset)
    ) {
        entries.forEach { entry ->
            when (entry) {
                is PivotEntry.Header -> item(key = "header_${entry.id}") {
                    MetroSectionHeader(entry.title)
                }
                is PivotEntry.Setting -> item(key = entry.id) {
                    entry.content()
                }
            }
        }
        item(key = "bottom_spacer") { Spacer(modifier = Modifier.height(56.dp)) }
    }
}

@Composable
private fun StartPivotView(
    vm: LauncherViewModel,
    settings: LauncherSettings,
    listState: LazyListState,
    highlightId: String?
) {
    val context = LocalContext.current
    val accent = LocalMetroAccentColor.current
    val subtle = LocalMetroSubtleText.current
    val wallpaper by vm.wallpaperBitmap.collectAsState()

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }
            vm.setBackgroundImageUri(uri.toString())
            if (settings.backgroundStyle == BackgroundStyle.NONE) {
                vm.setBackgroundStyle(BackgroundStyle.FULL_SCREEN)
            }
        }
    }

    val transparencyEnabled = settings.backgroundImageUri != null &&
        settings.backgroundStyle == BackgroundStyle.FULL_SCREEN

    val entries = buildList<PivotEntry> {
        add(PivotEntry.Header("start.preview.header", "PREVIEW"))
        add(PivotEntry.Setting("start.preview") {
            StartPreview(
                settings = settings,
                wallpaper = wallpaper,
                accent = accent
            )
        })

        add(PivotEntry.Header("start.background.header", "BACKGROUND"))
        add(PivotEntry.Setting("start.background") {
            Column {
                Text(
                    text = when {
                        settings.backgroundImageUri == null -> "No background picture"
                        settings.backgroundStyle == BackgroundStyle.TILE_PICTURE -> "Tile picture"
                        else -> "Full screen picture"
                    },
                    style = MetroTypography.settingsLabel.copy(
                        color = LocalMetroForeground.current
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetroButton(
                        text = "Browse",
                        onClick = {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )
                    if (settings.backgroundImageUri != null) {
                        MetroButton(
                            text = "Remove",
                            onClick = {
                                vm.setBackgroundImageUri(null)
                                vm.setBackgroundStyle(BackgroundStyle.NONE)
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Choose style",
                    style = MetroTypography.settingsSubtext.copy(color = subtle),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                MetroRadioGroup(
                    options = listOf("None", "Full screen picture", "Tile picture"),
                    selectedIndex = when (settings.backgroundStyle) {
                        BackgroundStyle.NONE -> 0
                        BackgroundStyle.FULL_SCREEN -> 1
                        BackgroundStyle.TILE_PICTURE -> 2
                    },
                    onSelect = { index ->
                        when (index) {
                            0 -> vm.setBackgroundStyle(BackgroundStyle.NONE)
                            1 -> vm.setBackgroundStyle(BackgroundStyle.FULL_SCREEN)
                            2 -> vm.setBackgroundStyle(BackgroundStyle.TILE_PICTURE)
                        }
                    }
                )
                Text(
                    text = "Tile picture shows the photo inside the tiles; " +
                        "full screen shows it behind translucent tiles.",
                    style = MetroTypography.settingsSubtext.copy(color = subtle),
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        })

        add(PivotEntry.Header("start.transparency.header", "TILE TRANSPARENCY"))
        add(PivotEntry.Setting("start.transparency") {
            val percentage = (settings.tileTransparency * 100f).toInt()
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Tile transparency",
                        style = MetroTypography.settingsLabel.copy(color = LocalMetroForeground.current)
                    )
                    Text(
                        text = "$percentage%",
                        style = MetroTypography.settingsLabel.copy(color = LocalMetroForeground.current)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                MetroSlider(
                    value = settings.tileTransparency,
                    onValueChange = { vm.setTileTransparency(it) },
                    enabled = transparencyEnabled,
                    contentDescription = "Tile transparency"
                )
                Text(
                    text = if (transparencyEnabled) {
                        "0% is fully opaque; 100% is transparent."
                    } else {
                        "Transparency is available for a full screen background picture."
                    },
                    style = MetroTypography.settingsSubtext.copy(color = subtle)
                )
            }
        })

        add(PivotEntry.Header("start.color.header", "CHOOSE YOUR COLOR"))
        add(PivotEntry.Setting("start.color") {
            MetroAccentGrid(
                selectedColor = settings.accentColor,
                onSelect = { vm.setAccentColor(it) }
            )
        })

        add(PivotEntry.Header("start.theme.header", "THEME"))
        add(PivotEntry.Setting("start.theme") {
            MetroRadioGroup(
                options = listOf("Dark", "Light"),
                selectedIndex = if (settings.darkTheme) 0 else 1,
                onSelect = { vm.setTheme(it == 0) }
            )
        })

        add(PivotEntry.Header("start.density.header", "START"))
        add(PivotEntry.Setting("start.density") {
            MetroSettingRow(
                title = "Show more tiles",
                subtitle = if (settings.showMoreTiles) {
                    "Dense 6-column layout"
                } else {
                    "Standard 4-column layout"
                },
                trailing = {
                    MetroToggle(
                        checked = settings.showMoreTiles,
                        onCheckedChange = { vm.setShowMoreTiles(it) },
                        contentDescription = "Show more tiles"
                    )
                }
            )
        })
    }

    PivotPage(entries = entries, listState = listState, highlightId = highlightId)
}

/** Lightweight Start preview. Does not render the real launcher recursively. */
@Composable
private fun StartPreview(
    settings: LauncherSettings,
    wallpaper: androidx.compose.ui.graphics.ImageBitmap?,
    accent: Color
) {
    val isDark = LocalMetroDarkTheme.current
    val columns = if (settings.showMoreTiles) 6 else 4
    val tileAlpha = (1f - settings.tileTransparency).coerceIn(0f, 1f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .background(if (isDark) Color.Black else Color.White, RectangleShape)
            .clipToBounds()
    ) {
        if (settings.backgroundStyle == BackgroundStyle.FULL_SCREEN && wallpaper != null) {
            Image(
                bitmap = wallpaper,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(3) { rowIndex ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(columns) { colIndex ->
                        val filled = (rowIndex + colIndex) % 3 != 0
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(30.dp)
                                .background(
                                    if (filled) accent.copy(alpha = tileAlpha)
                                    else Color.Transparent,
                                    RectangleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (filled &&
                                settings.backgroundStyle == BackgroundStyle.TILE_PICTURE &&
                                wallpaper != null
                            ) {
                                Image(
                                    bitmap = wallpaper,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
