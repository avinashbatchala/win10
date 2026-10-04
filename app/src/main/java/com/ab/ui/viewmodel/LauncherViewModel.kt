package com.ab.ui.viewmodel

import android.app.Application
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.widget.Toast
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ab.data.InstalledAppRepository
import com.ab.data.LauncherPreferences
import com.ab.model.AppInfo
import com.ab.model.LauncherSettings
import com.ab.model.TileModel
import com.ab.model.TileSize
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

    private val repository = InstalledAppRepository(application, viewModelScope)
    private val preferences = LauncherPreferences(application)

    val installedApps: StateFlow<List<AppInfo>> = repository.installedApps
    val isAppsLoaded: StateFlow<Boolean> = repository.isLoaded

    private val _pinnedTiles = MutableStateFlow<List<TileModel>>(emptyList())
    val pinnedTiles: StateFlow<List<TileModel>> = _pinnedTiles.asStateFlow()

    private val _settings = MutableStateFlow(LauncherSettings())
    val settings: StateFlow<LauncherSettings> = _settings.asStateFlow()

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
                _settings.value = newSettings.copy(
                    isDefaultLauncher = checkIsDefaultLauncher()
                )
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
                        // Mark availability for pinned apps
                        val installedPkgs = apps.map { it.packageName }.toSet()
                        val updated = _pinnedTiles.value.map { tile ->
                            tile.copy(isAvailable = installedPkgs.contains(tile.packageName))
                        }
                        if (updated != _pinnedTiles.value) {
                            _pinnedTiles.value = updated
                        }
                    }
                }
            }
        }
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
        val compacted = GridManager.compactGrid(_pinnedTiles.value, totalCols)
        _pinnedTiles.value = compacted
        saveTiles(compacted)
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
