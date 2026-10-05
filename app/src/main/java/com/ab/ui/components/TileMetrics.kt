package com.ab.ui.components

import androidx.compose.ui.unit.Dp
import com.ab.model.TileSize

/**
 * Ratio-based Start tile metrics matching Windows Phone's iconic tiles, where the app logo
 * dominates the tile (~50-60% of its short side) with the app name below.
 */
object TileMetrics {

    /** App icon size for a tile whose shortest side is [minDimension]. */
    fun iconSize(size: TileSize, minDimension: Dp): Dp {
        val factor = when (size) {
            TileSize.SMALL -> 0.58f
            TileSize.MEDIUM -> 0.55f
            TileSize.WIDE -> 0.55f
            TileSize.LARGE -> 0.42f
        }
        return minDimension * factor
    }
}
