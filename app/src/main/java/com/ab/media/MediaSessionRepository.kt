package com.ab.media

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * Centralized repository for active Android MediaSessions.
 * Interacts with MediaSessionManager and MediaControllers to observe playback states,
 * normalize metadata, and expose clean, immutable state to Live Tiles.
 */
class MediaSessionRepository(
    private val context: Context,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "MediaSessionRepo"
        private const val ARTWORK_TARGET_SIZE = 256
    }

    private val mediaSessionManager =
        context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager

    private val listenerComponent = LauncherNotificationListenerService.getServiceComponent(context)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _isNotificationAccessGranted = MutableStateFlow(false)
    val isNotificationAccessGranted: StateFlow<Boolean> = _isNotificationAccessGranted.asStateFlow()

    private val _activeSessionsByPackage = MutableStateFlow<Map<String, MediaSessionUiState>>(emptyMap())
    val activeSessionsByPackage: StateFlow<Map<String, MediaSessionUiState>> = _activeSessionsByPackage.asStateFlow()

    private val _primarySession = MutableStateFlow<MediaSessionUiState?>(null)
    val primarySession: StateFlow<MediaSessionUiState?> = _primarySession.asStateFlow()

    // Controller management
    private val activeControllers = ConcurrentHashMap<String, MediaController>()
    private val controllerCallbacks = ConcurrentHashMap<String, MediaController.Callback>()
    private val artworkCache = ConcurrentHashMap<String, ImageBitmap>()

    private val sessionsChangedListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        Log.d(TAG, "OnActiveSessionsChanged: ${controllers?.size ?: 0} controllers reported.")
        handleActiveSessionsChanged(controllers ?: emptyList())
    }

    init {
        LauncherNotificationListenerService.setConnectivityListener { isConnected ->
            Log.d(TAG, "Notification listener connectivity updated: $isConnected")
            checkNotificationAccess()
            if (isConnected) {
                refreshActiveSessions()
            } else {
                clearAllSessions()
            }
        }

        checkNotificationAccess()
        if (_isNotificationAccessGranted.value) {
            registerSessionsListener()
            refreshActiveSessions()
        }
    }

    fun checkNotificationAccess(): Boolean {
        val cr = context.contentResolver
        val enabledListeners = Settings.Secure.getString(cr, "enabled_notification_listeners") ?: ""
        val ownPackage = context.packageName
        val isGranted = enabledListeners.contains(ownPackage) ||
                LauncherNotificationListenerService.isServiceConnected()
        _isNotificationAccessGranted.value = isGranted
        return isGranted
    }

    fun openNotificationAccessSettings(context: Context) {
        try {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            } else {
                Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.w(TAG, "Could not open notification listener settings", e)
        }
    }

    fun refreshActiveSessions() {
        if (!checkNotificationAccess()) {
            clearAllSessions()
            return
        }

        if (mediaSessionManager == null) return

        try {
            val controllers = mediaSessionManager.getActiveSessions(listenerComponent)
            handleActiveSessionsChanged(controllers)
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification listener access not permitted by system yet.", e)
            _isNotificationAccessGranted.value = false
            clearAllSessions()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to query active media sessions", e)
        }
    }

    private fun registerSessionsListener() {
        if (mediaSessionManager == null) return
        try {
            mediaSessionManager.addOnActiveSessionsChangedListener(
                sessionsChangedListener,
                listenerComponent,
                mainHandler
            )
            Log.d(TAG, "Registered OnActiveSessionsChangedListener successfully.")
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException registering session listener: ${e.message}")
            _isNotificationAccessGranted.value = false
        } catch (e: Exception) {
            Log.w(TAG, "Exception registering session listener", e)
        }
    }

    private fun unregisterSessionsListener() {
        try {
            mediaSessionManager?.removeOnActiveSessionsChangedListener(sessionsChangedListener)
        } catch (_: Exception) {}
    }

    private fun handleActiveSessionsChanged(controllers: List<MediaController>) {
        val currentPackages = controllers.map { it.packageName }.toSet()

        // Unregister detached controllers
        val removedPackages = activeControllers.keys.filterNot { currentPackages.contains(it) }
        for (pkg in removedPackages) {
            val controller = activeControllers.remove(pkg)
            val callback = controllerCallbacks.remove(pkg)
            if (controller != null && callback != null) {
                try {
                    controller.unregisterCallback(callback)
                } catch (_: Exception) {}
            }
        }

        // Register and update current controllers
        for (controller in controllers) {
            val pkg = controller.packageName
            val existing = activeControllers[pkg]
            if (existing == null || existing.sessionToken != controller.sessionToken) {
                existing?.let { old ->
                    controllerCallbacks.remove(pkg)?.let { old.unregisterCallback(it) }
                }

                activeControllers[pkg] = controller
                val callback = object : MediaController.Callback() {
                    override fun onPlaybackStateChanged(state: PlaybackState?) {
                        Log.d(TAG, "[$pkg] onPlaybackStateChanged: state=${state?.state}")
                        updateSessionState(controller)
                    }

                    override fun onMetadataChanged(metadata: MediaMetadata?) {
                        Log.d(TAG, "[$pkg] onMetadataChanged")
                        updateSessionState(controller)
                    }

                    override fun onSessionDestroyed() {
                        Log.d(TAG, "[$pkg] onSessionDestroyed")
                        activeControllers.remove(pkg)
                        controllerCallbacks.remove(pkg)
                        removeSessionForPackage(pkg)
                    }
                }
                controllerCallbacks[pkg] = callback
                try {
                    controller.registerCallback(callback, mainHandler)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to register controller callback for $pkg", e)
                }
            }
            updateSessionState(controller)
        }

        recomputeAllStates()
    }

    private fun updateSessionState(controller: MediaController) {
        val pkg = controller.packageName
        val metadata = controller.metadata
        val state = controller.playbackState

        val title = extractTitle(metadata) ?: return
        val artist = extractArtist(metadata)
        val album = extractAlbum(metadata)

        val supportedActions = state?.actions ?: 0L
        val playbackState = state?.state ?: PlaybackState.STATE_NONE
        val positionMs = state?.position ?: 0L
        val speed = state?.playbackSpeed ?: 1.0f
        val lastUpdate = state?.lastPositionUpdateTime ?: 0L

        val durationMs = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L

        // Artwork resolution
        val artCacheKey = "$pkg/${title}/${artist ?: ""}"
        val cachedArt = artworkCache[artCacheKey]

        val uiState = MediaSessionUiState(
            packageName = pkg,
            title = title,
            artist = artist,
            album = album,
            artworkBitmap = cachedArt,
            playbackState = playbackState,
            supportedActions = supportedActions,
            positionMs = positionMs,
            durationMs = durationMs,
            playbackSpeed = speed,
            lastPositionUpdateTime = lastUpdate,
            updateTime = System.currentTimeMillis()
        )

        val updatedMap = _activeSessionsByPackage.value.toMutableMap()
        updatedMap[pkg] = uiState
        _activeSessionsByPackage.value = updatedMap

        if (cachedArt == null && metadata != null) {
            loadArtworkAsync(pkg, artCacheKey, metadata)
        }

        recomputePrimarySession()
    }

    private fun loadArtworkAsync(pkg: String, cacheKey: String, metadata: MediaMetadata) {
        scope.launch(Dispatchers.IO) {
            val bitmap = extractArtworkBitmap(metadata)
            if (bitmap != null) {
                val imageBitmap = bitmap.asImageBitmap()
                artworkCache[cacheKey] = imageBitmap
                withContext(Dispatchers.Main) {
                    val current = _activeSessionsByPackage.value[pkg]
                    if (current != null) {
                        _activeSessionsByPackage.value = _activeSessionsByPackage.value + (pkg to current.copy(artworkBitmap = imageBitmap))
                        recomputePrimarySession()
                    }
                }
            }
        }
    }

    private fun extractArtworkBitmap(metadata: MediaMetadata): Bitmap? {
        // Try direct bitmap keys first
        val keys = arrayOf(
            MediaMetadata.METADATA_KEY_ALBUM_ART,
            MediaMetadata.METADATA_KEY_ART,
            MediaMetadata.METADATA_KEY_DISPLAY_ICON
        )
        for (key in keys) {
            try {
                val b = metadata.getBitmap(key)
                if (b != null && !b.isRecycled) {
                    return scaleBitmap(b, ARTWORK_TARGET_SIZE)
                }
            } catch (_: Exception) {}
        }

        // Try artwork URI
        val uriStr = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_ART_URI)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI)

        if (!uriStr.isNullOrEmpty()) {
            try {
                val uri = Uri.parse(uriStr)
                var stream: InputStream? = null
                try {
                    stream = context.contentResolver.openInputStream(uri)
                    val decoded = BitmapFactory.decodeStream(stream)
                    if (decoded != null) {
                        return scaleBitmap(decoded, ARTWORK_TARGET_SIZE)
                    }
                } finally {
                    stream?.close()
                }
            } catch (_: Exception) {}
        }

        return null
    }

    private fun scaleBitmap(src: Bitmap, target: Int): Bitmap {
        if (src.width <= target && src.height <= target) return src
        val maxDim = maxOf(src.width, src.height)
        val ratio = target.toFloat() / maxDim
        val w = (src.width * ratio).toInt().coerceAtLeast(1)
        val h = (src.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(src, w, h, true)
    }

    private fun extractTitle(metadata: MediaMetadata?): String? {
        if (metadata == null) return null
        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
        return if (!title.isNullOrBlank()) title.trim() else null
    }

    private fun extractArtist(metadata: MediaMetadata?): String? {
        if (metadata == null) return null
        val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_AUTHOR)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_COMPOSER)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
        return if (!artist.isNullOrBlank()) artist.trim() else null
    }

    private fun extractAlbum(metadata: MediaMetadata?): String? {
        if (metadata == null) return null
        val album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_DESCRIPTION)
        return if (!album.isNullOrBlank()) album.trim() else null
    }

    private fun removeSessionForPackage(pkg: String) {
        val updated = _activeSessionsByPackage.value.toMutableMap()
        updated.remove(pkg)
        _activeSessionsByPackage.value = updated
        recomputePrimarySession()
    }

    private fun recomputeAllStates() {
        val activePkgs = activeControllers.keys
        val filtered = _activeSessionsByPackage.value.filterKeys { activePkgs.contains(it) }
        _activeSessionsByPackage.value = filtered
        recomputePrimarySession()
    }

    /**
     * Deterministic Primary Session Selection Algorithm:
     * 1. Session currently in STATE_PLAYING
     * 2. Session currently in STATE_BUFFERING or STATE_CONNECTING
     * 3. Most recently active paused session (STATE_PAUSED)
     * 4. Otherwise null.
     */
    private fun recomputePrimarySession() {
        val sessions = _activeSessionsByPackage.value.values.toList()

        val playing = sessions.firstOrNull { it.playbackState == PlaybackState.STATE_PLAYING }
        if (playing != null) {
            _primarySession.value = playing
            return
        }

        val buffering = sessions.firstOrNull {
            it.playbackState == PlaybackState.STATE_BUFFERING ||
                    it.playbackState == PlaybackState.STATE_CONNECTING
        }
        if (buffering != null) {
            _primarySession.value = buffering
            return
        }

        val paused = sessions
            .filter { it.playbackState == PlaybackState.STATE_PAUSED }
            .maxByOrNull { it.updateTime }

        _primarySession.value = paused
    }

    fun clearAllSessions() {
        for ((pkg, controller) in activeControllers) {
            controllerCallbacks[pkg]?.let {
                try { controller.unregisterCallback(it) } catch (_: Exception) {}
            }
        }
        activeControllers.clear()
        controllerCallbacks.clear()
        _activeSessionsByPackage.value = emptyMap()
        _primarySession.value = null
    }

    // Transport Actions Dispatching
    fun sendPlay(packageName: String) {
        val controller = activeControllers[packageName] ?: activeControllers.values.firstOrNull()
        try {
            controller?.transportControls?.play()
            Log.d(TAG, "Sent transport PLAY to ${controller?.packageName}")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to send PLAY", e)
        }
    }

    fun sendPause(packageName: String) {
        val controller = activeControllers[packageName] ?: activeControllers.values.firstOrNull()
        try {
            controller?.transportControls?.pause()
            Log.d(TAG, "Sent transport PAUSE to ${controller?.packageName}")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to send PAUSE", e)
        }
    }

    fun sendSkipNext(packageName: String) {
        val controller = activeControllers[packageName] ?: activeControllers.values.firstOrNull()
        try {
            controller?.transportControls?.skipToNext()
            Log.d(TAG, "Sent transport SKIP_NEXT to ${controller?.packageName}")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to send SKIP_NEXT", e)
        }
    }

    fun sendSkipPrevious(packageName: String) {
        val controller = activeControllers[packageName] ?: activeControllers.values.firstOrNull()
        try {
            controller?.transportControls?.skipToPrevious()
            Log.d(TAG, "Sent transport SKIP_PREVIOUS to ${controller?.packageName}")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to send SKIP_PREVIOUS", e)
        }
    }

    fun sendSeekTo(packageName: String, positionMs: Long) {
        val controller = activeControllers[packageName] ?: activeControllers.values.firstOrNull()
        try {
            controller?.transportControls?.seekTo(positionMs)
            Log.d(TAG, "Sent transport SEEK_TO $positionMs to ${controller?.packageName}")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to send SEEK_TO", e)
        }
    }

    fun onResume() {
        checkNotificationAccess()
        if (_isNotificationAccessGranted.value) {
            refreshActiveSessions()
        }
    }

    fun onDestroy() {
        unregisterSessionsListener()
        clearAllSessions()
    }
}
