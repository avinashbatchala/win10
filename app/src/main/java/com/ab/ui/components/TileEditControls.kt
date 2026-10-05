package com.ab.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ab.ui.icons.MetroIcons

/**
 * Windows Phone 8.1 edit-mode controls: small white circles with a black border and glyph,
 * anchored just outside the selected tile's corners.
 */
@Composable
fun TileUnpinButton(
    onUnpin: () -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: Dp = 30.dp,
    iconSize: Dp = 16.dp
) {
    Box(
        modifier = modifier
            .testTag("tile_unpin_button")
            .size(buttonSize)
            .background(Color.White, CircleShape)
            .border(2.dp, Color.Black, CircleShape)
            .clickable(onClick = onUnpin),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = MetroIcons.Unpin,
            contentDescription = "Unpin tile",
            tint = Color.Black,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun TileResizeButton(
    onResize: () -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: Dp = 30.dp,
    iconSize: Dp = 16.dp
) {
    Box(
        modifier = modifier
            .testTag("tile_resize_button")
            .size(buttonSize)
            .background(Color.White, CircleShape)
            .border(2.dp, Color.Black, CircleShape)
            .clickable(onClick = onResize),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = MetroIcons.Resize,
            contentDescription = "Resize tile",
            tint = Color.Black,
            modifier = Modifier.size(iconSize)
        )
    }
}
