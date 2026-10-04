package com.ab.ui.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.ab.ui.icons.MetroIcons
import com.ab.ui.theme.LocalMetroAccentColor
import com.ab.ui.theme.LocalMetroBackground
import com.ab.ui.theme.LocalMetroDarkTheme
import com.ab.ui.theme.LocalMetroForeground
import com.ab.ui.theme.LocalMetroSubtleText
import com.ab.ui.theme.MetroColors
import com.ab.ui.theme.MetroDimensions
import com.ab.ui.theme.MetroTypography
import kotlin.math.roundToInt

private val PageInset = 18.dp

/** Small uppercase section heading used inside a pivot page. */
@Composable
fun MetroSectionHeader(text: String, modifier: Modifier = Modifier) {
    val accent = LocalMetroAccentColor.current
    Text(
        text = text.uppercase(),
        style = MetroTypography.settingsSection.copy(color = accent),
        modifier = modifier.padding(top = 22.dp, bottom = 6.dp)
    )
}

/**
 * A Windows 10 Mobile settings row: left-aligned title + muted subtitle, optional trailing control.
 */
@Composable
fun MetroSettingRow(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val fg = LocalMetroForeground.current
    val subtle = LocalMetroSubtleText.current
    val rowModifier = modifier
        .fillMaxWidth()
        .then(
            if (onClick != null && enabled) Modifier.clickable { onClick() } else Modifier
        )
        .padding(vertical = 10.dp)

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MetroTypography.settingsLabel.copy(
                    color = if (enabled) fg else subtle
                )
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MetroTypography.settingsSubtext.copy(color = subtle),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(12.dp))
            trailing()
        }
    }
}

/** A tappable settings row that navigates to another page. */
@Composable
fun MetroSettingLink(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val fg = LocalMetroForeground.current
    val subtle = LocalMetroSubtleText.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MetroTypography.settingsLabel.copy(color = fg))
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MetroTypography.settingsSubtext.copy(color = subtle),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        Icon(
            imageVector = MetroIcons.Forward,
            contentDescription = null,
            tint = subtle,
            modifier = Modifier.size(18.dp)
        )
    }
}

/** Windows 10 Mobile compact rectangular toggle. */
@Composable
fun MetroToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null
) {
    val accent = LocalMetroAccentColor.current
    val isDark = LocalMetroDarkTheme.current
    val trackBorder = when {
        !enabled -> if (isDark) Color(0xFF555555) else Color(0xFFBBBBBB)
        checked -> accent
        else -> if (isDark) Color(0xFF888888) else Color(0xFF666666)
    }

    Row(
        modifier = modifier
            .toggleable(
                value = checked,
                enabled = enabled,
                onValueChange = onCheckedChange
            )
            .semantics { if (contentDescription != null) this.contentDescription = contentDescription }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 44.dp, height = 22.dp)
                .border(2.dp, trackBorder, RectangleShape)
                .background(
                    if (checked) accent.copy(alpha = if (enabled) 1f else 0.4f) else Color.Transparent,
                    RectangleShape
                )
                .padding(2.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .align(if (checked) Alignment.CenterEnd else Alignment.CenterStart)
                    .background(if (isDark || checked) Color.White else Color.Black, RectangleShape)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = if (checked) "On" else "Off",
            style = MetroTypography.settingsLabel.copy(
                color = if (enabled) LocalMetroForeground.current else LocalMetroSubtleText.current,
                fontSize = 15.sp
            )
        )
    }
}

/** Windows-style circular radio group rendered vertically. */
@Composable
fun MetroRadioGroup(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val accent = LocalMetroAccentColor.current
    val isDark = LocalMetroDarkTheme.current
    val fg = LocalMetroForeground.current
    Column(modifier = modifier.fillMaxWidth()) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = selected,
                        enabled = enabled,
                        onClick = { onSelect(index) }
                    )
                    .padding(vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .border(
                            2.dp,
                            if (selected) accent else if (isDark) Color(0xFF888888) else Color(0xFF666666),
                            RectangleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(accent, RectangleShape)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = label,
                    style = MetroTypography.settingsLabel.copy(color = fg)
                )
            }
        }
    }
}

/** Windows 10 Mobile thin-line slider with a rectangular thumb. */
@Composable
fun MetroSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null
) {
    val density = LocalDensity.current
    val accent = LocalMetroAccentColor.current
    val isDark = LocalMetroDarkTheme.current
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(value.coerceIn(0f, 1f)) }
    val currentFraction = if (isDragging) dragFraction else value.coerceIn(0f, 1f)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .semantics {
                if (contentDescription != null) this.contentDescription = contentDescription
                progressBarRangeInfo = ProgressBarRangeInfo(currentFraction, 0f..1f)
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures { offset ->
                    val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                    dragFraction = fraction
                    onValueChange(fraction)
                }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                        dragFraction = fraction
                        onValueChange(fraction)
                    },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false },
                    onDrag = { change, _ ->
                        change.consume()
                        val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                        dragFraction = fraction
                        onValueChange(fraction)
                    }
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        val totalWidthPx = constraints.maxWidth.toFloat()
        val thumbWidthDp = 10.dp
        val thumbWidthPx = with(density) { thumbWidthDp.toPx() }
        val trackColor = if (isDark) Color(0xFF444444) else Color(0xFFCCCCCC)
        val activeColor = if (enabled) accent else trackColor

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(trackColor, RectangleShape)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(currentFraction)
                .height(4.dp)
                .background(activeColor, RectangleShape)
        )
        val maxTravel = (totalWidthPx - thumbWidthPx).coerceAtLeast(0f)
        Box(
            modifier = Modifier
                .offset { IntOffset((currentFraction * maxTravel).roundToInt(), 0) }
                .size(width = thumbWidthDp, height = 24.dp)
                .border(1.dp, if (isDark) Color.White else Color.Black, RectangleShape)
                .background(if (enabled) accent else trackColor, RectangleShape)
        )
    }
}

/** Windows-style rectangular dropdown field with a squared-off selection surface. */
@Composable
fun MetroDropdown(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null
) {
    val fg = LocalMetroForeground.current
    val subtle = LocalMetroSubtleText.current
    val accent = LocalMetroAccentColor.current
    val isDark = LocalMetroDarkTheme.current
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.getOrNull(selectedIndex) ?: options.firstOrNull().orEmpty()

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, if (isDark) Color(0xFF888888) else Color(0xFF666666), RectangleShape)
                .clickable(enabled = enabled) { expanded = true }
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .semantics { if (contentDescription != null) this.contentDescription = contentDescription },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selectedLabel,
                style = MetroTypography.settingsLabel.copy(
                    color = if (enabled) fg else subtle
                ),
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "\u25BC",
                style = MetroTypography.settingsSubtext.copy(color = subtle)
            )
        }

        if (expanded) {
            Popup(
                alignment = Alignment.TopStart,
                offset = androidx.compose.ui.unit.IntOffset(0, 0),
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = true)
            ) {
                Column(
                    modifier = Modifier
                        .width(240.dp)
                        .background(LocalMetroBackground.current, RectangleShape)
                        .border(2.dp, if (isDark) Color(0xFF888888) else Color(0xFF666666), RectangleShape)
                ) {
                    options.forEachIndexed { index, option ->
                        val selected = index == selectedIndex
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (selected) accent.copy(alpha = 0.25f) else Color.Transparent)
                                .clickable {
                                    expanded = false
                                    onSelect(index)
                                }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = option,
                                style = MetroTypography.settingsLabel.copy(color = fg),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Windows 10 Mobile square-cornered search field. Search icon is aligned to the right. */
@Composable
fun MetroSearchBox(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val isDark = LocalMetroDarkTheme.current
    val fg = LocalMetroForeground.current
    val subtle = LocalMetroSubtleText.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(MetroDimensions.searchBoxHeight)
            .background(if (isDark) Color.Black else Color.White, RectangleShape)
            .border(
                MetroDimensions.searchBoxBorderWidth,
                if (isDark) MetroColors.SearchBorderDark else MetroColors.SearchBorderLight,
                RectangleShape
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    style = MetroTypography.searchHint.copy(color = subtle)
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MetroTypography.searchInput.copy(color = fg),
                cursorBrush = SolidColor(fg),
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = MetroIcons.Search,
            contentDescription = "Search",
            tint = subtle,
            modifier = Modifier.size(20.dp)
        )
    }
}

/** Square accent-colour swatches with a simple Windows-style outline for the active colour. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MetroAccentGrid(
    selectedColor: Long,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalMetroDarkTheme.current
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MetroColors.WindowsAccents.forEach { accent ->
            val colorLong = accent.color.toArgb().toLong() and 0xFFFFFFFFL
            val isSelected = selectedColor == colorLong
            Box(
                modifier = Modifier
                    .testTag("accent_color_${accent.name.lowercase()}")
                    .size(46.dp)
                    .background(accent.color, RectangleShape)
                    .clickable { onSelect(colorLong) }
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
                        imageVector = MetroIcons.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/** A compact label/value information row. */
@Composable
fun MetroInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val fg = LocalMetroForeground.current
    val subtle = LocalMetroSubtleText.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MetroTypography.settingsLabel.copy(color = subtle),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MetroTypography.settingsLabel.copy(color = fg)
        )
    }
}

/** Flat Windows-style rectangular button. */
@Composable
fun MetroButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    outlined: Boolean = true
) {
    val fg = LocalMetroForeground.current
    val accent = LocalMetroAccentColor.current
    val isDark = LocalMetroDarkTheme.current
    val borderColor = when {
        !outlined -> Color.Transparent
        enabled -> accent
        else -> if (isDark) Color(0xFF555555) else Color(0xFFBBBBBB)
    }
    Box(
        modifier = modifier
            .border(if (outlined) 2.dp else 0.dp, borderColor, RectangleShape)
            .background(if (!outlined && enabled) accent else Color.Transparent, RectangleShape)
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MetroTypography.buttonLabel.copy(
                color = if (enabled) fg else LocalMetroSubtleText.current
            )
        )
    }
}

/** Windows-style square confirmation dialog. Never resets or changes state silently. */
@Composable
fun MetroSettingsDialog(
    title: String,
    message: String,
    confirmText: String,
    dismissText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val bg = LocalMetroBackground.current
    val fg = LocalMetroForeground.current
    val subtle = LocalMetroSubtleText.current
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(bg, RectangleShape)
                .border(1.dp, subtle, RectangleShape)
                .padding(20.dp)
        ) {
            Text(
                text = title,
                style = MetroTypography.pageHeader.copy(fontSize = 22.sp, color = fg),
                modifier = Modifier.padding(bottom = 10.dp)
            )
            Text(
                text = message,
                style = MetroTypography.settingsSubtext.copy(color = subtle),
                modifier = Modifier.padding(bottom = 22.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)
            ) {
                MetroButton(text = dismissText, onClick = onDismiss, outlined = true)
                MetroButton(text = confirmText, onClick = onConfirm, outlined = false)
            }
        }
    }
}

/** Standard left/right page inset used across all Settings screens. */
val SettingsPageInset = PageInset
