package com.ab.livetile.model

/**
 * Encapsulates the complete live state for a tile across all available faces.
 */
data class LiveTileState(
    val providerId: String,
    val faces: List<LiveTileFace>,
    val activeFaceIndex: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis(),
    val validityDurationMs: Long = 60_000L,
    val isAvailable: Boolean = true,
    val error: String? = null
) {
    val activeFace: LiveTileFace?
        get() = if (faces.isNotEmpty()) faces[activeFaceIndex.coerceIn(0, faces.size - 1)] else null

    val isStale: Boolean
        get() = (System.currentTimeMillis() - lastUpdated) > validityDurationMs

    fun nextFace(): LiveTileState {
        if (faces.size <= 1) return this
        val nextIndex = (activeFaceIndex + 1) % faces.size
        return copy(activeFaceIndex = nextIndex)
    }
}
