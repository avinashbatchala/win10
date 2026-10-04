package com.ab.ui.components

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ab.model.AppInfo
import com.ab.ui.theme.MetroDimensions
import com.ab.ui.theme.MetroTypography

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppListRow(
    app: AppInfo,
    isPinned: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    onPinToStart: () -> Unit,
    onUnpinFromStart: () -> Unit,
    onUninstall: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = com.ab.ui.theme.LocalMetroDarkTheme.current
    val fgColor = com.ab.ui.theme.LocalMetroForeground.current
    val surfaceColor = com.ab.ui.theme.LocalMetroSurface.current
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .testTag("app_row_${app.packageName}")
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(MetroDimensions.appListRowHeight)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showMenu = true }
                )
                .padding(horizontal = MetroDimensions.appListHorizontalInset),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App icon container
            val accentColor = com.ab.ui.theme.LocalMetroAccentColor.current
            val isMetroOrMono = app.resolvedIcon is com.ab.model.ResolvedLauncherIcon.VectorGlyph ||
                    app.resolvedIcon is com.ab.model.ResolvedLauncherIcon.MonochromeBitmap

            Box(
                modifier = Modifier
                    .size(MetroDimensions.appListIconBoxSize)
                    .background(
                        if (isMetroOrMono) accentColor else (if (isDark) Color(0xFF181818) else Color(0xFFF0F0F0)),
                        RectangleShape
                    ),
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
                    Image(
                        bitmap = app.iconBitmap,
                        contentDescription = app.label,
                        modifier = Modifier.size(MetroDimensions.appListIconInnerSize)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // App label
            Text(
                text = app.label,
                style = MetroTypography.appListItem.copy(color = fgColor),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        // Windows 10 Mobile style context popup
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier
                .background(surfaceColor, RectangleShape)
                .testTag("app_context_menu")
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = if (isPinned) "Unpin from Start" else "Pin to Start",
                        style = MetroTypography.contextMenuItem.copy(color = fgColor)
                    )
                },
                onClick = {
                    showMenu = false
                    if (isPinned) onUnpinFromStart() else onPinToStart()
                },
                colors = MenuDefaults.itemColors(
                    textColor = fgColor
                )
            )

            DropdownMenuItem(
                text = {
                    Text(
                        text = "App settings",
                        style = MetroTypography.contextMenuItem.copy(color = fgColor)
                    )
                },
                onClick = {
                    showMenu = false
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${app.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try {
                        context.startActivity(intent)
                    } catch (_: Exception) {}
                },
                colors = MenuDefaults.itemColors(
                    textColor = fgColor
                )
            )

            DropdownMenuItem(
                text = {
                    Text(
                        text = "Uninstall",
                        style = MetroTypography.contextMenuItem.copy(
                            color = if (app.canUninstall) fgColor else (if (isDark) Color(0xFF666666) else Color(0xFFAAAAAA))
                        )
                    )
                },
                enabled = app.canUninstall,
                onClick = {
                    showMenu = false
                    if (app.canUninstall) {
                        onUninstall()
                    }
                },
                colors = MenuDefaults.itemColors(
                    textColor = fgColor,
                    disabledTextColor = if (isDark) Color(0xFF666666) else Color(0xFFAAAAAA)
                )
            )
        }
    }
}
