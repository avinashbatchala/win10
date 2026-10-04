package com.example.model

data class TileModel(
    val id: String,
    val packageName: String,
    val activityName: String = "",
    val label: String,
    val size: TileSize = TileSize.MEDIUM,
    val col: Int = 0,
    val row: Int = 0,
    val customColor: Long? = null,
    val customLabel: String? = null
) {
    val effectiveCols: Int get() = size.cols
    val effectiveRows: Int get() = size.rows
}
