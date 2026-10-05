package com.ab.livetile.model

import androidx.compose.ui.graphics.vector.ImageVector

/** A single forecast day shown on a wide/large weather Live Tile. */
data class WeatherDay(
    val label: String,
    val icon: ImageVector,
    val highText: String,
    val lowText: String,
    val precipChance: Int? = null
)

/**
 * Structured Windows 10 Mobile weather Live Tile content. The renderer owns the layout per
 * tile size; providers supply only the data. The tile's own label is used as the city name
 * so a pinned city can never be mixed with an IP-detected one.
 */
data class WeatherTileData(
    val temperatureText: String,
    val conditionText: String,
    val icon: ImageVector,
    val highText: String? = null,
    val lowText: String? = null,
    val days: List<WeatherDay> = emptyList()
)
