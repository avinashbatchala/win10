package com.ab.livetile.engine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Coordinates Live Tile visual transitions.
 * Ensures transitions are staggered and never animate all tiles simultaneously.
 * Respects foreground/background lifecycle.
 */
class LiveTileScheduler(
    private val scope: CoroutineScope,
    private val onStepFace: (tileKey: String) -> Unit
) {
    private var schedulerJob: Job? = null
    private val isForeground = AtomicBoolean(true)

    // Registered tiles with multiple faces eligible for staggered rotation
    private val eligibleTiles = mutableListOf<String>()
    private var currentTileIndex = 0

    fun updateEligibleTiles(tileKeys: List<String>) {
        synchronized(eligibleTiles) {
            eligibleTiles.clear()
            eligibleTiles.addAll(tileKeys)
            if (currentTileIndex >= eligibleTiles.size) {
                currentTileIndex = 0
            }
        }
    }

    fun start() {
        if (schedulerJob?.isActive == true) return
        schedulerJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                // Wait between 3.5 and 5 seconds between individual tile flips
                delay(4000L)

                if (!isForeground.get()) continue

                val targetKey = synchronized(eligibleTiles) {
                    if (eligibleTiles.isNotEmpty()) {
                        val key = eligibleTiles[currentTileIndex % eligibleTiles.size]
                        currentTileIndex = (currentTileIndex + 1) % eligibleTiles.size
                        key
                    } else null
                }

                if (targetKey != null) {
                    onStepFace(targetKey)
                }
            }
        }
    }

    fun pause() {
        isForeground.set(false)
    }

    fun resume() {
        isForeground.set(true)
        if (schedulerJob == null || schedulerJob?.isActive == false) {
            start()
        }
    }

    fun stop() {
        schedulerJob?.cancel()
        schedulerJob = null
    }
}
