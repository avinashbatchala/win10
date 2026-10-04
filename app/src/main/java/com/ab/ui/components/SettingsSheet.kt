package com.ab.ui.components

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ab.model.LauncherSettings
import com.ab.ui.theme.LocalMetroAccentColor
import com.ab.ui.theme.LocalMetroBackground
import com.ab.ui.theme.LocalMetroDarkTheme
import com.ab.ui.theme.LocalMetroForeground
import com.ab.ui.theme.LocalMetroSubtleText
import com.ab.ui.theme.MetroColors
import com.ab.ui.theme.MetroDimensions
import com.ab.ui.theme.MetroTypography
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsSheet(
    isOpen: Boolean,
    settings: LauncherSettings,
    onAccentColorSelected: (Long) -> Unit,
    onToggleShowMoreTiles: () -> Unit,
    onTileTransparencyChanged: (Float) -> Unit,
    onThemeChanged: (Boolean) -> Unit,
    onSelectBackgroundUri: (String?) -> Unit,
    onRequestSetDefault: () -> Unit,
    isMediaAccessGranted: Boolean = false,
    onToggleShowMediaLiveTiles: (Boolean) -> Unit = {},
    onOpenMediaAccessSettings: () -> Unit = {},
    onPinNowPlayingTile: () -> Unit = {},
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isOpen) return

    val context = LocalContext.current
    val accentColor = LocalMetroAccentColor.current
    val isDark = LocalMetroDarkTheme.current
    val bgColor = LocalMetroBackground.current
    val fgColor = LocalMetroForeground.current
    val subtleColor = LocalMetroSubtleText.current

    // Modern Android Photo Picker for Start background image
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val flag = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, flag)
            } catch (_: Exception) {}
            onSelectBackgroundUri(uri.toString())
        }
    }

    AnimatedVisibility(
        visible = isOpen,
        enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .testTag("personalization_screen")
                .fillMaxSize()
                .background(bgColor)
                .statusBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = MetroDimensions.startHorizontalInset)
                    .padding(bottom = 60.dp)
            ) {
                // Top header with back button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("personalization_back_button")
                    ) {
                        Icon(
                            imageVector = com.ab.ui.icons.MetroIcons.Back,
                            contentDescription = "Back to Start",
                            tint = fgColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "START",
                        style = MetroTypography.settingsTitle.copy(color = fgColor)
                    )
                }

                Text(
                    text = "Personalization",
                    style = MetroTypography.settingsSubtext.copy(color = subtleColor),
                    modifier = Modifier.padding(start = 8.dp, bottom = 28.dp)
                )

                // 1. Background image section
                Text(
                    text = "BACKGROUND",
                    style = MetroTypography.settingsSection.copy(color = accentColor),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = if (settings.backgroundImageUri != null) "Full Start wallpaper active" else "No background image selected",
                    style = MetroTypography.settingsLabel.copy(color = fgColor),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Windows 10 Mobile flat Choose Photo button
                    Box(
                        modifier = Modifier
                            .testTag("choose_photo_button")
                            .border(2.dp, if (isDark) Color.White else Color.Black, RectangleShape)
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Choose photo",
                            style = MetroTypography.buttonLabel.copy(color = fgColor)
                        )
                    }

                    if (settings.backgroundImageUri != null) {
                        // Windows 10 Mobile flat Remove Photo button
                        Box(
                            modifier = Modifier
                                .testTag("remove_photo_button")
                                .border(2.dp, if (isDark) Color(0xFF888888) else Color(0xFFCCCCCC), RectangleShape)
                                .clickable {
                                    onSelectBackgroundUri(null)
                                }
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Remove",
                                style = MetroTypography.buttonLabel.copy(color = subtleColor)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // 2. Tile transparency section
                val percentage = (settings.tileTransparency * 100f).roundToInt()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TILE TRANSPARENCY",
                        style = MetroTypography.settingsSection.copy(color = accentColor)
                    )
                    Text(
                        text = "$percentage%",
                        style = MetroTypography.settingsLabel.copy(color = fgColor)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Custom Windows 10 Mobile flat rectangular slider
                WindowsSlider(
                    value = settings.tileTransparency,
                    onValueChange = onTileTransparencyChanged,
                    accentColor = accentColor,
                    isDark = isDark,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "0% is fully opaque; 100% is transparent",
                    style = MetroTypography.settingsSubtext.copy(color = subtleColor),
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // 3. Accent color section
                Text(
                    text = "ACCENT COLOR",
                    style = MetroTypography.settingsSection.copy(color = accentColor),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetroColors.WindowsAccents.forEach { accent ->
                        val colorLong = accent.color.toArgb().toLong() and 0xFFFFFFFFL
                        val isSelected = settings.accentColor == colorLong

                        Box(
                            modifier = Modifier
                                .testTag("accent_color_${accent.name.lowercase()}")
                                .size(46.dp)
                                .background(accent.color, RectangleShape)
                                .clickable { onAccentColorSelected(colorLong) }
                                .then(
                                    if (isSelected) {
                                        Modifier.border(3.dp, if (isDark) Color.White else Color.Black, RectangleShape)
                                    } else {
                                        Modifier
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = com.ab.ui.icons.MetroIcons.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // 4. Dark / Light theme section
                Text(
                    text = "THEME",
                    style = MetroTypography.settingsSection.copy(color = accentColor),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WindowsThemeRadio(
                        label = "Dark",
                        selected = settings.darkTheme,
                        onClick = { onThemeChanged(true) },
                        accentColor = accentColor,
                        isDark = isDark,
                        fgColor = fgColor
                    )

                    WindowsThemeRadio(
                        label = "Light",
                        selected = !settings.darkTheme,
                        onClick = { onThemeChanged(false) },
                        accentColor = accentColor,
                        isDark = isDark,
                        fgColor = fgColor
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // 5. Start tile density section ('Show more tiles')
                Text(
                    text = "START DENSITY",
                    style = MetroTypography.settingsSection.copy(color = accentColor),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleShowMoreTiles() },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Show more tiles",
                            style = MetroTypography.settingsLabel.copy(color = fgColor)
                        )
                        Text(
                            text = if (settings.showMoreTiles) "Dense 8-column layout" else "Standard 6-column layout",
                            style = MetroTypography.settingsSubtext.copy(color = subtleColor)
                        )
                    }

                    // Windows 10 Mobile flat toggle
                    WindowsToggle(
                        checked = settings.showMoreTiles,
                        onCheckedChange = { onToggleShowMoreTiles() },
                        accentColor = accentColor,
                        isDark = isDark
                    )
                }

                Spacer(modifier = Modifier.height(36.dp))

                // 6. Default Launcher Integration
                Text(
                    text = "SYSTEM INTEGRATION",
                    style = MetroTypography.settingsSection.copy(color = accentColor),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = if (settings.isDefaultLauncher) "Set as default Home launcher" else "Not currently the default launcher",
                    style = MetroTypography.settingsLabel.copy(color = fgColor),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (!settings.isDefaultLauncher) {
                    Box(
                        modifier = Modifier
                            .testTag("set_default_launcher_button")
                            .border(2.dp, if (isDark) Color.White else Color.Black, RectangleShape)
                            .clickable { onRequestSetDefault() }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Set as default launcher",
                            style = MetroTypography.buttonLabel.copy(color = fgColor)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(36.dp))

                // 7. Media Live Tiles
                Text(
                    text = "MEDIA",
                    style = MetroTypography.settingsSection.copy(color = accentColor),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleShowMediaLiveTiles(!settings.showMediaLiveTiles) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Show media on live tiles",
                            style = MetroTypography.settingsLabel.copy(color = fgColor)
                        )
                        Text(
                            text = if (settings.showMediaLiveTiles) "Display playback from compatible media apps" else "Disabled",
                            style = MetroTypography.settingsSubtext.copy(color = subtleColor)
                        )
                    }

                    WindowsToggle(
                        checked = settings.showMediaLiveTiles,
                        onCheckedChange = { onToggleShowMediaLiveTiles(it) },
                        accentColor = accentColor,
                        isDark = isDark
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Media access: " + if (isMediaAccessGranted) "Allowed" else "Not allowed",
                    style = MetroTypography.settingsLabel.copy(color = fgColor),
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Text(
                    text = "Android requires Notification Access to discover active media playback sessions. The launcher does not access or store your messages or notifications.",
                    style = MetroTypography.settingsSubtext.copy(color = subtleColor),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (!isMediaAccessGranted) {
                    Box(
                        modifier = Modifier
                            .testTag("allow_media_access_button")
                            .border(2.dp, if (isDark) Color.White else Color.Black, RectangleShape)
                            .clickable { onOpenMediaAccessSettings() }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "ALLOW MEDIA ACCESS",
                            style = MetroTypography.buttonLabel.copy(color = fgColor)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Box(
                    modifier = Modifier
                        .testTag("pin_now_playing_tile_button")
                        .border(1.5.dp, accentColor, RectangleShape)
                        .clickable { onPinNowPlayingTile() }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Pin 'Now Playing' Tile to Start",
                        style = MetroTypography.buttonLabel.copy(color = fgColor)
                    )
                }
            }
        }
    }
}

/**
 * Windows 10 Mobile flat rectangular slider.
 */
@Composable
private fun WindowsSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    accentColor: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(value.coerceIn(0f, 1f)) }

    val currentFraction = if (isDragging) dragFraction else value.coerceIn(0f, 1f)

    BoxWithConstraints(
        modifier = modifier
            .testTag("tile_transparency_slider")
            .height(36.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val newFraction = (offset.x / size.width).coerceIn(0f, 1f)
                    dragFraction = newFraction
                    onValueChange(newFraction)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        val newFraction = (offset.x / size.width).coerceIn(0f, 1f)
                        dragFraction = newFraction
                        onValueChange(newFraction)
                    },
                    onDragEnd = {
                        isDragging = false
                    },
                    onDragCancel = {
                        isDragging = false
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val newFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                        dragFraction = newFraction
                        onValueChange(newFraction)
                    }
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        val totalWidthPx = constraints.maxWidth.toFloat()
        val thumbWidthDp = 10.dp
        val thumbWidthPx = with(density) { thumbWidthDp.toPx() }
        val trackHeightDp = 4.dp
        val unfilledTrackColor = if (isDark) Color(0xFF444444) else Color(0xFFCCCCCC)

        // Unfilled track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(trackHeightDp)
                .background(unfilledTrackColor, RectangleShape)
        )

        // Filled track
        Box(
            modifier = Modifier
                .fillMaxWidth(currentFraction)
                .height(trackHeightDp)
                .background(accentColor, RectangleShape)
        )

        // Rectangular thumb indicator moving along the track
        val maxThumbTravel = (totalWidthPx - thumbWidthPx).coerceAtLeast(0f)
        val thumbOffsetPx = currentFraction * maxThumbTravel
        Box(
            modifier = Modifier
                .offset { IntOffset(thumbOffsetPx.roundToInt(), 0) }
                .size(width = thumbWidthDp, height = 24.dp)
                .border(1.dp, if (isDark) Color.White else Color.Black, RectangleShape)
                .background(accentColor, RectangleShape)
        )
    }
}

/**
 * Windows 10 Mobile flat toggle switch.
 */
@Composable
private fun WindowsToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    accentColor: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .testTag("windows_toggle_density")
            .clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 44.dp, height = 22.dp)
                .border(2.dp, if (checked) accentColor else if (isDark) Color(0xFF888888) else Color(0xFF666666), RectangleShape)
                .background(if (checked) accentColor else Color.Transparent, RectangleShape)
                .padding(2.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .align(if (checked) Alignment.CenterEnd else Alignment.CenterStart)
                    .background(if (checked) Color.White else if (isDark) Color.White else Color.Black, RectangleShape)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = if (checked) "On" else "Off",
            style = MetroTypography.settingsLabel.copy(
                color = if (isDark) Color.White else Color.Black,
                fontSize = 15.sp
            )
        )
    }
}

/**
 * Windows 10 Mobile flat radio selector.
 */
@Composable
private fun WindowsThemeRadio(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    accentColor: Color,
    isDark: Boolean,
    fgColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .testTag("theme_radio_${label.lowercase()}")
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .border(2.dp, if (selected) accentColor else if (isDark) Color(0xFF888888) else Color(0xFF666666), RectangleShape),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(accentColor, RectangleShape)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = label,
            style = MetroTypography.settingsLabel.copy(color = fgColor)
        )
    }
}
