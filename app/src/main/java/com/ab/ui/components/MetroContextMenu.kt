package com.ab.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.ab.ui.theme.LocalMetroForeground
import com.ab.ui.theme.LocalMetroSurface
import com.ab.ui.theme.MetroTypography

data class MetroContextMenuItem(
    val label: String,
    val enabled: Boolean = true,
    val onClick: () -> Unit
)

/**
 * Flat Windows context flyout used instead of the rounded Material [DropdownMenu]:
 * a square dark panel of full-width rows with hairline separators.
 */
@Composable
fun MetroContextMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    items: List<MetroContextMenuItem>,
    modifier: Modifier = Modifier
) {
    if (!expanded) return

    val fg = LocalMetroForeground.current
    val surface = LocalMetroSurface.current

    Popup(
        onDismissRequest = onDismiss,
        offset = IntOffset(0, 0),
        properties = PopupProperties(focusable = true)
    ) {
        Column(
            modifier = modifier
                .widthIn(min = 220.dp)
                .background(surface, RectangleShape)
                .border(1.dp, fg.copy(alpha = 0.20f), RectangleShape)
                .testTag("app_context_menu")
        ) {
            items.forEachIndexed { index, item ->
                Text(
                    text = item.label,
                    style = MetroTypography.contextMenuItem.copy(
                        color = if (item.enabled) fg else fg.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = item.enabled) {
                            onDismiss()
                            item.onClick()
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                )
                if (index != items.lastIndex) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(fg.copy(alpha = 0.15f))
                    )
                }
            }
        }
    }
}
