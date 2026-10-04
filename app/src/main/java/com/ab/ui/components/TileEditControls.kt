package com.ab.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.ab.ui.icons.MetroIcons
import com.ab.ui.theme.MetroColors
import com.ab.ui.theme.MetroDimensions

@Composable
fun TileUnpinButton(
    onUnpin: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .testTag("tile_unpin_button")
            .size(MetroDimensions.editButtonSize)
            .background(MetroColors.EditControlBackground, RectangleShape)
            .border(1.5.dp, MetroColors.EditControlBorder, RectangleShape)
            .clickable(onClick = onUnpin),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = MetroIcons.Unpin,
            contentDescription = "Unpin tile",
            tint = Color.White,
            modifier = Modifier.size(MetroDimensions.editIconSize)
        )
    }
}

@Composable
fun TileResizeButton(
    onResize: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .testTag("tile_resize_button")
            .size(MetroDimensions.editButtonSize)
            .background(MetroColors.EditControlBackground, RectangleShape)
            .border(1.5.dp, MetroColors.EditControlBorder, RectangleShape)
            .clickable(onClick = onResize),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = MetroIcons.Resize,
            contentDescription = "Resize tile",
            tint = Color.White,
            modifier = Modifier.size(MetroDimensions.editIconSize)
        )
    }
}
