package com.ab.ui.settings

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ab.ui.icons.MetroIcons
import com.ab.ui.settings.components.MetroSearchBox
import com.ab.ui.settings.components.SettingsPageInset
import com.ab.ui.theme.LocalMetroAccentColor
import com.ab.ui.theme.LocalMetroBackground
import com.ab.ui.theme.LocalMetroForeground
import com.ab.ui.theme.LocalMetroSubtleText
import com.ab.ui.theme.MetroTypography

@Composable
fun SettingsRootScreen(
    onOpenPivot: (SettingsPivot) -> Unit,
    onOpenDestination: (SettingsDestination) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = LocalMetroAccentColor.current
    val bg = LocalMetroBackground.current
    val fg = LocalMetroForeground.current
    val subtle = LocalMetroSubtleText.current

    var query by rememberSaveable { mutableStateOf("") }
    val results = remember(query) { SettingsSearchIndex.search(query) }
    val isSearching = query.isNotBlank()

    // Back priority: clear the active search before leaving the root Settings page.
    BackHandler {
        if (query.isNotEmpty()) query = "" else onBack()
    }

    Column(
        modifier = modifier
            .testTag("settings_root")
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
                    .testTag("settings_root_back")
                    .size(48.dp)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = MetroIcons.Back,
                    contentDescription = "Back to Start",
                    tint = fg,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Text(
            text = "Settings",
            style = MetroTypography.settingsTitle.copy(color = fg, fontSize = 40.sp),
            modifier = Modifier.padding(horizontal = SettingsPageInset, vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        MetroSearchBox(
            value = query,
            onValueChange = { query = it },
            placeholder = "Find a setting",
            modifier = Modifier
                .testTag("settings_search_box")
                .padding(horizontal = SettingsPageInset)
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (isSearching) {
            SearchResults(
                results = results,
                accent = accent,
                subtle = subtle,
                fg = fg,
                onOpenDestination = onOpenDestination,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(4.dp))
                SettingsCategories.ALL.forEach { category ->
                    CategoryRow(
                        title = category.title,
                        subtitle = category.subtitle,
                        icon = category.icon,
                        accent = accent,
                        fg = fg,
                        subtle = subtle,
                        onClick = { onOpenPivot(category.pivot) }
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun CategoryRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    fg: Color,
    subtle: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .testTag("settings_category_${title.lowercase().replace(" ", "_")}")
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = SettingsPageInset, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(26.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MetroTypography.settingsLabel.copy(color = fg))
            Text(
                text = subtitle,
                style = MetroTypography.settingsSubtext.copy(color = subtle),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Icon(
            imageVector = MetroIcons.Forward,
            contentDescription = null,
            tint = subtle,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun SearchResults(
    results: List<SettingsSearchEntry>,
    accent: Color,
    subtle: Color,
    fg: Color,
    onOpenDestination: (SettingsDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = SettingsPageInset)
    ) {
        if (results.isEmpty()) {
            Text(
                text = "No results",
                style = MetroTypography.settingsSubtext.copy(color = subtle),
                modifier = Modifier.padding(vertical = 24.dp)
            )
        } else {
            results.forEach { entry ->
                Column(
                    modifier = Modifier
                        .testTag("settings_search_result_${entry.settingId}")
                        .fillMaxWidth()
                        .clickable { onOpenDestination(entry.destination) }
                        .padding(vertical = 12.dp)
                ) {
                    Text(text = entry.title, style = MetroTypography.settingsLabel.copy(color = fg))
                    Text(
                        text = entry.breadcrumb,
                        style = MetroTypography.settingsSubtext.copy(color = accent),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
