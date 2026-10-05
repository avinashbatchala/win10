package com.ab.livetile.ui

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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ab.livetile.model.LiveTileFace
import com.ab.livetile.model.WeatherDay
import com.ab.model.TileSize
import com.ab.ui.theme.MetroDimensions
import com.ab.ui.theme.MetroTypography

/**
 * Windows 10 Mobile MSN Weather Live Tile layout. The tile background is the tile colour;
 * everything here is white foreground. The tile's own label is the city name.
 */
@Composable
fun WeatherTileContent(face: LiveTileFace, size: TileSize, label: String) {
    val data = face.weather ?: return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                if (size == TileSize.SMALL) {
                    MetroDimensions.tileContentPaddingSmall
                } else {
                    MetroDimensions.tileContentPadding
                }
            )
    ) {
        when (size) {
            TileSize.SMALL -> SmallWeather(data)
            TileSize.MEDIUM -> MediumWeather(data, label)
            TileSize.WIDE -> WideWeather(data, label)
            TileSize.LARGE -> LargeWeather(data, label)
        }
    }
}

@Composable
private fun SmallWeather(data: com.ab.livetile.model.WeatherTileData) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = data.icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = data.temperatureText,
            style = MetroTypography.tileCountNumber.copy(fontSize = 16.sp, fontWeight = FontWeight.Light),
            color = Color.White,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MediumWeather(data: com.ab.livetile.model.WeatherTileData, label: String) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.align(Alignment.TopStart)) {
            Text(
                text = data.temperatureText,
                style = MetroTypography.tileLargeHeader.copy(fontSize = 44.sp, fontWeight = FontWeight.Light),
                color = Color.White,
                maxLines = 1
            )
            Text(
                text = data.conditionText,
                style = MetroTypography.tileSubtext.copy(fontSize = 13.sp),
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = data.icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.9f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(34.dp)
        )
        if (label.isNotBlank()) {
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
private fun WideWeather(data: com.ab.livetile.model.WeatherTileData, label: String) {
    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = data.temperatureText,
                    style = MetroTypography.tileLargeHeader.copy(fontSize = 38.sp, fontWeight = FontWeight.Light),
                    color = Color.White,
                    maxLines = 1
                )
                Text(
                    text = data.conditionText,
                    style = MetroTypography.tileSubtext.copy(fontSize = 12.sp),
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (data.days.isEmpty()) {
                Icon(
                    imageVector = data.icon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier
                        .size(44.dp)
                        .padding(end = 8.dp)
                )
            } else {
                data.days.take(3).forEach { day ->
                    ForecastColumn(day, Modifier.weight(1f), compact = true)
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

@Composable
private fun LargeWeather(data: com.ab.livetile.model.WeatherTileData, label: String) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 22.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = data.temperatureText,
                        style = MetroTypography.tileLargeHeader.copy(fontSize = 54.sp, fontWeight = FontWeight.Light),
                        color = Color.White,
                        maxLines = 1
                    )
                    Text(
                        text = data.conditionText,
                        style = MetroTypography.tileSubtext.copy(fontSize = 15.sp),
                        color = Color.White.copy(alpha = 0.9f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = data.icon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(52.dp)
                )
            }

            if (data.days.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    data.days.take(3).forEach { day -> ForecastColumn(day, Modifier.weight(1f)) }
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

@Composable
private fun ForecastColumn(day: WeatherDay, modifier: Modifier = Modifier, compact: Boolean = false) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = day.label,
            style = MetroTypography.tileSubtext.copy(fontSize = if (compact) 11.sp else 12.sp),
            color = Color.White.copy(alpha = 0.8f),
            maxLines = 1
        )
        Icon(
            imageVector = day.icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.9f),
            modifier = Modifier
                .padding(vertical = if (compact) 1.dp else 2.dp)
                .size(if (compact) 18.dp else 22.dp)
        )
        Text(
            text = day.highText,
            style = MetroTypography.tileLabel.copy(fontSize = if (compact) 12.sp else 13.sp),
            color = Color.White,
            maxLines = 1
        )
        Text(
            text = day.lowText,
            style = MetroTypography.tileSubtext.copy(fontSize = if (compact) 11.sp else 12.sp),
            color = Color.White.copy(alpha = 0.75f),
            maxLines = 1
        )
        if (!compact && day.precipChance != null && day.precipChance > 0) {
            Text(
                text = "${day.precipChance}%",
                style = MetroTypography.tileSubtext.copy(fontSize = 11.sp),
                color = Color.White.copy(alpha = 0.65f),
                maxLines = 1
            )
        }
    }
}
