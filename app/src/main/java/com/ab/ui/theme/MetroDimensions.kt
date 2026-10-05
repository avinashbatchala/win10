package com.ab.ui.theme

import androidx.compose.ui.unit.dp

object MetroDimensions {
    // Start Screen Grid (Windows Phone 8.1: 6 columns = 3 medium tiles across)
    val tileGap = 8.dp
    val startHorizontalInset = 16.dp
    val startTopInset = 10.dp
    val startBottomInset = 28.dp

    // Tile inner padding
    val tileContentPadding = 8.dp
    val tileContentPaddingSmall = 4.dp
    // Fixed glyph sizes matching the WP8.1 tile template (icon size is not proportional to the tile).
    val tileIconSizeSmall = 30.dp
    val tileIconSizeMedium = 48.dp
    val tileIconSizeWide = 48.dp
    val tileIconSizeLarge = 64.dp

    // Edit controls
    val editButtonSize = 32.dp
    val editIconSize = 18.dp

    // App List Dimensions (WP8.1: 46px icon tile, 18px page margin)
    val appListHorizontalInset = 18.dp
    val appListRowHeight = 54.dp
    val appListIconBoxSize = 46.dp
    val appListIconInnerSize = 30.dp
    val appListHeaderBoxSize = 46.dp
    val appListRowSpacing = 4.dp

    // Jump List Dimensions
    val jumpCellSize = 52.dp
    val jumpGridGap = 6.dp

    // Search Box
    val searchBoxHeight = 44.dp
    val searchBoxBorderWidth = 2.dp
}
