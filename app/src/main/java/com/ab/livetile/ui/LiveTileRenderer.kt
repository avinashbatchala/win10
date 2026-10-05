package com.ab.livetile.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ab.livetile.model.LiveTileFace
import com.ab.livetile.model.LiveTileState
import com.ab.livetile.model.LiveTileTemplate
import com.ab.model.TileSize
import com.ab.ui.theme.MetroDimensions
import com.ab.ui.theme.MetroMotion
import com.ab.ui.theme.MetroTypography

/**
 * Windows 10 Mobile Live Tile face renderer.
 * Handles templates (ICONIC, COUNT, PRIMARY_TEXT, TEXT_LINES, DATE, IMAGE, IMAGE_AND_TEXT)
 * with size awareness (Small, Medium, Wide, Large) and authentic vertical 3D flip animation.
 */
@Composable
fun LiveTileRenderer(
    liveState: LiveTileState,
    tileSize: TileSize,
    defaultLabel: String,
    dispatcher: com.ab.media.MediaActionDispatcher? = null,
    mediaShowControls: Boolean = true,
    mediaShowProgress: Boolean = true,
    modifier: Modifier = Modifier
) {
    val activeFace = liveState.activeFace ?: return
    val targetIndex = liveState.activeFaceIndex

    // Windows Phone 3D vertical flip transition state
    var displayedFace by remember { mutableStateOf(activeFace) }
    var displayedIndex by remember { mutableIntStateOf(targetIndex) }
    val rotationX = remember { Animatable(0f) }

    // Key on the face content as well as the index: single-face tiles (media, clock,
    // battery) update their content while keeping index 0, and would otherwise stay stale.
    LaunchedEffect(targetIndex, activeFace) {
        if (targetIndex != displayedIndex && liveState.faces.size > 1) {
            // First half of flip: rotate out from 0 to 90 degrees
            rotationX.animateTo(
                targetValue = 90f,
                animationSpec = tween(
                    durationMillis = 220,
                    easing = MetroMotion.MetroEaseIn
                )
            )
            displayedFace = liveState.faces[targetIndex % liveState.faces.size]
            displayedIndex = targetIndex
            rotationX.snapTo(-90f)
            // Second half of flip: rotate in from -90 to 0 degrees
            rotationX.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = 260,
                    easing = MetroMotion.MetroEaseOut
                )
            )
        } else {
            displayedFace = activeFace
            displayedIndex = targetIndex
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                this.rotationX = rotationX.value
                cameraDistance = 14f * density
            }
            .semantics {
                contentDescription = displayedFace.accessibilityDescription
            }
    ) {
        RenderTemplate(
            face = displayedFace,
            tileSize = tileSize,
            defaultLabel = defaultLabel,
            dispatcher = dispatcher,
            mediaShowControls = mediaShowControls,
            mediaShowProgress = mediaShowProgress
        )
    }
}

@Composable
private fun RenderTemplate(
    face: LiveTileFace,
    tileSize: TileSize,
    defaultLabel: String,
    dispatcher: com.ab.media.MediaActionDispatcher?,
    mediaShowControls: Boolean,
    mediaShowProgress: Boolean
) {
    val displayLabel = face.labelOverride ?: defaultLabel

    when (face.template) {
        LiveTileTemplate.ICONIC -> IconicTemplate(face, tileSize, displayLabel)
        LiveTileTemplate.COUNT -> CountTemplate(face, tileSize, displayLabel)
        LiveTileTemplate.PRIMARY_TEXT -> PrimaryTextTemplate(face, tileSize, displayLabel)
        LiveTileTemplate.TEXT_LINES -> TextLinesTemplate(face, tileSize, displayLabel)
        LiveTileTemplate.DATE -> DateTemplate(face, tileSize, displayLabel)
        LiveTileTemplate.IMAGE -> ImageTemplate(face, tileSize, displayLabel)
        LiveTileTemplate.IMAGE_AND_TEXT -> ImageAndTextTemplate(face, tileSize, displayLabel)
        LiveTileTemplate.MEDIA -> MediaTileContent(
            face,
            tileSize,
            displayLabel,
            dispatcher,
            showControls = mediaShowControls,
            showProgress = mediaShowProgress
        )
        LiveTileTemplate.WEATHER -> WeatherTileContent(face, tileSize, displayLabel)
    }
}

@Composable
private fun IconicTemplate(face: LiveTileFace, size: TileSize, label: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(if (size == TileSize.SMALL) MetroDimensions.tileContentPaddingSmall else MetroDimensions.tileContentPadding)
    ) {
        // Icon
        if (face.iconVector != null) {
            Icon(
                imageVector = face.iconVector,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(if (size == TileSize.SMALL) 26.dp else 40.dp)
                    .align(if (size == TileSize.SMALL) Alignment.Center else Alignment.TopStart)
            )
        }

        // Badge count in corner if present
        if (face.badgeCount != null && face.badgeCount > 0 && size != TileSize.SMALL) {
            Text(
                text = "${face.badgeCount}",
                style = MetroTypography.tileCountNumber.copy(fontSize = 24.sp),
                color = Color.White,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }

        // Label
        if (size != TileSize.SMALL) {
            Text(
                text = label,
                style = MetroTypography.tileLabel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.BottomStart)
            )
        }
    }
}

@Composable
private fun CountTemplate(face: LiveTileFace, size: TileSize, label: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(if (size == TileSize.SMALL) MetroDimensions.tileContentPaddingSmall else MetroDimensions.tileContentPadding)
    ) {
        Column(
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Text(
                text = face.primaryText ?: "",
                style = MetroTypography.tileCountNumber.copy(
                    fontSize = if (size == TileSize.SMALL) 15.sp else 46.sp,
                    fontWeight = FontWeight.Light
                ),
                color = Color.White,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            if (face.secondaryText != null && size != TileSize.SMALL) {
                Text(
                    text = face.secondaryText,
                    style = MetroTypography.tileSubtext,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1
                )
            }
        }

        if (size != TileSize.SMALL) {
            Text(
                text = label,
                style = MetroTypography.tileLabel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.BottomStart)
            )
        }
    }
}

@Composable
private fun PrimaryTextTemplate(face: LiveTileFace, size: TileSize, label: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(if (size == TileSize.SMALL) MetroDimensions.tileContentPaddingSmall else MetroDimensions.tileContentPadding)
    ) {
        when (size) {
            TileSize.SMALL -> {
                Text(
                    text = face.primaryText ?: "",
                    style = MetroTypography.tileLabel.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = Color.White,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            TileSize.MEDIUM -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopStart)
                ) {
                    Text(
                        text = face.primaryText ?: "",
                        style = MetroTypography.tileLargeHeader.copy(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Light
                        ),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (face.secondaryText != null) {
                        Text(
                            text = face.secondaryText,
                            style = MetroTypography.tileSubtext,
                            color = Color.White.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Text(
                    text = label,
                    style = MetroTypography.tileLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.align(Alignment.BottomStart)
                )
            }
            TileSize.WIDE, TileSize.LARGE -> {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = face.primaryText ?: "",
                            style = MetroTypography.tileLargeHeader.copy(
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Light
                            ),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (face.secondaryText != null) {
                            Text(
                                text = face.secondaryText,
                                style = MetroTypography.tileSubtext.copy(fontSize = 14.sp),
                                color = Color.White.copy(alpha = 0.9f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (face.tertiaryText != null) {
                            Text(
                                text = face.tertiaryText,
                                style = MetroTypography.tileSubtext.copy(fontSize = 12.sp),
                                color = Color.White.copy(alpha = 0.75f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (face.iconVector != null) {
                        Icon(
                            imageVector = face.iconVector,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier
                                .size(46.dp)
                                .padding(end = 8.dp)
                        )
                    }
                }
                Text(
                    text = label,
                    style = MetroTypography.tileLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.align(Alignment.BottomStart)
                )
            }
        }
    }
}

@Composable
private fun TextLinesTemplate(face: LiveTileFace, size: TileSize, label: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(if (size == TileSize.SMALL) MetroDimensions.tileContentPaddingSmall else MetroDimensions.tileContentPadding)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = if (size != TileSize.SMALL) 22.dp else 0.dp)
                .align(Alignment.TopStart)
        ) {
            if (face.primaryText != null) {
                Text(
                    text = face.primaryText,
                    style = MetroTypography.tileLabel.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = if (size == TileSize.SMALL) 13.sp else 16.sp
                    ),
                    color = Color.White,
                    maxLines = 1
                )
            }
            face.textLines.take(if (size == TileSize.WIDE || size == TileSize.LARGE) 3 else 1).forEach { line ->
                Text(
                    text = line,
                    style = MetroTypography.tileSubtext.copy(fontSize = 13.sp),
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (size != TileSize.SMALL) {
            Text(
                text = label,
                style = MetroTypography.tileLabel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.BottomStart)
            )
        }
    }
}

@Composable
private fun DateTemplate(face: LiveTileFace, size: TileSize, label: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(if (size == TileSize.SMALL) MetroDimensions.tileContentPaddingSmall else MetroDimensions.tileContentPadding)
    ) {
        when (size) {
            TileSize.SMALL -> {
                Text(
                    text = face.primaryText ?: "",
                    style = MetroTypography.tileCountNumber.copy(
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Light
                    ),
                    color = Color.White,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            TileSize.MEDIUM -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopStart)
                ) {
                    Text(
                        text = face.primaryText ?: "",
                        style = MetroTypography.tileCountNumber.copy(
                            fontSize = 46.sp,
                            fontWeight = FontWeight.Light,
                            lineHeight = 48.sp
                        ),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (face.secondaryText != null) {
                        Text(
                            text = face.secondaryText,
                            style = MetroTypography.tileSubtext.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Text(
                    text = label,
                    style = MetroTypography.tileLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.align(Alignment.BottomStart)
                )
            }
            TileSize.WIDE, TileSize.LARGE -> {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 22.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Large Day Number
                    Text(
                        text = face.primaryText ?: "",
                        style = MetroTypography.tileCountNumber.copy(
                            fontSize = 58.sp,
                            fontWeight = FontWeight.Light
                        ),
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(end = 14.dp)
                    )

                    // Day & Month info + agenda lines
                    Column(modifier = Modifier.weight(1f)) {
                        if (face.secondaryText != null) {
                            Text(
                                text = face.secondaryText,
                                style = MetroTypography.tileLabel.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (face.tertiaryText != null) {
                            Text(
                                text = face.tertiaryText,
                                style = MetroTypography.tileSubtext.copy(fontSize = 13.sp),
                                color = Color.White.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        face.textLines.take(2).forEach { line ->
                            Text(
                                text = line,
                                style = MetroTypography.tileSubtext.copy(fontSize = 12.sp),
                                color = Color.White.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Text(
                    text = label,
                    style = MetroTypography.tileLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.align(Alignment.BottomStart)
                )
            }
        }
    }
}

@Composable
private fun ImageTemplate(face: LiveTileFace, size: TileSize, label: String) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (face.imageBitmap != null) {
            Image(
                bitmap = face.imageBitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (size != TileSize.SMALL) {
            Text(
                text = label,
                style = MetroTypography.tileLabel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(if (size == TileSize.SMALL) MetroDimensions.tileContentPaddingSmall else MetroDimensions.tileContentPadding)
            )
        }
    }
}

@Composable
private fun ImageAndTextTemplate(face: LiveTileFace, size: TileSize, label: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(if (size == TileSize.SMALL) MetroDimensions.tileContentPaddingSmall else MetroDimensions.tileContentPadding)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (size != TileSize.SMALL) 22.dp else 0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (face.imageBitmap != null) {
                Image(
                    bitmap = face.imageBitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(if (size == TileSize.SMALL) 32.dp else 48.dp)
                        .padding(end = 8.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                if (face.primaryText != null) {
                    Text(
                        text = face.primaryText,
                        style = MetroTypography.tileLabel.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White,
                        maxLines = 1
                    )
                }
                if (face.secondaryText != null) {
                    Text(
                        text = face.secondaryText,
                        style = MetroTypography.tileSubtext,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 1
                    )
                }
            }
        }

        if (size != TileSize.SMALL) {
            Text(
                text = label,
                style = MetroTypography.tileLabel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.BottomStart)
            )
        }
    }
}
