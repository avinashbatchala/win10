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
import android.os.Process
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ab.data.InstalledAppRepository
import com.ab.data.LauncherPreferences
import com.ab.model.AppInfo
import com.ab.model.LauncherSettings
import com.ab.model.TileModel
import com.ab.model.TileSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
    }

    private val repository = InstalledAppRepository(application, viewModelScope)
    private val preferences = LauncherPreferences(application)

    val installedApps: StateFlow<List<AppInfo>> = repository.installedApps
    val isAppsLoaded: StateFlow<Boolean> = repository.isLoaded

    private val _pinnedTiles = MutableStateFlow<List<TileModel>>(emptyList())
    val pinnedTiles: StateFlow<List<TileModel>> = _pinnedTiles.asStateFlow()

    private val _settings = MutableStateFlow(LauncherSettings())
    val settings: StateFlow<LauncherSettings> = _settings.asStateFlow()

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

    private val _requestScrollToStart = MutableStateFlow(false)
    val requestScrollToStart: StateFlow<Boolean> = _requestScrollToStart.asStateFlow()

    val filteredApps: StateFlow<List<AppInfo>> = combine(
        installedApps,
        searchQuery
    ) { apps, query ->
        if (query.isBlank()) {
            apps
        } else {
            val q = query.trim().lowercase()
            apps.filter { it.label.lowercase().contains(q) }
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
                if (newSettings.backgroundImageUri != prevUri || (_wallpaperBitmap.value == null && newSettings.backgroundImageUri != null)) {
                    loadWallpaperAsync(newSettings.backgroundImageUri)
                }
            }
        }

        // Initialize pinned tiles and synchronize with installed apps
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
                        val currentTiles = _pinnedTiles.value
                        val hasRemovedTiles = currentTiles.any { !installedPkgs.contains(it.packageName) }

                        if (hasRemovedTiles) {
                            Log.d(TAG_PACKAGE, "Detected package removal. Purging uninstalled tiles and updating Start layout.")
                            val remainingTiles = currentTiles.filter { installedPkgs.contains(it.packageName) }
                            val totalCols = if (_settings.value.showMoreTiles) 8 else 6
                            val compacted = GridManager.compactGrid(remainingTiles, totalCols)
                            _pinnedTiles.value = compacted
                            saveTiles(compacted)
                        } else {
                            val updated = currentTiles.map { tile ->
                                tile.copy(isAvailable = installedPkgs.contains(tile.packageName))
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
            Log.d(TAG_BACK, "Back consumed: closing settings sheet.")
            _isSettingsOpen.value = false
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
        return repository.resolveLauncherIcon(packageName, activityName, targetSizePx, iconModeOverride, customIconId)
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

    fun pinApp(app: AppInfo, size: TileSize = TileSize.MEDIUM) {
        val current = _pinnedTiles.value
        val totalCols = if (_settings.value.showMoreTiles) 8 else 6
        val pos = GridManager.findFirstAvailablePosition(size.cols, size.rows, current, totalCols)
        val maxOrder = (current.maxOfOrNull { it.order } ?: 0) + 1
        val newTile = TileModel(
            id = UUID.randomUUID().toString(),
            packageName = app.packageName,
            activityName = app.activityName,
            label = app.label,
            size = size,
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

    fun openSettings() {
        _isSettingsOpen.value = true
    }

    fun closeSettings() {
        _isSettingsOpen.value = false
    }
}
