package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.LauncherSettings
import com.example.ui.theme.MetroColors
import com.example.ui.theme.MetroDimensions
import com.example.ui.theme.MetroTypography

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsSheet(
    isOpen: Boolean,
    settings: LauncherSettings,
    onAccentColorSelected: (Long) -> Unit,
    onToggleShowMoreTiles: () -> Unit,
    onRequestSetDefault: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isOpen) return

    BackHandler(enabled = isOpen) {
        onClose()
    }

    AnimatedVisibility(
        visible = isOpen,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
    ) {
        Box(
            modifier = modifier
                .testTag("settings_screen")
                .fillMaxSize()
                .background(MetroColors.BackgroundBlack)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                // Header with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "personalization",
                        style = MetroTypography.pageHeader
                    )
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("settings_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Default launcher section
                if (!settings.isDefaultLauncher) {
                    Box(
                        modifier = Modifier
                            .testTag("set_default_banner")
                            .fillMaxWidth()
                            .background(Color(settings.accentColor), RectangleShape)
                            .clickable { onRequestSetDefault() }
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(
                                text = "Set as default launcher",
                                style = MetroTypography.contextMenuTitle
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap here to make Win10 Start your primary Home app",
                                style = MetroTypography.tileLabel
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(28.dp))
                }

                // Grid layout options
                Text(
                    text = "START",
                    style = MetroTypography.contextMenuTitle,
                    color = Color(settings.accentColor)
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleShowMoreTiles() }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Show more tiles",
                            style = MetroTypography.appListItem
                        )
                        Text(
                            text = if (settings.showMoreTiles) "8-column layout (denser)" else "6-column layout (classic)",
                            style = MetroTypography.tileLabel,
                            color = MetroColors.TextDim
                        )
                    }

                    Switch(
                        checked = settings.showMoreTiles,
                        onCheckedChange = { onToggleShowMoreTiles() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(settings.accentColor),
                            uncheckedThumbColor = MetroColors.TextDim,
                            uncheckedTrackColor = Color(0xFF2B2B2B)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Accent colors section
                Text(
                    text = "ACCENT COLOR",
                    style = MetroTypography.contextMenuTitle,
                    color = Color(settings.accentColor)
                )
                Spacer(modifier = Modifier.height(14.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    maxItemsInEachRow = 5
                ) {
                    for (accent in MetroColors.WindowsAccents) {
                        val accentLong = accent.toArgb().toLong() and 0xFFFFFFFFL
                        val isSelected = (settings.accentColor and 0xFFFFFFFFL) == accentLong

                        Box(
                            modifier = Modifier
                                .testTag("accent_color_${accentLong}")
                                .size(52.dp)
                                .background(accent, RectangleShape)
                                .then(
                                    if (isSelected) {
                                        Modifier.border(2.5.dp, Color.White, RectangleShape)
                                    } else {
                                        Modifier
                                    }
                                )
                                .clickable {
                                    onAccentColorSelected(accentLong)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}
