package com.ab.livetile.engine

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.ab.livetile.api.LiveTileProvider
import com.ab.livetile.model.LiveTileState
import com.ab.livetile.providers.MediaLiveTileProvider
import com.ab.livetile.providers.MetroClockTileClient
import com.ab.media.MediaSessionRepository
import com.ab.model.TileModel
import com.ab.model.TileSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Core coordinator of the Windows 10 Mobile Live Tile engine.
 * Resolves providers for pinned tiles, caches states, schedules content refreshes,
 * and isolates provider errors.
 */
class LiveTileManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val mediaRepository: MediaSessionRepository? = null,
    private val isMediaLiveTilesEnabled: () -> Boolean = { true },
    private val isMediaArtworkEnabled: () -> Boolean = { true },
    private val shouldPauseWhenHidden: () -> Boolean = { true },
    private val shouldPauseInBatterySaver: () -> Boolean = { false },
    private val isBatterySaverOn: () -> Boolean = { false }
) {
    companion object {
        private const val TAG = "LiveTileManager"
    }

    val registry = LiveTileRegistry()
    private val mediaProvider = mediaRepository?.let { MediaLiveTileProvider(it, isMediaArtworkEnabled) }

    private val _tileStates = MutableStateFlow<Map<String, LiveTileState>>(emptyMap())
    val tileStates: StateFlow<Map<String, LiveTileState>> = _tileStates.asStateFlow()

    // Cache of active providers mapped to tile keys
    private val activeTileProviders = ConcurrentHashMap<String, LiveTileProvider>()
    // Current tile sizes observed
    private val tileModels = ConcurrentHashMap<String, TileModel>()

    // Background periodic refresh jobs
    private val refreshJobs = ConcurrentHashMap<String, Job>()

    // Cross-APK invalidation observer (MetroClock notifies its content URI on state changes).
    private var externalObserver: ContentObserver? = null

    // Face rotation scheduler
    private val scheduler = LiveTileScheduler(scope) { targetKey ->
        rotateFace(targetKey)
    }

    init {
        if (mediaProvider != null) {
            registry.registerProvider(mediaProvider)
            observeMediaSessions()
        }
        scheduler.start()
    }

    private fun observeMediaSessions() {
        if (mediaRepository == null || mediaProvider == null) return

        scope.launch {
            mediaRepository.activeSessionsByPackage.collect { activeMap ->
                if (!isMediaLiveTilesEnabled()) return@collect
                recomputeMediaTiles()
            }
        }

        scope.launch {
            mediaRepository.primarySession.collect { _ ->
                if (!isMediaLiveTilesEnabled()) return@collect
                recomputeMediaTiles()
            }
        }
    }

    fun recomputeMediaTiles() {
        if (mediaProvider == null) return
        val currentStates = _tileStates.value.toMutableMap()
        val mediaEnabled = isMediaLiveTilesEnabled()

        for ((key, tile) in tileModels) {
            val isNowPlaying = tile.packageName == MediaLiveTileProvider.NOW_PLAYING_PACKAGE
            val hasActiveSession = mediaRepository?.activeSessionsByPackage?.value?.containsKey(tile.packageName) == true

            if (mediaEnabled && (isNowPlaying || hasActiveSession)) {
                val state = mediaProvider.getMediaTileState(tile.packageName, tile.size, tile.label)
                if (state != null) {
                    currentStates[key] = state
                } else {
                    currentStates.remove(key)
                }
            } else if (currentStates[key]?.providerId?.startsWith("livetile.system.media") == true) {
                // Revert to static tile when media stops
                currentStates.remove(key)
            }
        }

        _tileStates.value = currentStates
        updateSchedulerEligibleTiles()
    }

    /**
     * Informs LiveTileManager of the current set of pinned tiles on Start.
     */
    fun syncPinnedTiles(tiles: List<TileModel>) {
        val currentKeys = tiles.map { it.componentKey }.toSet()

        // Clean up removed tiles
        val removedKeys = tileModels.keys.filterNot { currentKeys.contains(it) }
        removedKeys.forEach { key ->
            tileModels.remove(key)
            activeTileProviders.remove(key)
            refreshJobs.remove(key)?.cancel()
        }

        // Register and update current tiles
        tiles.forEach { tile ->
            tileModels[tile.componentKey] = tile
            val provider = registry.findProvider(tile.packageName, tile.activityName)
            if (provider != null) {
                activeTileProviders[tile.componentKey] = provider
                startProviderSchedule(tile.componentKey, tile, provider)
            } else {
                activeTileProviders.remove(tile.componentKey)
                refreshJobs.remove(tile.componentKey)?.cancel()
            }
        }

        recomputeMediaTiles()
        updateSchedulerEligibleTiles()
    }

    private fun startProviderSchedule(tileKey: String, tile: TileModel, provider: LiveTileProvider) {
        if (provider is MediaLiveTileProvider) {
            // Media tiles are event-driven by MediaSessionRepository callbacks
            return
        }

        if (refreshJobs.containsKey(tileKey)) return

        refreshJobs[tileKey] = scope.launch(Dispatchers.IO) {
            // Initial fetch
            refreshTileState(tileKey, tile, provider)

            while (isActive) {
                val interval = provider.refreshIntervalMs.coerceAtLeast(10_000L)
                delay(interval)
                val currentTile = tileModels[tileKey] ?: tile
                refreshTileState(tileKey, currentTile, provider)
            }
        }
    }

    private suspend fun refreshTileState(tileKey: String, tile: TileModel, provider: LiveTileProvider) {
        try {
            val newState = provider.getLiveTileState(context, tile.size, tile)
            if (newState != null && newState.faces.isNotEmpty()) {
                val current = _tileStates.value[tileKey]
                val preservedFaceIndex = if (current != null && current.providerId == newState.providerId) {
                    current.activeFaceIndex % newState.faces.size
                } else {
                    0
                }

                val finalState = newState.copy(activeFaceIndex = preservedFaceIndex)
                _tileStates.value = _tileStates.value + (tileKey to finalState)
                updateSchedulerEligibleTiles()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Provider ${provider.providerId} failed for tile $tileKey", e)
            val fallback = LiveTileState(
                providerId = provider.providerId,
                faces = emptyList(),
                isAvailable = false,
                error = e.localizedMessage
            )
            _tileStates.value = _tileStates.value + (tileKey to fallback)
        }
    }

    private fun rotateFace(tileKey: String) {
        val current = _tileStates.value[tileKey] ?: return
        if (current.faces.size > 1) {
            val updated = current.nextFace()
            _tileStates.value = _tileStates.value + (tileKey to updated)
        }
    }

    private fun updateSchedulerEligibleTiles() {
        val multiFaceKeys = _tileStates.value.filter { (_, state) ->
            state.faces.size > 1
        }.keys.toList()
        scheduler.updateEligibleTiles(multiFaceKeys)
    }

    /**
     * Resolves live state for a given tile.
     * Returns null if no live state or if fallback to static tile is required.
     */
    fun getLiveTileState(packageName: String, activityName: String? = null): LiveTileState? {
        val componentKey = if (!activityName.isNullOrEmpty()) "$packageName/$activityName" else packageName

        // Check if media Live Tile is applicable for this tile
        if (isMediaLiveTilesEnabled() && mediaProvider != null) {
            val isNowPlaying = packageName == MediaLiveTileProvider.NOW_PLAYING_PACKAGE
            val hasActiveSession = mediaRepository?.activeSessionsByPackage?.value?.containsKey(packageName) == true

            if (isNowPlaying || hasActiveSession) {
                val tile = tileModels[componentKey]
                val size = tile?.size ?: TileSize.MEDIUM
                val label = tile?.label ?: "Music"
                val mediaState = mediaProvider.getMediaTileState(packageName, size, label)
                if (mediaState != null) return mediaState
            }
        }

        val state = _tileStates.value[componentKey] ?: _tileStates.value[packageName]
        return if (state != null && state.isAvailable && state.faces.isNotEmpty()) state else null
    }

    fun setAnimationsEnabled(enabled: Boolean) {
        scheduler.setAnimationsEnabled(enabled)
    }

    fun setAnimationDelay(delayMs: Long) {
        scheduler.setFlipInterval(delayMs)
    }

    fun isSchedulerRunning(): Boolean = scheduler.isRunning

    fun onStart() {
        registry.getAllProviders().forEach { it.onStart(context) }
        registerExternalObserver()
        // Respect battery saver if the user asked us to.
        if (!(isBatterySaverOn() && shouldPauseInBatterySaver())) {
            scheduler.resume()
        }
        recomputeMediaTiles()
        // Refresh stale tiles asynchronously
        scope.launch(Dispatchers.IO) {
            tileModels.forEach { (key, tile) ->
                val provider = activeTileProviders[key]
                if (provider != null && provider !is MediaLiveTileProvider) {
                    val current = _tileStates.value[key]
                    if (current == null || current.isStale) {
                        refreshTileState(key, tile, provider)
                    }
                }
            }
        }
    }

    fun onStop() {
        if (shouldPauseWhenHidden()) {
            scheduler.pause()
        }
        unregisterExternalObserver()
        registry.getAllProviders().forEach { it.onStop(context) }
    }

    /**
     * Observe MetroClock's content URI so timer/stopwatch/alarm transitions refresh the tile
     * immediately, without polling the Clock app once per second.
     */
    private fun registerExternalObserver() {
        if (externalObserver != null) return
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                scope.launch(Dispatchers.IO) { refreshProviderTiles(MetroClockTileClient.PROVIDER_ID) }
            }
        }
        try {
            context.contentResolver.registerContentObserver(MetroClockTileClient.URI, false, observer)
            externalObserver = observer
        } catch (e: Exception) {
            Log.w(TAG, "Failed to observe ${MetroClockTileClient.URI}", e)
        }
    }

    private fun unregisterExternalObserver() {
        val observer = externalObserver ?: return
        try {
            context.contentResolver.unregisterContentObserver(observer)
        } catch (_: Exception) {
        }
        externalObserver = null
    }

    private suspend fun refreshProviderTiles(providerId: String) {
        tileModels.forEach { (key, tile) ->
            val provider = activeTileProviders[key] ?: return@forEach
            if (provider.providerId == providerId) {
                refreshTileState(key, tile, provider)
            }
        }
    }
}
