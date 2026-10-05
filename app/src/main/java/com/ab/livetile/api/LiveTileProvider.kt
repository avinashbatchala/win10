package com.ab.livetile.api

import android.content.Context
import com.ab.livetile.model.LiveTileState
import com.ab.model.TileModel
import com.ab.model.TileSize

/**
 * Standard contract for Live Tile data providers.
 * Providers supply structured content (states, faces, templates) without controlling UI layout.
 *
 * Designed to be implemented both in-process by built-in providers and out-of-process
 * by future companion applications (Calendar, Messages, Weather, Photos).
 */
interface LiveTileProvider {

    /**
     * Unique identifier for this provider (e.g. "livetile.system.clock").
     */
    val providerId: String

    /**
     * Human-readable name for diagnostics.
     */
    val displayName: String

    /**
     * Content refresh interval in milliseconds.
     * Content updates are separated from visual face animation transitions.
     */
    val refreshIntervalMs: Long

    /**
     * Returns whether this provider can supply Live Tile data for the specified app.
     */
    fun matchesComponent(packageName: String, activityName: String?): Boolean

    /**
     * Fetches current Live Tile state for the given tile dimensions and (optional) tile.
     * The tile is supplied so providers can use per-tile data such as the pinned weather
     * city. Must be resilient and return null on failure rather than throwing.
     */
    suspend fun getLiveTileState(
        context: Context,
        tileSize: TileSize,
        tile: TileModel? = null
    ): LiveTileState?

    /**
     * Called when the launcher becomes active and foregrounded.
     */
    fun onStart(context: Context) {}

    /**
     * Called when the launcher goes to background.
     */
    fun onStop(context: Context) {}
}
