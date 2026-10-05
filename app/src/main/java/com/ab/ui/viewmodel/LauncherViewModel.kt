package com.ab.ui.viewmodel

import android.app.Application
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ab.BuildConfig
import com.ab.data.InstalledAppRepository
import com.ab.data.LauncherPreferences
import com.ab.model.AppIconPreference
import com.ab.model.DisplayScale
import com.ab.model.AppInfo
import com.ab.model.BackgroundStyle
import com.ab.model.IconRenderMode
import com.ab.model.LauncherOrientation
import com.ab.model.LauncherSettings
import com.ab.model.LiveTileAnimationFrequency
import com.ab.model.TileModel
import com.ab.model.TileSize
import com.ab.ui.settings.SettingsDestination
import com.ab.ui.settings.SettingsPivot
import com.ab.ui.settings.SystemTileDef
import com.ab.ui.settings.SystemTiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG_BACK = "LauncherBack"
        private const val TAG_UNINSTALL = "LauncherUninstall"
        private const val TAG_PACKAGE = "LauncherPackage"
        private const val TAG_WALLPAPER = "LauncherWallpaper"
        const val METRO_WEATHER_PACKAGE = "com.metroweather.app"
    }

    private val repository = InstalledAppRepository(application, viewModelScope)
    private val preferences = LauncherPreferences(application)

    // Last icon mode pushed to the repository, so we only reload when it actually changes.
    private var appliedIconMode: IconRenderMode? = null

    val installedApps: StateFlow<List<AppInfo>> = repository.installedApps
    val isAppsLoaded: StateFlow<Boolean> = repository.isLoaded

    private val _pinnedTiles = MutableStateFlow<List<TileModel>>(emptyList())
    val pinnedTiles: StateFlow<List<TileModel>> = _pinnedTiles.asStateFlow()

    private val _settings = MutableStateFlow(LauncherSettings())
    val settings: StateFlow<LauncherSettings> = _settings.asStateFlow()

    // Android MediaSession Integration
    val mediaSessionRepository = com.ab.media.MediaSessionRepository(application, viewModelScope)
    val mediaActionDispatcher = object : com.ab.media.MediaActionDispatcher {
        override fun play(packageName: String) = mediaSessionRepository.sendPlay(packageName)
        override fun pause(packageName: String) = mediaSessionRepository.sendPause(packageName)
        override fun skipNext(packageName: String) = mediaSessionRepository.sendSkipNext(packageName)
        override fun skipPrevious(packageName: String) = mediaSessionRepository.sendSkipPrevious(packageName)
        override fun seekTo(packageName: String, positionMs: Long) = mediaSessionRepository.sendSeekTo(packageName, positionMs)
    }

    // Live Tile Manager with MediaSessionProvider wired
    val liveTileManager = com.ab.livetile.engine.LiveTileManager(
        context = application,
        scope = viewModelScope,
        mediaRepository = mediaSessionRepository,
        isMediaLiveTilesEnabled = { _settings.value.showMediaLiveTiles },
        isMediaArtworkEnabled = { _settings.value.mediaShowArtwork },
        shouldPauseWhenHidden = { _settings.value.pauseLiveTilesWhenHidden },
        shouldPauseInBatterySaver = { _settings.value.pauseLiveTilesInBatterySaver },
        isBatterySaverOn = {
            val pm = application.getSystemService(Context.POWER_SERVICE) as? PowerManager
            pm?.isPowerSaveMode == true
        }
    )
    val liveTileStates: StateFlow<Map<String, com.ab.livetile.model.LiveTileState>> = liveTileManager.tileStates

    /**
     * Live tile states exposed to the UI, gated by the global Live Tiles preference.
     * Observed by Start so tiles update reactively (media changes, face flips) as soon as
     * the underlying state changes, instead of only on the next recomposition.
     */
    val visibleLiveTileStates: StateFlow<Map<String, com.ab.livetile.model.LiveTileState>> =
        combine(liveTileManager.tileStates, settings) { states, currentSettings ->
            if (currentSettings.liveTilesEnabled) states else emptyMap()
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyMap()
        )

    // Wallpaper bitmap loaded asynchronously
    private val _wallpaperBitmap = MutableStateFlow<ImageBitmap?>(null)
    val wallpaperBitmap: StateFlow<ImageBitmap?> = _wallpaperBitmap.asStateFlow()

    // Edit mode state
    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

    private val _selectedTileId = MutableStateFlow<String?>(null)
    val selectedTileId: StateFlow<String?> = _selectedTileId.asStateFlow()

    // Drag & Drop state
    private val _draggedTileId = MutableStateFlow<String?>(null)
    val draggedTileId: StateFlow<String?> = _draggedTileId.asStateFlow()

    private val _dragOffset = MutableStateFlow(Offset.Zero)
    val dragOffset: StateFlow<Offset> = _dragOffset.asStateFlow()

    // Search and Jump List state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()

    private val _isJumpListOpen = MutableStateFlow(false)
    val isJumpListOpen: StateFlow<Boolean> = _isJumpListOpen.asStateFlow()

    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

    // Internal settings navigation stack (Root -> Launcher(pivot) -> nested pages).
    private val _settingsBackStack = MutableStateFlow<List<SettingsDestination>>(emptyList())
    val settingsBackStack: StateFlow<List<SettingsDestination>> = _settingsBackStack.asStateFlow()

    private val _requestScrollToStart = MutableStateFlow(false)
    val requestScrollToStart: StateFlow<Boolean> = _requestScrollToStart.asStateFlow()

    val filteredApps: StateFlow<List<AppInfo>> = combine(
        installedApps,
        searchQuery,
        settings
    ) { apps, query, currentSettings ->
        val visible = apps.filterNot { currentSettings.hiddenApps.contains(it.packageName) }
        if (query.isBlank()) {
            visible
        } else {
            val q = query.trim().lowercase()
            visible.filter { it.label.lowercase().contains(q) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeLetters: StateFlow<Set<Char>> = installedApps.combine(searchQuery) { apps, _ ->
        apps.map { it.firstLetter }.toSet()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    init {
        // Observe settings
        viewModelScope.launch {
            preferences.settingsFlow.collect { newSettings ->
                val prevUri = _settings.value.backgroundImageUri
                _settings.value = newSettings.copy(
                    isDefaultLauncher = checkIsDefaultLauncher()
                )
                // Keep the Live Tile engine in sync with the persisted preferences.
                liveTileManager.setAnimationsEnabled(
                    newSettings.liveTilesEnabled && newSettings.animateLiveTiles
                )
                liveTileManager.setAnimationDelay(newSettings.liveTileAnimationFrequency.delayMs)
                // Apply the persisted icon preference to the app repository (source of truth).
                val iconMode = newSettings.appIconPreference.toRenderMode()
                if (iconMode != appliedIconMode) {
                    appliedIconMode = iconMode
                    repository.setIconModeOverride(iconMode)
                    repository.reloadApps()
                }
                if (newSettings.backgroundImageUri != prevUri || (_wallpaperBitmap.value == null && newSettings.backgroundImageUri != null)) {
                    loadWallpaperAsync(newSettings.backgroundImageUri)
                }
            }
        }

        // Initialize pinned tiles and synchronize with installed apps
        viewModelScope.launch {
            _pinnedTiles.collect { tiles ->
                liveTileManager.syncPinnedTiles(tiles)
            }
        }

        // Adopt layout changes written by other components while the launcher process is
        // alive (e.g. PinWeatherTileActivity handling a "pin to start" from MetroWeather).
        // Compares layout identity only, so locally-recomputed isAvailable flags are kept.
        viewModelScope.launch {
            preferences.pinnedTilesFlow.drop(1).collect { external ->
                if (external != null && layoutsDiffer(external, _pinnedTiles.value)) {
                    _pinnedTiles.value = external
                }
            }
        }

        viewModelScope.launch {
            val savedTiles = preferences.pinnedTilesFlow.first()
            if (savedTiles != null && savedTiles.isNotEmpty()) {
                _pinnedTiles.value = savedTiles
            }

            // Observe installed apps to generate initial layout or update availability
            combine(repository.isLoaded, repository.installedApps) { loaded, apps ->
                Pair(loaded, apps)
            }.collect { (loaded, apps) ->
                if (loaded) {
                    if (_pinnedTiles.value.isEmpty()) {
                        val defaultTiles = createDefaultLayout(apps)
                        _pinnedTiles.value = defaultTiles
                        preferences.savePinnedTiles(defaultTiles)
                    } else {
                        val installedPkgs = apps.map { it.packageName }.toSet()
                        // Launcher-owned system tiles (weather, now playing, clock, …) are not
                        // installed packages, so they must never be treated as "uninstalled".
                        val systemPkgs = SystemTiles.ALL.map { it.packageName }.toSet()
                        val isPresent = { pkg: String -> installedPkgs.contains(pkg) || systemPkgs.contains(pkg) }
                        val currentTiles = _pinnedTiles.value
                        val hasRemovedTiles = currentTiles.any { !isPresent(it.packageName) }

                        if (hasRemovedTiles) {
                            Log.d(TAG_PACKAGE, "Detected package removal. Purging uninstalled tiles and updating Start layout.")
                            val remainingTiles = currentTiles.filter { isPresent(it.packageName) }
                            val totalCols = if (_settings.value.showMoreTiles) 8 else 6
                            val compacted = GridManager.compactGrid(remainingTiles, totalCols)
                            _pinnedTiles.value = compacted
                            saveTiles(compacted)
                        } else {
                            val updated = currentTiles.map { tile ->
                                tile.copy(isAvailable = isPresent(tile.packageName))
                            }
                            if (updated != currentTiles) {
                                _pinnedTiles.value = updated
                            }
                        }
                    }
                }
            }
        }
    }

    private fun loadWallpaperAsync(uriString: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            if (uriString.isNullOrEmpty()) {
                _wallpaperBitmap.value = null
                return@launch
            }
            try {
                val uri = Uri.parse(uriString)
                val context = getApplication<Application>()
                val resolver = context.contentResolver

                // First decode bounds only
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                resolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, options)
                }

                if (options.outWidth <= 0 || options.outHeight <= 0) {
                    _wallpaperBitmap.value = null
                    return@launch
                }

                // Sample based on screen dimensions for memory efficiency
                val dm = context.resources.displayMetrics
                val targetW = dm.widthPixels.coerceAtLeast(720)
                val targetH = dm.heightPixels.coerceAtLeast(1280)

                var sampleSize = 1
                while (options.outWidth / (sampleSize * 2) >= targetW &&
                    options.outHeight / (sampleSize * 2) >= targetH) {
                    sampleSize *= 2
                }

                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }

                val decoded = resolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, decodeOptions)
                }

                _wallpaperBitmap.value = decoded?.asImageBitmap()
                Log.d(TAG_WALLPAPER, "Wallpaper decoded (${decoded?.width}x${decoded?.height})")
            } catch (e: Exception) {
                Log.w(TAG_WALLPAPER, "Failed to load wallpaper: $uriString", e)
                _wallpaperBitmap.value = null
            }
        }
    }

    fun setBackgroundImageUri(uriString: String?) {
        _settings.value = _settings.value.copy(backgroundImageUri = uriString)
        loadWallpaperAsync(uriString)
        viewModelScope.launch {
            preferences.updateBackgroundImageUri(uriString)
        }
    }

    fun setTileTransparency(transparency: Float) {
        val clamped = transparency.coerceIn(0.0f, 1.0f)
        _settings.value = _settings.value.copy(tileTransparency = clamped)
        viewModelScope.launch {
            preferences.updateTileTransparency(clamped)
        }
    }

    fun setTheme(dark: Boolean) {
        _settings.value = _settings.value.copy(darkTheme = dark)
        viewModelScope.launch {
            preferences.updateTheme(dark)
        }
    }

    /**
     * Consumes Back in internal launcher priority order:
     * 1. Close any modal / dialog / settings.
     * 2. Exit tile edit mode.
     * 3. Close search if active.
     * 4. Close alphabet jump overlay if open.
     * Returns true if internal state was handled; false if at page level.
     */
    fun handleInternalBack(): Boolean {
        if (_isSettingsOpen.value) {
            Log.d(TAG_BACK, "Back consumed: navigating back inside Settings.")
            navigateSettingsBack()
            return true
        }
        if (_isEditMode.value) {
            Log.d(TAG_BACK, "Back consumed: exiting tile edit mode.")
            exitEditMode()
            return true
        }
        if (_isSearchActive.value || _searchQuery.value.isNotEmpty()) {
            Log.d(TAG_BACK, "Back consumed: closing search and clearing query.")
            setSearchActive(false)
            return true
        }
        if (_isJumpListOpen.value) {
            Log.d(TAG_BACK, "Back consumed: closing alphabet jump list.")
            closeJumpList()
            return true
        }
        return false
    }

    fun resetToStartRoot() {
        Log.d("LauncherNav", "Resetting launcher to Start root destination.")
        exitEditMode()
        closeSettings()
        closeJumpList()
        setSearchActive(false)
        _requestScrollToStart.value = true
    }

    fun onScrollToStartHandled() {
        _requestScrollToStart.value = false
    }

    fun checkDefaultLauncherStatus() {
        val isDefault = checkIsDefaultLauncher()
        _settings.value = _settings.value.copy(isDefaultLauncher = isDefault)
    }

    private fun checkIsDefaultLauncher(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolveInfo = getApplication<Application>().packageManager.resolveActivity(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY
        )
        return resolveInfo?.activityInfo?.packageName == getApplication<Application>().packageName
    }

    fun requestSetDefaultLauncher(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
            if (roleManager?.isRoleAvailable(RoleManager.ROLE_HOME) == true &&
                !roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    context.startActivity(intent)
                    return
                } catch (_: Exception) {}
            }
        }
        val intent = Intent(Settings.ACTION_HOME_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(fallback)
            } catch (_: Exception) {}
        }
    }

    private fun createDefaultLayout(apps: List<AppInfo>): List<TileModel> {
        if (apps.isEmpty()) return emptyList()

        fun findApp(vararg keywords: String): AppInfo? {
            for (kw in keywords) {
                val match = apps.firstOrNull {
                    it.packageName.contains(kw, ignoreCase = true) ||
                    it.label.contains(kw, ignoreCase = true)
                }
                if (match != null) return match
            }
            return null
        }

        val usedPackages = mutableSetOf<String>()
        val tiles = mutableListOf<TileModel>()
        var orderIndex = 0

        fun addTile(app: AppInfo?, size: TileSize, col: Int, row: Int) {
            if (app != null && !usedPackages.contains(app.packageName)) {
                usedPackages.add(app.packageName)
                tiles.add(
                    TileModel(
                        id = UUID.randomUUID().toString(),
                        packageName = app.packageName,
                        activityName = app.activityName,
                        label = app.label,
                        size = size,
                        col = col,
                        row = row,
                        order = orderIndex++,
                        isAvailable = true
                    )
                )
            }
        }

        // 1. Phone (Medium 2x2 at 0, 0)
        addTile(findApp("dialer", "phone", "call"), TileSize.MEDIUM, 0, 0)
        // 2. Messaging (Medium 2x2 at 2, 0)
        addTile(findApp("message", "messaging", "sms", "mms"), TileSize.MEDIUM, 2, 0)
        // 3. People / Contacts (Small 1x1 at 4, 0)
        addTile(findApp("contacts", "people"), TileSize.SMALL, 4, 0)
        // 4. Camera (Small 1x1 at 5, 0)
        addTile(findApp("camera"), TileSize.SMALL, 5, 0)
        // 5. Photos / Gallery (Wide 4x2 at 0, 2)
        addTile(findApp("gallery", "photos", "photo", "image"), TileSize.WIDE, 0, 2)
        // 6. Settings (Medium 2x2 at 4, 2)
        addTile(findApp("settings", "config"), TileSize.MEDIUM, 4, 2)
        // 7. Browser (Medium 2x2 at 0, 4)
        addTile(findApp("chrome", "browser", "firefox", "edge"), TileSize.MEDIUM, 0, 4)
        // 8. Calendar (Medium 2x2 at 2, 4)
        addTile(findApp("calendar"), TileSize.MEDIUM, 2, 4)
        // 9. Clock (Small 1x1 at 4, 4)
        addTile(findApp("deskclock", "clock", "alarm"), TileSize.SMALL, 4, 4)
        // 10. Calculator (Small 1x1 at 5, 4)
        addTile(findApp("calculator", "calc"), TileSize.SMALL, 5, 4)
        // 11. Mail / Gmail (Medium 2x2 at 0, 6)
        addTile(findApp("gmail", "mail", "email", "outlook"), TileSize.MEDIUM, 0, 6)
        // 12. Maps (Medium 2x2 at 2, 6)
        addTile(findApp("maps", "navigation"), TileSize.MEDIUM, 2, 6)
        // 13. Music / Media (Medium 2x2 at 4, 6)
        addTile(findApp("music", "spotify", "audio", "youtube", "media"), TileSize.MEDIUM, 4, 6)

        // Fill remaining apps if any of the above were missing
        val remainingApps = apps.filter { !usedPackages.contains(it.packageName) }
        var appIdx = 0
        while (tiles.size < 12 && appIdx < remainingApps.size) {
            val app = remainingApps[appIdx++]
            val pos = GridManager.findFirstAvailablePosition(2, 2, tiles, 6)
            tiles.add(
                TileModel(
                    id = UUID.randomUUID().toString(),
                    packageName = app.packageName,
                    activityName = app.activityName,
                    label = app.label,
                    size = TileSize.MEDIUM,
                    col = pos.first,
                    row = pos.second,
                    order = orderIndex++,
                    isAvailable = true
                )
            )
        }

        return GridManager.compactGrid(tiles, 6)
    }

    fun getAppIcon(packageName: String, activityName: String? = null): ImageBitmap? {
        return repository.getAppIcon(packageName, activityName)
    }

    fun resolveLauncherIcon(
        packageName: String,
        activityName: String? = null,
        targetSizePx: Int = 144,
        iconModeOverride: com.ab.model.IconRenderMode? = null,
        customIconId: String? = null
    ): com.ab.model.ResolvedLauncherIcon {
        val mode = iconModeOverride ?: _settings.value.appIconPreference.toRenderMode()
        return repository.resolveLauncherIcon(packageName, activityName, targetSizePx, mode, customIconId)
    }

    fun launchApp(context: Context, packageName: String, activityName: String? = null, label: String = "") {
        val pm = context.packageManager
        var launched = false

        // 1. Try explicit component intent
        if (!activityName.isNullOrEmpty()) {
            try {
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    component = ComponentName(packageName, activityName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                }
                context.startActivity(intent)
                launched = true
            } catch (_: Exception) {}
        }

        // 2. Try LauncherApps API
        if (!launched && !activityName.isNullOrEmpty()) {
            try {
                val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
                if (launcherApps != null) {
                    val component = ComponentName(packageName, activityName)
                    launcherApps.startMainActivity(component, Process.myUserHandle(), null, null)
                    launched = true
                }
            } catch (_: Exception) {}
        }

        // 3. Fallback to package launch intent
        if (!launched) {
            try {
                val intent = pm.getLaunchIntentForPackage(packageName)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                    context.startActivity(intent)
                    launched = true
                }
            } catch (_: Exception) {}
        }

        // Graceful error notification if app is unavailable
        if (!launched) {
            val appName = label.ifEmpty { packageName }
            Toast.makeText(context, "$appName is no longer available", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Opens the MetroWeather companion app for the launcher-owned weather tile.
     * Falls back to a hint if MetroWeather is not installed.
     */
    fun launchWeatherApp(context: Context, label: String = "Weather") {
        val pm = context.packageManager
        try {
            val intent = pm.getLaunchIntentForPackage(METRO_WEATHER_PACKAGE)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                context.startActivity(intent)
                return
            }
        } catch (_: Exception) {
        }
        Toast.makeText(context, "Install MetroWeather to see the full forecast", Toast.LENGTH_SHORT).show()
    }

    fun uninstallApp(context: Context, packageName: String) {
        Log.d(TAG_UNINSTALL, "Triggering uninstall for package: $packageName")
        var started = false
        try {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.parse("package:$packageName")
                putExtra(Intent.EXTRA_RETURN_RESULT, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            started = true
            Log.d(TAG_UNINSTALL, "Native ACTION_DELETE intent launched for $packageName")
        } catch (e: Exception) {
            Log.w(TAG_UNINSTALL, "ACTION_DELETE failed for $packageName, trying ACTION_UNINSTALL_PACKAGE", e)
        }

        if (!started) {
            try {
                val fallbackIntent = Intent(Intent.ACTION_UNINSTALL_PACKAGE).apply {
                    data = Uri.parse("package:$packageName")
                    putExtra(Intent.EXTRA_RETURN_RESULT, true)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
                started = true
                Log.d(TAG_UNINSTALL, "Fallback ACTION_UNINSTALL_PACKAGE launched for $packageName")
            } catch (e: Exception) {
                Log.w(TAG_UNINSTALL, "ACTION_UNINSTALL_PACKAGE failed for $packageName", e)
            }
        }

        if (!started) {
            try {
                val settingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(settingsIntent)
                Log.d(TAG_UNINSTALL, "Fallback ACTION_APPLICATION_DETAILS_SETTINGS launched for $packageName")
            } catch (e: Exception) {
                Log.e(TAG_UNINSTALL, "All uninstall attempts failed for $packageName", e)
            }
        }
    }

    fun onTileLongClicked(tileId: String) {
        _isEditMode.value = true
        _selectedTileId.value = tileId
    }

    fun onTileClickedInEdit(tileId: String) {
        _selectedTileId.value = tileId
    }

    fun exitEditMode() {
        _isEditMode.value = false
        _selectedTileId.value = null
        _draggedTileId.value = null
        _dragOffset.value = Offset.Zero
    }

    fun resizeSelectedTile(tileId: String) {
        val current = _pinnedTiles.value
        val tile = current.firstOrNull { it.id == tileId } ?: return
        val nextSize = tile.size.next()
        val totalCols = if (_settings.value.showMoreTiles) 8 else 6
        val updated = GridManager.resizeTile(tileId, nextSize, current, totalCols)
        _pinnedTiles.value = updated
        saveTiles(updated)
    }

    fun unpinTile(tileId: String) {
        val current = _pinnedTiles.value
        val updated = current.filter { it.id != tileId }
        val totalCols = if (_settings.value.showMoreTiles) 8 else 6
        val compacted = GridManager.compactGrid(updated, totalCols)
        _pinnedTiles.value = compacted
        saveTiles(compacted)
        if (_selectedTileId.value == tileId) {
            _selectedTileId.value = null
        }
    }

    fun onTileDragStart(tileId: String) {
        _draggedTileId.value = tileId
        _selectedTileId.value = tileId
        _dragOffset.value = Offset.Zero
    }

    fun onTileDrag(dragAmount: Offset) {
        _dragOffset.value = _dragOffset.value + dragAmount
    }

    fun onTileDragEnd(targetCol: Int, targetRow: Int) {
        val tileId = _draggedTileId.value ?: return
        val current = _pinnedTiles.value
        val totalCols = if (_settings.value.showMoreTiles) 8 else 6
        val moved = GridManager.moveTile(tileId, targetCol, targetRow, current, totalCols)
        _pinnedTiles.value = moved
        saveTiles(moved)
        _draggedTileId.value = null
        _dragOffset.value = Offset.Zero
    }

    fun cancelTileDrag() {
        _draggedTileId.value = null
        _dragOffset.value = Offset.Zero
    }

    fun pinApp(app: AppInfo, size: TileSize? = null) {
        val actualSize = size ?: _settings.value.defaultTileSize
        val current = _pinnedTiles.value
        val totalCols = if (_settings.value.showMoreTiles) 8 else 6
        val pos = GridManager.findFirstAvailablePosition(actualSize.cols, actualSize.rows, current, totalCols)
        val maxOrder = (current.maxOfOrNull { it.order } ?: 0) + 1
        val newTile = TileModel(
            id = UUID.randomUUID().toString(),
            packageName = app.packageName,
            activityName = app.activityName,
            label = app.label,
            size = actualSize,
            col = pos.first,
            row = pos.second,
            order = maxOrder,
            isAvailable = true
        )
        val updated = current + newTile
        _pinnedTiles.value = updated
        saveTiles(updated)
    }

    fun isAppPinned(packageName: String): Boolean {
        return _pinnedTiles.value.any { it.packageName == packageName }
    }

    fun unpinAppByPackage(packageName: String) {
        val current = _pinnedTiles.value
        val updated = current.filter { it.packageName != packageName }
        val totalCols = if (_settings.value.showMoreTiles) 8 else 6
        val compacted = GridManager.compactGrid(updated, totalCols)
        _pinnedTiles.value = compacted
        saveTiles(compacted)
    }

    /**
     * True when two tile lists describe a different layout (ids, geometry, size or order).
     * Runtime-only fields such as [TileModel.isAvailable] are ignored so an unrelated
     * DataStore emission cannot clobber locally-recomputed availability.
     */
    private fun layoutsDiffer(a: List<TileModel>, b: List<TileModel>): Boolean {
        if (a.size != b.size) return true
        val byId = b.associateBy { it.id }
        return a.any { t ->
            val o = byId[t.id] ?: return true
            o.packageName != t.packageName ||
                o.col != t.col ||
                o.row != t.row ||
                o.size != t.size ||
                o.order != t.order
        }
    }

    private fun saveTiles(tiles: List<TileModel>) {
        viewModelScope.launch {
            preferences.savePinnedTiles(tiles)
        }
    }

    fun setAccentColor(colorLong: Long) {
        _settings.value = _settings.value.copy(accentColor = colorLong)
        viewModelScope.launch {
            preferences.updateAccentColor(colorLong)
        }
    }

    fun toggleShowMoreTiles() {
        val newValue = !_settings.value.showMoreTiles
        _settings.value = _settings.value.copy(showMoreTiles = newValue)
        val totalCols = if (newValue) 8 else 6
        val repacked = GridManager.repackGrid(_pinnedTiles.value, totalCols)
        _pinnedTiles.value = repacked
        saveTiles(repacked)
        viewModelScope.launch {
            preferences.updateShowMoreTiles(newValue)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSearchActive(active: Boolean) {
        _isSearchActive.value = active
        if (!active) {
            _searchQuery.value = ""
        }
    }

    fun openJumpList() {
        _isJumpListOpen.value = true
    }

    fun closeJumpList() {
        _isJumpListOpen.value = false
    }

    // ---------------------------------------------------------------------
    // Settings navigation
    // ---------------------------------------------------------------------

    fun openSettings() {
        _settingsBackStack.value = listOf(SettingsDestination.Root)
        _isSettingsOpen.value = true
    }

    fun openLauncherPivot(pivot: SettingsPivot, settingId: String? = null) {
        _settingsBackStack.value = listOf(
            SettingsDestination.Root,
            SettingsDestination.Launcher(pivot, settingId)
        )
        _isSettingsOpen.value = true
    }

    fun openSettingsDestination(destination: SettingsDestination) {
        val stack = _settingsBackStack.value
        _settingsBackStack.value = if (stack.isEmpty()) {
            listOf(SettingsDestination.Root, destination)
        } else {
            stack + destination
        }
        _isSettingsOpen.value = true
    }

    /** Returns true if Back was consumed; closes Settings when already at the root page. */
    fun navigateSettingsBack(): Boolean {
        val stack = _settingsBackStack.value
        if (stack.size > 1) {
            _settingsBackStack.value = stack.dropLast(1)
            return true
        }
        closeSettings()
        return true
    }

    fun closeSettings() {
        _isSettingsOpen.value = false
        _settingsBackStack.value = emptyList()
    }

    // ---------------------------------------------------------------------
    // Start / personalization setters
    // ---------------------------------------------------------------------

    fun setBackgroundStyle(style: BackgroundStyle) {
        _settings.value = _settings.value.copy(backgroundStyle = style)
        viewModelScope.launch { preferences.updateBackgroundStyle(style) }
    }

    fun setShowMoreTiles(enabled: Boolean) {
        if (enabled != _settings.value.showMoreTiles) {
            toggleShowMoreTiles()
        }
    }

    // ---------------------------------------------------------------------
    // Live Tiles
    // ---------------------------------------------------------------------

    fun setLiveTilesEnabled(enabled: Boolean) {
        _settings.value = _settings.value.copy(liveTilesEnabled = enabled)
        liveTileManager.setAnimationsEnabled(enabled && _settings.value.animateLiveTiles)
        viewModelScope.launch { preferences.updateLiveTilesEnabled(enabled) }
    }

    fun setAnimateLiveTiles(enabled: Boolean) {
        _settings.value = _settings.value.copy(animateLiveTiles = enabled)
        liveTileManager.setAnimationsEnabled(_settings.value.liveTilesEnabled && enabled)
        viewModelScope.launch { preferences.updateAnimateLiveTiles(enabled) }
    }

    fun setLiveTileAnimationFrequency(frequency: LiveTileAnimationFrequency) {
        _settings.value = _settings.value.copy(liveTileAnimationFrequency = frequency)
        liveTileManager.setAnimationDelay(frequency.delayMs)
        viewModelScope.launch { preferences.updateLiveTileAnimationFrequency(frequency) }
    }

    fun setPauseLiveTilesWhenHidden(enabled: Boolean) {
        _settings.value = _settings.value.copy(pauseLiveTilesWhenHidden = enabled)
        viewModelScope.launch { preferences.updatePauseLiveTilesWhenHidden(enabled) }
    }

    fun setPauseLiveTilesInBatterySaver(enabled: Boolean) {
        _settings.value = _settings.value.copy(pauseLiveTilesInBatterySaver = enabled)
        viewModelScope.launch { preferences.updatePauseLiveTilesInBatterySaver(enabled) }
    }

    fun setDefaultTileSize(size: TileSize) {
        _settings.value = _settings.value.copy(defaultTileSize = size)
        viewModelScope.launch { preferences.updateDefaultTileSize(size) }
    }

    fun setShowAppNames(enabled: Boolean) {
        _settings.value = _settings.value.copy(showAppNames = enabled)
        viewModelScope.launch { preferences.updateShowAppNames(enabled) }
    }

    fun setDisplayScale(scale: DisplayScale) {
        _settings.value = _settings.value.copy(displayScale = scale)
        viewModelScope.launch { preferences.updateDisplayScale(scale) }
    }

    // ---------------------------------------------------------------------
    // Apps
    // ---------------------------------------------------------------------

    fun setShowAlphabetJumpList(enabled: Boolean) {
        _settings.value = _settings.value.copy(showAlphabetJumpList = enabled)
        viewModelScope.launch { preferences.updateShowAlphabetJumpList(enabled) }
    }

    fun setAppIconPreference(preference: AppIconPreference) {
        _settings.value = _settings.value.copy(appIconPreference = preference)
        val mode = preference.toRenderMode()
        if (mode != appliedIconMode) {
            appliedIconMode = mode
            repository.setIconModeOverride(mode)
            repository.reloadApps()
        }
        viewModelScope.launch { preferences.updateAppIconPreference(preference) }
    }

    fun setAppHidden(packageName: String, hidden: Boolean) {
        val current = _settings.value.hiddenApps
        val updated = if (hidden) current + packageName else current - packageName
        _settings.value = _settings.value.copy(hiddenApps = updated)
        viewModelScope.launch { preferences.updateHiddenApps(updated) }
    }

    // ---------------------------------------------------------------------
    // Media
    // ---------------------------------------------------------------------

    fun setMediaShowArtwork(enabled: Boolean) {
        _settings.value = _settings.value.copy(mediaShowArtwork = enabled)
        liveTileManager.recomputeMediaTiles()
        viewModelScope.launch { preferences.updateMediaShowArtwork(enabled) }
    }

    fun setMediaShowControls(enabled: Boolean) {
        _settings.value = _settings.value.copy(mediaShowControls = enabled)
        viewModelScope.launch { preferences.updateMediaShowControls(enabled) }
    }

    fun setMediaShowProgress(enabled: Boolean) {
        _settings.value = _settings.value.copy(mediaShowProgress = enabled)
        viewModelScope.launch { preferences.updateMediaShowProgress(enabled) }
    }

    // ---------------------------------------------------------------------
    // System
    // ---------------------------------------------------------------------

    fun setLauncherOrientation(orientation: LauncherOrientation) {
        _settings.value = _settings.value.copy(launcherOrientation = orientation)
        viewModelScope.launch { preferences.updateLauncherOrientation(orientation) }
    }

    // ---------------------------------------------------------------------
    // Launcher-owned system tiles
    // ---------------------------------------------------------------------

    fun isSystemTilePinned(packageName: String): Boolean =
        _pinnedTiles.value.any { it.packageName == packageName }

    fun pinSystemTile(def: SystemTileDef) {
        if (isSystemTilePinned(def.packageName)) return
        val currentTiles = _pinnedTiles.value
        val totalCols = if (_settings.value.showMoreTiles) 8 else 6
        val (col, row) = GridManager.findFirstAvailablePosition(
            cols = def.defaultSize.cols,
            rows = def.defaultSize.rows,
            tiles = currentTiles,
            totalColumns = totalCols
        )
        val newTile = TileModel(
            id = "system_${def.packageName}_${System.currentTimeMillis()}",
            packageName = def.packageName,
            label = def.label,
            size = def.defaultSize,
            col = col,
            row = row,
            order = (currentTiles.maxOfOrNull { it.order } ?: 0) + 1,
            isAvailable = true
        )
        val updated = currentTiles + newTile
        _pinnedTiles.value = updated
        saveTiles(updated)
    }

    fun unpinSystemTile(packageName: String) {
        val current = _pinnedTiles.value
        val updated = current.filter { it.packageName != packageName }
        if (updated == current) return
        val totalCols = if (_settings.value.showMoreTiles) 8 else 6
        val compacted = GridManager.compactGrid(updated, totalCols)
        _pinnedTiles.value = compacted
        saveTiles(compacted)
    }

    // ---------------------------------------------------------------------
    // Reset
    // ---------------------------------------------------------------------

    fun resetStartLayout() {
        viewModelScope.launch {
            val tiles = createDefaultLayout(repository.installedApps.value)
            _pinnedTiles.value = tiles
            preferences.savePinnedTiles(tiles)
        }
    }

    fun resetAllLauncherSettings() {
        viewModelScope.launch {
            preferences.resetAllSettings()
            val tiles = createDefaultLayout(repository.installedApps.value)
            _pinnedTiles.value = tiles
            preferences.savePinnedTiles(tiles)
        }
    }

    // ---------------------------------------------------------------------
    // Diagnostics
    // ---------------------------------------------------------------------

    fun packageName(): String = getApplication<Application>().packageName

    fun launcherVersionName(): String = BuildConfig.VERSION_NAME

    fun launcherVersionCode(): Long = BuildConfig.VERSION_CODE.toLong()

    fun androidVersion(): String = Build.VERSION.RELEASE ?: ""

    fun deviceModel(): String = "${Build.MANUFACTURER} ${Build.MODEL}"

    fun activeLiveTileProviderCount(): Int = liveTileManager.registry.getAllProviders().size

    fun isLiveTileSchedulerRunning(): Boolean = liveTileManager.isSchedulerRunning()

    private fun AppIconPreference.toRenderMode(): IconRenderMode? = when (this) {
        AppIconPreference.AUTOMATIC -> null
        AppIconPreference.ORIGINAL_ICON -> IconRenderMode.ANDROID_ORIGINAL
        AppIconPreference.MONOCHROME -> IconRenderMode.ANDROID_MONOCHROME
    }

    fun getLiveTileState(packageName: String, activityName: String? = null): com.ab.livetile.model.LiveTileState? {
        if (!_settings.value.liveTilesEnabled) return null
        return liveTileManager.getLiveTileState(packageName, activityName)
    }

    fun toggleShowMediaLiveTiles(enabled: Boolean) {
        _settings.value = _settings.value.copy(showMediaLiveTiles = enabled)
        liveTileManager.recomputeMediaTiles()
        viewModelScope.launch {
            preferences.updateShowMediaLiveTiles(enabled)
        }
    }

    fun openNotificationAccessSettings(context: Context) {
        mediaSessionRepository.openNotificationAccessSettings(context)
    }

    fun pinNowPlayingTile() {
        val def = SystemTiles.ALL.firstOrNull {
            it.packageName == SystemTiles.NOW_PLAYING_PACKAGE
        } ?: return
        pinSystemTile(def)
    }

    fun onStart() {
        mediaSessionRepository.onResume()
        liveTileManager.onStart()
    }

    fun onStop() {
        liveTileManager.onStop()
    }

    override fun onCleared() {
        super.onCleared()
        mediaSessionRepository.onDestroy()
        liveTileManager.onStop()
    }
}
