package com.ab.model

enum class TileSize(val cols: Int, val rows: Int) {
    SMALL(1, 1),
    MEDIUM(2, 2),
    WIDE(4, 2),
    LARGE(4, 4);

    /** WP8.1 resize cycle (no large tile): medium -> small -> wide -> medium. */
    fun next(): TileSize = when (this) {
        MEDIUM -> SMALL
        SMALL -> WIDE
        WIDE -> MEDIUM
        LARGE -> MEDIUM
    }
}
