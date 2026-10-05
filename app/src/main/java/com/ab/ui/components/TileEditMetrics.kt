package com.ab.ui.components

/**
 * Sizes the per-tile edit controls (unpin + resize) so the two stacked buttons always
 * fit inside the tile. On a 1x1 SMALL tile the default 32.dp controls overlap, so the
 * buttons shrink with the tile instead of being a fixed size.
 */
object TileEditMetrics {

    /** Control size for a tile whose smallest side is [tileMinDp] dp. Capped at 32.dp. */
    fun buttonSizeDp(tileMinDp: Float): Float = minOf(32f, tileMinDp * 0.40f)

    /** Glyph size inside the control. Capped at 18.dp. */
    fun iconSizeDp(tileMinDp: Float): Float = minOf(18f, buttonSizeDp(tileMinDp) * 0.58f)

    /** Inset between the control and the tile edge. Capped at 4.dp. */
    fun paddingDp(tileMinDp: Float): Float = minOf(4f, tileMinDp * 0.04f)

    /**
     * The two controls are anchored to the top-end and bottom-end of the tile and are
     * stacked vertically, so combined they occupy `2 * (button + 2 * padding)`. Returns
     * true when that fits without overlap.
     */
    fun controlsFit(tileMinDp: Float): Boolean {
        val perControl = buttonSizeDp(tileMinDp) + (2f * paddingDp(tileMinDp))
        return (2f * perControl) <= tileMinDp
    }
}
