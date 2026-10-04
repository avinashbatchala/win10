package com.ab.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ab.model.AppIconPreference
import com.ab.model.LauncherSettings
import com.ab.model.LiveTileAnimationFrequency
import com.ab.model.LauncherOrientation
import com.ab.model.TileSize
import com.ab.ui.icons.MetroIcons
import com.ab.ui.settings.components.MetroButton
import com.ab.ui.settings.components.MetroDropdown
import com.ab.ui.settings.components.MetroInfoRow
import com.ab.ui.settings.components.MetroRadioGroup
import com.ab.ui.settings.components.MetroSectionHeader
import com.ab.ui.settings.components.MetroSettingLink
import com.ab.ui.settings.components.MetroSettingRow
import com.ab.ui.settings.components.MetroSettingsDialog
import com.ab.ui.settings.components.MetroToggle
import com.ab.ui.theme.LocalMetroAccentColor
import com.ab.ui.theme.LocalMetroForeground
import com.ab.ui.theme.LocalMetroSubtleText
import com.ab.ui.theme.MetroTypography
import com.ab.ui.viewmodel.LauncherViewModel

@Composable
internal fun TilesPivotView(
    vm: LauncherViewModel,
    settings: LauncherSettings,
    listState: LazyListState,
    highlightId: String?
) {
    val subtle = LocalMetroSubtleText.current

    val entries = buildList<PivotEntry> {
        add(PivotEntry.Header("tiles.live.header", "LIVE TILES"))
        add(PivotEntry.Setting("tiles.live.enable") {
            MetroSettingRow(
                title = "Enable Live Tiles",
                subtitle = "Turn live tile data on or off",
                trailing = {
                    MetroToggle(
                        checked = settings.liveTilesEnabled,
                        onCheckedChange = { vm.setLiveTilesEnabled(it) },
                        contentDescription = "Enable Live Tiles"
                    )
                }
            )
        })
        add(PivotEntry.Setting("tiles.live.animate") {
            MetroSettingRow(
                title = "Animate Live Tiles",
                subtitle = "Keep data visible while suppressing automatic face transitions",
                enabled = settings.liveTilesEnabled,
                trailing = {
                    MetroToggle(
                        checked = settings.animateLiveTiles,
                        onCheckedChange = { vm.setAnimateLiveTiles(it) },
                        enabled = settings.liveTilesEnabled,
                        contentDescription = "Animate Live Tiles"
                    )
                }
            )
        })
        add(PivotEntry.Setting("tiles.live.frequency") {
            val labels = listOf("Low", "Normal", "High")
            val selected = when (settings.liveTileAnimationFrequency) {
                LiveTileAnimationFrequency.LOW -> 0
                LiveTileAnimationFrequency.NORMAL -> 1
                LiveTileAnimationFrequency.HIGH -> 2
            }
            Column {
                MetroSettingRow(
                    title = "Animation frequency",
                    subtitle = "How often tile faces rotate",
                    enabled = settings.liveTilesEnabled && settings.animateLiveTiles
                )
                MetroDropdown(
                    options = labels,
                    selectedIndex = selected,
                    onSelect = { index ->
                        vm.setLiveTileAnimationFrequency(
                            LiveTileAnimationFrequency.entries[index]
                        )
                    },
                    enabled = settings.liveTilesEnabled && settings.animateLiveTiles,
                    contentDescription = "Animation frequency"
                )
            }
        })

        add(PivotEntry.Header("tiles.behavior.header", "LIVE TILE BEHAVIOR"))
        add(PivotEntry.Setting("tiles.live.pause_hidden") {
            MetroSettingRow(
                title = "Pause animations when launcher is not visible",
                subtitle = "Recommended to save battery",
                enabled = settings.liveTilesEnabled,
                trailing = {
                    MetroToggle(
                        checked = settings.pauseLiveTilesWhenHidden,
                        onCheckedChange = { vm.setPauseLiveTilesWhenHidden(it) },
                        enabled = settings.liveTilesEnabled,
                        contentDescription = "Pause animations when launcher is not visible"
                    )
                }
            )
        })
        add(PivotEntry.Setting("tiles.live.pause_battery") {
            MetroSettingRow(
                title = "Pause animations in battery saver",
                subtitle = "Recommended when Android battery saver is on",
                enabled = settings.liveTilesEnabled,
                trailing = {
                    MetroToggle(
                        checked = settings.pauseLiveTilesInBatterySaver,
                        onCheckedChange = { vm.setPauseLiveTilesInBatterySaver(it) },
                        enabled = settings.liveTilesEnabled,
                        contentDescription = "Pause animations in battery saver"
                    )
                }
            )
        })

        add(PivotEntry.Header("tiles.system.header", "SYSTEM TILES"))
        add(PivotEntry.Setting("tiles.system_tiles") {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Launcher-owned tiles you can pin without adding apps to the apps list.",
                    style = MetroTypography.settingsSubtext.copy(color = subtle),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                SystemTiles.ALL.forEach { def ->
                    SystemTileRow(
                        def = def,
                        pinned = vm.isSystemTilePinned(def.packageName),
                        onToggle = {
                            if (vm.isSystemTilePinned(def.packageName)) {
                                vm.unpinSystemTile(def.packageName)
                            } else {
                                vm.pinSystemTile(def)
                            }
                        }
                    )
                }
            }
        })

        add(PivotEntry.Header("tiles.default.header", "TILES"))
        add(PivotEntry.Setting("tiles.default_size") {
            MetroRadioGroup(
                options = listOf("Small", "Medium"),
                selectedIndex = if (settings.defaultTileSize == TileSize.SMALL) 0 else 1,
                onSelect = { index ->
                    vm.setDefaultTileSize(if (index == 0) TileSize.SMALL else TileSize.MEDIUM)
                }
            )
        })
        add(PivotEntry.Setting("tiles.app_names") {
            MetroSettingRow(
                title = "Show app names on medium and larger tiles",
                trailing = {
                    MetroToggle(
                        checked = settings.showAppNames,
                        onCheckedChange = { vm.setShowAppNames(it) },
                        contentDescription = "Show app names"
                    )
                }
            )
        })
    }

    PivotPage(entries = entries, listState = listState, highlightId = highlightId)
}

@Composable
private fun SystemTileRow(
    def: SystemTileDef,
    pinned: Boolean,
    onToggle: () -> Unit
) {
    val accent = LocalMetroAccentColor.current
    val fg = LocalMetroForeground.current
    val subtle = LocalMetroSubtleText.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = def.icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = def.label, style = MetroTypography.settingsLabel.copy(color = fg))
            Text(
                text = if (pinned) "Pinned to Start" else def.description,
                style = MetroTypography.settingsSubtext.copy(color = subtle),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        MetroButton(
            text = if (pinned) "Unpin" else "Pin",
            onClick = onToggle,
            outlined = !pinned
        )
    }
}

@Composable
internal fun AppsPivotView(
    vm: LauncherViewModel,
    settings: LauncherSettings,
    listState: LazyListState,
    highlightId: String?
) {
    val entries = buildList<PivotEntry> {
        add(PivotEntry.Header("apps.list.header", "APP LIST"))
        add(PivotEntry.Setting("apps.jumplist") {
            MetroSettingRow(
                title = "Alphabet jump list",
                subtitle = "Show the A–Z jump list in the Apps list",
                trailing = {
                    MetroToggle(
                        checked = settings.showAlphabetJumpList,
                        onCheckedChange = { vm.setShowAlphabetJumpList(it) },
                        contentDescription = "Alphabet jump list"
                    )
                }
            )
        })

        add(PivotEntry.Header("apps.icons.header", "ICONS"))
        add(PivotEntry.Setting("apps.icons") {
            Column {
                Text(
                    text = "Icon appearance",
                    style = MetroTypography.settingsLabel.copy(color = LocalMetroForeground.current),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                MetroRadioGroup(
                    options = listOf(
                        "Automatic",
                        "Original icon",
                        "Monochrome when available"
                    ),
                    selectedIndex = when (settings.appIconPreference) {
                        AppIconPreference.AUTOMATIC -> 0
                        AppIconPreference.ORIGINAL_ICON -> 1
                        AppIconPreference.MONOCHROME -> 2
                    },
                    onSelect = { index ->
                        vm.setAppIconPreference(
                            when (index) {
                                1 -> AppIconPreference.ORIGINAL_ICON
                                2 -> AppIconPreference.MONOCHROME
                                else -> AppIconPreference.AUTOMATIC
                            }
                        )
                    }
                )
                Text(
                    text = "Monochrome uses an app's monochrome icon when it provides one; " +
                        "icons are never recoloured artificially.",
                    style = MetroTypography.settingsSubtext.copy(color = LocalMetroSubtleText.current),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        })

        add(PivotEntry.Header("apps.hidden.header", "HIDDEN APPS"))
        add(PivotEntry.Setting("apps.hidden") {
            MetroSettingLink(
                title = "Hidden apps",
                subtitle = "${settings.hiddenApps.size} hidden",
                onClick = { vm.openSettingsDestination(SettingsDestination.HiddenApps) }
            )
        })

        add(PivotEntry.Header("apps.manage.header", "APP MANAGEMENT"))
        add(PivotEntry.Setting("apps.manage") {
            MetroSettingLink(
                title = "Manage apps",
                subtitle = "Pin, hide, inspect or uninstall installed applications",
                onClick = { vm.openSettingsDestination(SettingsDestination.ManageApps) }
            )
        })
    }

    PivotPage(entries = entries, listState = listState, highlightId = highlightId)
}

@Composable
internal fun MediaPivotView(
    vm: LauncherViewModel,
    settings: LauncherSettings,
    listState: LazyListState,
    highlightId: String?
) {
    val context = LocalContext.current
    val subtle = LocalMetroSubtleText.current
    val notificationAccess by vm.mediaSessionRepository.isNotificationAccessGranted.collectAsState()
    val nowPlayingPinned = vm.isSystemTilePinned(SystemTiles.NOW_PLAYING_PACKAGE)

    val entries = buildList<PivotEntry> {
        add(PivotEntry.Header("media.live.header", "MEDIA LIVE TILES"))
        add(PivotEntry.Setting("media.show") {
            MetroSettingRow(
                title = "Show media on Live Tiles",
                subtitle = "Display playback from compatible media apps",
                trailing = {
                    MetroToggle(
                        checked = settings.showMediaLiveTiles,
                        onCheckedChange = { vm.toggleShowMediaLiveTiles(it) },
                        contentDescription = "Show media on Live Tiles"
                    )
                }
            )
        })

        add(PivotEntry.Header("media.access.header", "MEDIA ACCESS"))
        add(PivotEntry.Setting("media.access") {
            Column {
                Text(
                    text = "Media access: " + if (notificationAccess) "Allowed" else "Not allowed",
                    style = MetroTypography.settingsLabel.copy(color = LocalMetroForeground.current)
                )
                Text(
                    text = "Media Live Tiles use Android Notification Access to discover active " +
                        "media sessions. The launcher does not read or store your messages or " +
                        "notifications.",
                    style = MetroTypography.settingsSubtext.copy(color = subtle),
                    modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
                )
                if (!notificationAccess) {
                    MetroButton(
                        text = "Allow media access",
                        onClick = { vm.openNotificationAccessSettings(context) }
                    )
                }
            }
        })

        add(PivotEntry.Header("media.nowplaying.header", "NOW PLAYING"))
        add(PivotEntry.Setting("media.nowplaying") {
            MetroSettingRow(
                title = if (nowPlayingPinned) "Unpin Now Playing from Start" else "Pin Now Playing to Start",
                subtitle = "A launcher-owned tile for the current media session",
                trailing = {
                    MetroButton(
                        text = if (nowPlayingPinned) "Unpin" else "Pin",
                        onClick = {
                            if (nowPlayingPinned) {
                                vm.unpinSystemTile(SystemTiles.NOW_PLAYING_PACKAGE)
                            } else {
                                vm.pinNowPlayingTile()
                            }
                        },
                        outlined = !nowPlayingPinned
                    )
                }
            )
        })

        add(PivotEntry.Header("media.artwork.header", "ARTWORK"))
        add(PivotEntry.Setting("media.artwork") {
            MetroSettingRow(
                title = "Show album artwork",
                subtitle = "Otherwise tiles use the accent colour and a music glyph",
                trailing = {
                    MetroToggle(
                        checked = settings.mediaShowArtwork,
                        onCheckedChange = { vm.setMediaShowArtwork(it) },
                        contentDescription = "Show album artwork"
                    )
                }
            )
        })

        add(PivotEntry.Header("media.playback.header", "PLAYBACK"))
        add(PivotEntry.Setting("media.controls") {
            MetroSettingRow(
                title = "Show controls on wide and large tiles",
                trailing = {
                    MetroToggle(
                        checked = settings.mediaShowControls,
                        onCheckedChange = { vm.setMediaShowControls(it) },
                        contentDescription = "Show playback controls"
                    )
                }
            )
        })
        add(PivotEntry.Setting("media.progress") {
            MetroSettingRow(
                title = "Show playback progress",
                trailing = {
                    MetroToggle(
                        checked = settings.mediaShowProgress,
                        onCheckedChange = { vm.setMediaShowProgress(it) },
                        contentDescription = "Show playback progress"
                    )
                }
            )
        })
    }

    PivotPage(entries = entries, listState = listState, highlightId = highlightId)
}

@Composable
internal fun SystemPivotView(
    vm: LauncherViewModel,
    settings: LauncherSettings,
    listState: LazyListState,
    highlightId: String?
) {
    val context = LocalContext.current
    var showResetLayout by remember { mutableStateOf(false) }
    var showResetAll by remember { mutableStateOf(false) }

    val entries = buildList<PivotEntry> {
        add(PivotEntry.Header("system.home.header", "HOME"))
        add(PivotEntry.Setting("system.home") {
            Column {
                Text(
                    text = "Default Home app",
                    style = MetroTypography.settingsLabel.copy(color = LocalMetroForeground.current)
                )
                Text(
                    text = if (settings.isDefaultLauncher) "Metro Launcher" else "Not default",
                    style = MetroTypography.settingsSubtext.copy(color = LocalMetroSubtleText.current),
                    modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
                )
                if (!settings.isDefaultLauncher) {
                    MetroButton(
                        text = "Set as default",
                        onClick = { vm.requestSetDefaultLauncher(context) }
                    )
                }
            }
        })

        add(PivotEntry.Header("system.nav.header", "NAVIGATION"))
        add(PivotEntry.Setting("system.orientation") {
            Column {
                Text(
                    text = "Screen orientation",
                    style = MetroTypography.settingsLabel.copy(color = LocalMetroForeground.current),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                MetroRadioGroup(
                    options = listOf("Portrait", "Follow system"),
                    selectedIndex = if (settings.launcherOrientation == LauncherOrientation.PORTRAIT) 0 else 1,
                    onSelect = { index ->
                        vm.setLauncherOrientation(
                            if (index == 0) LauncherOrientation.PORTRAIT else LauncherOrientation.FOLLOW_SYSTEM
                        )
                    }
                )
                Text(
                    text = "Landscape is not offered because the Start layout is portrait only.",
                    style = MetroTypography.settingsSubtext.copy(color = LocalMetroSubtleText.current)
                )
            }
        })

        add(PivotEntry.Header("system.reset.header", "RESET"))
        add(PivotEntry.Setting("system.reset_layout") {
            MetroSettingRow(
                title = "Reset Start layout",
                subtitle = "Restore the default arrangement of tiles",
                trailing = {
                    MetroButton(text = "Reset", onClick = { showResetLayout = true })
                }
            )
        })
        add(PivotEntry.Setting("system.reset_all") {
            MetroSettingRow(
                title = "Reset all launcher settings",
                subtitle = "Restore every launcher setting to its default value",
                trailing = {
                    MetroButton(text = "Reset", onClick = { showResetAll = true })
                }
            )
        })
    }

    PivotPage(entries = entries, listState = listState, highlightId = highlightId)

    if (showResetLayout) {
        MetroSettingsDialog(
            title = "Reset Start layout",
            message = "This restores the default tile arrangement. Your apps are not affected.",
            confirmText = "Reset",
            dismissText = "Cancel",
            onConfirm = {
                showResetLayout = false
                vm.resetStartLayout()
            },
            onDismiss = { showResetLayout = false }
        )
    }
    if (showResetAll) {
        MetroSettingsDialog(
            title = "Reset all launcher settings",
            message = "This restores the Start layout and every launcher setting to its default. " +
                "Your apps are not uninstalled.",
            confirmText = "Reset all",
            dismissText = "Cancel",
            onConfirm = {
                showResetAll = false
                vm.resetAllLauncherSettings()
            },
            onDismiss = { showResetAll = false }
        )
    }
}

@Composable
internal fun AboutPivotView(
    vm: LauncherViewModel,
    settings: LauncherSettings,
    listState: LazyListState,
    highlightId: String?
) {
    val installedApps by vm.installedApps.collectAsState()
    val pinnedTiles by vm.pinnedTiles.collectAsState()
    val notificationAccess by vm.mediaSessionRepository.isNotificationAccessGranted.collectAsState()
    var showLicenses by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }

    val entries = buildList<PivotEntry> {
        add(PivotEntry.Setting("about.version") {
            Column(modifier = Modifier.padding(top = 6.dp, bottom = 6.dp)) {
                Text(
                    text = "Win10 Start",
                    style = MetroTypography.settingsTitle.copy(
                        fontSize = 34.sp,
                        color = LocalMetroForeground.current
                    )
                )
                Text(
                    text = "Version ${vm.launcherVersionName()} (${vm.launcherVersionCode()})",
                    style = MetroTypography.settingsSubtext.copy(color = LocalMetroSubtleText.current),
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    text = vm.packageName(),
                    style = MetroTypography.settingsSubtext.copy(color = LocalMetroSubtleText.current)
                )
            }
        })

        add(PivotEntry.Header("about.diag.header", "DIAGNOSTICS"))
        add(PivotEntry.Setting("about.diagnostics") {
            Column {
                MetroInfoRow("Android version", vm.androidVersion())
                MetroInfoRow("Device", vm.deviceModel())
                MetroInfoRow(
                    "Default Home",
                    if (settings.isDefaultLauncher) "Launcher" else "Not default"
                )
                MetroInfoRow(
                    "Media access",
                    if (notificationAccess) "Allowed" else "Not allowed"
                )
                MetroInfoRow("Installed apps", installedApps.size.toString())
                MetroInfoRow("Pinned tiles", pinnedTiles.size.toString())
                MetroInfoRow("Live Tile providers", vm.activeLiveTileProviderCount().toString())
                MetroInfoRow(
                    "Live Tile scheduler",
                    if (vm.isLiveTileSchedulerRunning()) "Running" else "Paused"
                )
                MetroInfoRow(
                    "Grid density",
                    if (settings.showMoreTiles) "8 columns" else "6 columns"
                )
                MetroInfoRow("Theme", if (settings.darkTheme) "Dark" else "Light")
            }
        })

        add(PivotEntry.Header("about.legal.header", "LEGAL"))
        add(PivotEntry.Setting("about.licenses") {
            MetroSettingLink(
                title = "Open source licenses",
                subtitle = "Software used by the launcher",
                onClick = { showLicenses = true }
            )
        })
        add(PivotEntry.Setting("about.privacy") {
            MetroSettingLink(
                title = "Privacy",
                subtitle = "How the launcher handles your data",
                onClick = { showPrivacy = true }
            )
        })
    }

    PivotPage(entries = entries, listState = listState, highlightId = highlightId)

    if (showLicenses) {
        MetroSettingsDialog(
            title = "Open source licenses",
            message = "This launcher is built with Android Jetpack, Jetpack Compose, " +
                "Kotlin Coroutines, OkHttp, Retrofit, Moshi and Room, each licensed under " +
                "the Apache License 2.0. Open Sans is licensed under the Apache License 2.0.",
            confirmText = "Close",
            dismissText = "Close",
            onConfirm = { showLicenses = false },
            onDismiss = { showLicenses = false }
        )
    }
    if (showPrivacy) {
        MetroSettingsDialog(
            title = "Privacy",
            message = "Launcher settings and your pinned tile layout are stored only on this device. " +
                "Media Live Tiles read active media session metadata through Android Notification " +
                "Access; message and notification contents are never read or stored, and nothing is " +
                "sent off the device.",
            confirmText = "Close",
            dismissText = "Close",
            onConfirm = { showPrivacy = false },
            onDismiss = { showPrivacy = false }
        )
    }
}
