package com.ab.livetile.model

/**
 * Windows 10 Mobile Live Tile templates.
 * The launcher owns visual layout and styling; providers supply structured data.
 */
enum class LiveTileTemplate {
    /**
     * Large centered glyph/icon with optional badge/count in corner and label.
     */
    ICONIC,

    /**
     * Large numeric counter (e.g., missed calls "3" or messages "12")
     * with secondary descriptive label and bottom app title.
     */
    COUNT,

    /**
     * Large primary headline (e.g., temperature "18°" or status "Active")
     * with supporting secondary and tertiary text lines.
     */
    PRIMARY_TEXT,

    /**
     * Multiple short text lines (e.g., calendar appointments, message snippets, news).
     */
    TEXT_LINES,

    /**
     * Large calendar date number (e.g., "4") plus day-of-week and month information.
     */
    DATE,

    /**
     * Full-bleed photo/image with optional bottom label overlay.
     */
    IMAGE,

    /**
     * Split layout with an image area plus structured textual information.
     */
    IMAGE_AND_TEXT,

    /**
     * Windows 10 Mobile Now Playing media template with artwork, track metadata,
     * transport playback controls, and interpolated progress.
     */
    MEDIA,

    /**
     * Windows 10 Mobile MSN Weather template: current temperature + condition glyph,
     * with a 3-day forecast on wide/large tiles. Data comes from [WeatherTileData].
     */
    WEATHER
}
