package com.ab.ui.components

import androidx.compose.foundation.Image
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import com.ab.model.ResolvedLauncherIcon

@Composable
fun LauncherIconView(
    icon: ResolvedLauncherIcon,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Color.White
) {
    when (icon) {
        is ResolvedLauncherIcon.VectorGlyph -> {
            Icon(
                imageVector = icon.imageVector,
                contentDescription = contentDescription,
                tint = tint,
                modifier = modifier
            )
        }
        is ResolvedLauncherIcon.MonochromeBitmap -> {
            Image(
                bitmap = icon.bitmap,
                contentDescription = contentDescription,
                colorFilter = ColorFilter.tint(tint),
                modifier = modifier
            )
        }
        is ResolvedLauncherIcon.OriginalBitmap -> {
            Image(
                bitmap = icon.bitmap,
                contentDescription = contentDescription,
                colorFilter = null,
                modifier = modifier
            )
        }
    }
}
