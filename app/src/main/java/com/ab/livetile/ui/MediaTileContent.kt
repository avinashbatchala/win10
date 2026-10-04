package com.ab.livetile.ui

import android.os.SystemClock
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ab.livetile.model.LiveTileFace
import com.ab.media.MediaActionDispatcher
import com.ab.media.MediaSessionUiState
import com.ab.model.TileSize
import com.ab.ui.icons.MetroIcons
import com.ab.ui.theme.MetroDimensions
import com.ab.ui.theme.MetroTypography
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Locale

@Composable
fun MediaTileContent(
    face: LiveTileFace,
    size: TileSize,
    defaultLabel: String,
    dispatcher: MediaActionDispatcher? = null,
    showControls: Boolean = true,
    showProgress: Boolean = true,
    modifier: Modifier = Modifier
) {
    val media = face.mediaState
    val label = face.labelOverride ?: defaultLabel

    if (media == null) {
        // Fallback: simple music glyph and label
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(MetroDimensions.tileContentPadding)
        ) {
            Icon(
                imageVector = MetroIcons.Music,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(if (size == TileSize.SMALL) 24.dp else 36.dp)
                    .align(if (size == TileSize.SMALL) Alignment.Center else Alignment.Center)
            )
            if (size != TileSize.SMALL) {
                Text(
                    text = label,
                    style = MetroTypography.tileLabel,
                    modifier = Modifier.align(Alignment.BottomStart)
                )
            }
        }
        return
    }

    when (size) {
        TileSize.SMALL -> SmallMediaTile(face, media)
        TileSize.MEDIUM -> MediumMediaTile(face, media, label, dispatcher, showControls)
        TileSize.WIDE -> WideMediaTile(face, media, label, dispatcher, showControls, showProgress)
        TileSize.LARGE -> LargeMediaTile(face, media, label, dispatcher, showControls, showProgress)
    }
}

@Composable
private fun SmallMediaTile(face: LiveTileFace, media: MediaSessionUiState) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (face.imageBitmap != null) {
            Image(
                bitmap = face.imageBitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
            )
        }
        // Small center play/pause indicator glyph
        Icon(
            imageVector = if (media.isPlaying) MetroIcons.Play else MetroIcons.Pause,
            contentDescription = if (media.isPlaying) "Playing" else "Paused",
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun MediumMediaTile(
    face: LiveTileFace,
    media: MediaSessionUiState,
    label: String,
    dispatcher: MediaActionDispatcher?,
    showControls: Boolean
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Artwork Background if available
        if (face.imageBitmap != null) {
            Image(
                bitmap = face.imageBitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Subtle Windows-style vertical gradient for text readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.15f),
                                Color.Black.copy(alpha = 0.75f)
                            )
                        )
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(MetroDimensions.tileContentPadding),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Row: Play/Pause state badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = MetroIcons.Music,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(20.dp)
                )

                // Quick Play/Pause tap target
                if (showControls) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color.Black.copy(alpha = 0.4f), RectangleShape)
                            .clickable {
                                if (media.isPlaying) {
                                    dispatcher?.pause(media.packageName)
                                } else {
                                    dispatcher?.play(media.packageName)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (media.isPlaying) MetroIcons.Pause else MetroIcons.Play,
                            contentDescription = if (media.isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Bottom: Title & Subtitle + App label
            Column {
                Text(
                    text = media.title,
                    style = MetroTypography.tileLabel.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White
                )
                if (!media.displaySubtitle.isNullOrBlank()) {
                    Text(
                        text = media.displaySubtitle,
                        style = MetroTypography.tileSubtext.copy(fontSize = 12.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = label,
                    style = MetroTypography.tileLabel.copy(fontSize = 11.sp),
                    color = Color.White.copy(alpha = 0.65f),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun WideMediaTile(
    face: LiveTileFace,
    media: MediaSessionUiState,
    label: String,
    dispatcher: MediaActionDispatcher?,
    showControls: Boolean,
    showProgress: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(MetroDimensions.tileContentPadding)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (media.durationMs > 0) 6.dp else 0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Artwork square region on left
            Box(
                modifier = Modifier
                    .size(width = 82.dp, height = 82.dp)
                    .background(Color(0xFF101010), RectangleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RectangleShape),
                contentAlignment = Alignment.Center
            ) {
                if (face.imageBitmap != null) {
                    Image(
                        bitmap = face.imageBitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = MetroIcons.Music,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Right region: Metadata and Transport Controls
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = media.title,
                        style = MetroTypography.tileLabel.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.White
                    )
                    if (!media.displaySubtitle.isNullOrBlank()) {
                        Text(
                            text = media.displaySubtitle,
                            style = MetroTypography.tileSubtext.copy(fontSize = 12.5.sp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }

                // Transport Controls: Previous, Play/Pause, Next
                if (showControls) Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (media.canSkipToPrevious) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clickable { dispatcher?.skipPrevious(media.packageName) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = MetroIcons.SkipPrevious,
                                contentDescription = "Previous track",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    if (media.canPlay || media.canPause) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clickable {
                                    if (media.isPlaying) {
                                        dispatcher?.pause(media.packageName)
                                    } else {
                                        dispatcher?.play(media.packageName)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (media.isPlaying) MetroIcons.Pause else MetroIcons.Play,
                                contentDescription = if (media.isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    if (media.canSkipToNext) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clickable { dispatcher?.skipNext(media.packageName) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = MetroIcons.SkipNext,
                                contentDescription = "Next track",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Playback progress bar along bottom if duration is known
        if (showProgress && media.durationMs > 0) {
            MediaProgressBar(
                positionMs = media.positionMs,
                durationMs = media.durationMs,
                isPlaying = media.isPlaying,
                playbackSpeed = media.playbackSpeed,
                lastUpdateTime = media.lastPositionUpdateTime,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun LargeMediaTile(
    face: LiveTileFace,
    media: MediaSessionUiState,
    label: String,
    dispatcher: MediaActionDispatcher?,
    showControls: Boolean,
    showProgress: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(MetroDimensions.tileContentPadding * 1.25f)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Prominent Artwork Hero Region
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF101010), RectangleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RectangleShape),
                contentAlignment = Alignment.Center
            ) {
                if (face.imageBitmap != null) {
                    Image(
                        bitmap = face.imageBitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = MetroIcons.Music,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(64.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Metadata: Track title and Artist/Album
            Column {
                Text(
                    text = media.title,
                    style = MetroTypography.tileLargeHeader.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White
                )
                if (!media.displaySubtitle.isNullOrBlank()) {
                    Text(
                        text = media.displaySubtitle,
                        style = MetroTypography.tileSubtext.copy(fontSize = 14.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Transport Controls: Centered Previous, Play/Pause, Next
            if (showControls) Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (media.canSkipToPrevious) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clickable { dispatcher?.skipPrevious(media.packageName) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = MetroIcons.SkipPrevious,
                            contentDescription = "Previous track",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                }

                if (media.canPlay || media.canPause) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.White.copy(alpha = 0.15f), RectangleShape)
                            .clickable {
                                if (media.isPlaying) {
                                    dispatcher?.pause(media.packageName)
                                } else {
                                    dispatcher?.play(media.packageName)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (media.isPlaying) MetroIcons.Pause else MetroIcons.Play,
                            contentDescription = if (media.isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                }

                if (media.canSkipToNext) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clickable { dispatcher?.skipNext(media.packageName) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = MetroIcons.SkipNext,
                            contentDescription = "Next track",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Progress Bar with timestamps
            if (showProgress && media.durationMs > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                MediaProgressBarWithTime(
                    positionMs = media.positionMs,
                    durationMs = media.durationMs,
                    isPlaying = media.isPlaying,
                    playbackSpeed = media.playbackSpeed,
                    lastUpdateTime = media.lastPositionUpdateTime,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Localized ticking progress bar.
 * Updates position based on SystemClock.elapsedRealtime() interpolation without global recompositions.
 */
@Composable
fun MediaProgressBar(
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    playbackSpeed: Float,
    lastUpdateTime: Long,
    modifier: Modifier = Modifier
) {
    if (durationMs <= 0) return

    var currentPos by remember(positionMs, isPlaying, lastUpdateTime) {
        mutableLongStateOf(positionMs)
    }

    LaunchedEffect(positionMs, isPlaying, lastUpdateTime, playbackSpeed) {
        if (!isPlaying || playbackSpeed <= 0f) {
            currentPos = positionMs.coerceIn(0L, durationMs)
            return@LaunchedEffect
        }
        while (isActive) {
            val elapsed = SystemClock.elapsedRealtime() - lastUpdateTime
            val calculated = (positionMs + (elapsed * playbackSpeed).toLong()).coerceIn(0L, durationMs)
            currentPos = calculated
            delay(500L)
        }
    }

    val progress = (currentPos.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)

    LinearProgressIndicator(
        progress = { progress },
        modifier = modifier.height(3.dp),
        color = Color.White,
        trackColor = Color.White.copy(alpha = 0.25f)
    )
}

@Composable
fun MediaProgressBarWithTime(
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    playbackSpeed: Float,
    lastUpdateTime: Long,
    modifier: Modifier = Modifier
) {
    if (durationMs <= 0) return

    var currentPos by remember(positionMs, isPlaying, lastUpdateTime) {
        mutableLongStateOf(positionMs)
    }

    LaunchedEffect(positionMs, isPlaying, lastUpdateTime, playbackSpeed) {
        if (!isPlaying || playbackSpeed <= 0f) {
            currentPos = positionMs.coerceIn(0L, durationMs)
            return@LaunchedEffect
        }
        while (isActive) {
            val elapsed = SystemClock.elapsedRealtime() - lastUpdateTime
            val calculated = (positionMs + (elapsed * playbackSpeed).toLong()).coerceIn(0L, durationMs)
            currentPos = calculated
            delay(500L)
        }
    }

    val progress = (currentPos.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)

    Column(modifier = modifier) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp),
            color = Color.White,
            trackColor = Color.White.copy(alpha = 0.25f)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(currentPos),
                style = MetroTypography.tileSubtext.copy(fontSize = 11.sp),
                color = Color.White.copy(alpha = 0.75f)
            )
            Text(
                text = formatTime(durationMs),
                style = MetroTypography.tileSubtext.copy(fontSize = 11.sp),
                color = Color.White.copy(alpha = 0.75f)
            )
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}
