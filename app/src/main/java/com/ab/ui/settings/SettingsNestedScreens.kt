package com.ab.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ab.model.AppInfo
import com.ab.ui.components.LauncherIconView
import com.ab.ui.icons.MetroIcons
import com.ab.ui.settings.components.MetroButton
import com.ab.ui.settings.components.MetroSettingRow
import com.ab.ui.settings.components.MetroToggle
import com.ab.ui.settings.components.SettingsPageInset
import com.ab.ui.theme.LocalMetroBackground
import com.ab.ui.theme.LocalMetroForeground
import com.ab.ui.theme.LocalMetroSubtleText
import com.ab.ui.theme.MetroDimensions
import com.ab.ui.theme.MetroTypography
import com.ab.ui.viewmodel.LauncherViewModel

@Composable
private fun SettingsDetailScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val bg = LocalMetroBackground.current
    val fg = LocalMetroForeground.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bg)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = SettingsPageInset - 8.dp, end = SettingsPageInset, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = MetroIcons.Back,
                    contentDescription = "Back",
                    tint = fg,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Text(
            text = title,
            style = MetroTypography.settingsTitle.copy(color = fg, fontSize = 34.sp),
            modifier = Modifier.padding(horizontal = SettingsPageInset, vertical = 6.dp)
        )
        content()
    }
}

@Composable
internal fun HiddenAppsScreen(
    vm: LauncherViewModel,
    onBack: () -> Unit,
    onAppClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val apps by vm.installedApps.collectAsState()
    val settings by vm.settings.collectAsState()

    SettingsDetailScaffold(title = "Hidden apps", onBack = onBack, modifier = modifier) {
        Text(
            text = "Hidden apps stay installed and any pinned tiles remain on Start.",
            style = MetroTypography.settingsSubtext.copy(color = LocalMetroSubtleText.current),
            modifier = Modifier.padding(horizontal = SettingsPageInset, vertical = 4.dp)
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = SettingsPageInset)
        ) {
            items(items = apps, key = { it.key }) { app ->
                AppToggleRow(
                    app = app,
                    checked = settings.hiddenApps.contains(app.packageName),
                    onCheckedChange = { hidden -> vm.setAppHidden(app.packageName, hidden) },
                    onClick = { onAppClick(app.packageName) }
                )
            }
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
internal fun ManageAppsScreen(
    vm: LauncherViewModel,
    onBack: () -> Unit,
    onAppClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val apps by vm.installedApps.collectAsState()
    val settings by vm.settings.collectAsState()

    SettingsDetailScaffold(title = "Manage apps", onBack = onBack, modifier = modifier) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = SettingsPageInset)
        ) {
            items(items = apps, key = { it.key }) { app ->
                val pinned = vm.isAppPinned(app.packageName)
                val hidden = settings.hiddenApps.contains(app.packageName)
                val status = buildList {
                    if (pinned) add("Pinned")
                    if (hidden) add("Hidden")
                }.joinToString(" · ").ifEmpty { "Not pinned" }
                AppInfoRow(
                    app = app,
                    subtitle = status,
                    onClick = { onAppClick(app.packageName) }
                )
            }
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
internal fun AppDetailsScreen(
    vm: LauncherViewModel,
    packageName: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val apps by vm.installedApps.collectAsState()
    val settings by vm.settings.collectAsState()
    val pinnedTiles by vm.pinnedTiles.collectAsState()
    val app = apps.firstOrNull { it.packageName == packageName }

    SettingsDetailScaffold(
        title = app?.label ?: packageName,
        onBack = onBack,
        modifier = modifier
    ) {
        if (app == null) {
            Text(
                text = "This app is no longer installed.",
                style = MetroTypography.settingsSubtext.copy(color = LocalMetroSubtleText.current),
                modifier = Modifier.padding(SettingsPageInset)
            )
        } else {
        val pinned = pinnedTiles.any { it.packageName == packageName }
        val hidden = settings.hiddenApps.contains(packageName)

        Column(modifier = Modifier.padding(horizontal = SettingsPageInset)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(app)
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.label,
                        style = MetroTypography.settingsLabel.copy(color = LocalMetroForeground.current)
                    )
                    Text(
                        text = app.packageName,
                        style = MetroTypography.settingsSubtext.copy(color = LocalMetroSubtleText.current),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            MetroSettingRow(
                title = if (pinned) "Unpin from Start" else "Pin to Start",
                trailing = {
                    MetroButton(
                        text = if (pinned) "Unpin" else "Pin",
                        onClick = {
                            if (pinned) {
                                vm.unpinAppByPackage(packageName)
                            } else {
                                vm.pinApp(app)
                            }
                        },
                        outlined = !pinned
                    )
                }
            )

            MetroSettingRow(
                title = if (hidden) "Show in Apps list" else "Hide from Apps list",
                subtitle = if (hidden) "Currently hidden" else "Pinned tiles remain on Start",
                trailing = {
                    MetroToggle(
                        checked = !hidden,
                        onCheckedChange = { visible -> vm.setAppHidden(packageName, !visible) },
                        contentDescription = "Show in Apps list"
                    )
                }
            )

            MetroSettingRow(
                title = "App info",
                subtitle = "Open Android app settings",
                onClick = { openAppInfo(context, packageName) }
            )

            if (app.canUninstall) {
                MetroSettingRow(
                    title = "Uninstall",
                    subtitle = "Removes the app from your device",
                    onClick = { vm.uninstallApp(context, packageName) }
                )
            }
        }
        }
    }
}

private fun openAppInfo(context: android.content.Context, packageName: String) {
    try {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    } catch (_: Exception) {
    }
}

@Composable
private fun IconBadge(app: AppInfo) {
    val isDark = LocalMetroForeground.current == Color.White
    Box(
        modifier = Modifier
            .size(MetroDimensions.appListIconBoxSize)
            .background(if (isDark) Color(0xFF181818) else Color(0xFFF0F0F0), RectangleShape),
        contentAlignment = Alignment.Center
    ) {
        if (app.resolvedIcon != null) {
            LauncherIconView(
                icon = app.resolvedIcon,
                contentDescription = app.label,
                tint = Color.White,
                modifier = Modifier.size(MetroDimensions.appListIconInnerSize)
            )
        } else if (app.iconBitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = app.iconBitmap,
                contentDescription = app.label,
                modifier = Modifier.size(MetroDimensions.appListIconInnerSize)
            )
        }
    }
}

@Composable
private fun AppInfoRow(
    app: AppInfo,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(app)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.label,
                style = MetroTypography.settingsLabel.copy(color = LocalMetroForeground.current),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MetroTypography.settingsSubtext.copy(color = LocalMetroSubtleText.current)
            )
        }
        Icon(
            imageVector = MetroIcons.Forward,
            contentDescription = null,
            tint = LocalMetroSubtleText.current,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun AppToggleRow(
    app: AppInfo,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable { onClick() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBadge(app)
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = app.label,
                style = MetroTypography.settingsLabel.copy(color = LocalMetroForeground.current),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        MetroToggle(
            checked = checked,
            onCheckedChange = onCheckedChange,
            contentDescription = "Hide ${app.label}"
        )
    }
}
