package com.ab.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ab.model.AppIconPreference
import com.ab.model.BackgroundStyle
import com.ab.model.LauncherOrientation
import com.ab.model.LauncherSettings
import com.ab.model.LiveTileAnimationFrequency
import com.ab.model.TileModel
import com.ab.model.TileSize
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "win10_launcher_prefs")

class LauncherPreferences(private val context: Context) {

    companion object {
        const val CURRENT_SCHEMA_VERSION = 3

        private val KEY_SCHEMA_VERSION = intPreferencesKey("layout_schema_version")
        private val KEY_PINNED_TILES = stringPreferencesKey("pinned_tiles_json")
        private val KEY_ACCENT_COLOR = longPreferencesKey("accent_color_long")
        private val KEY_SHOW_MORE_TILES = booleanPreferencesKey("show_more_tiles")
        private val KEY_DARK_THEME = booleanPreferencesKey("dark_theme")
        private val KEY_TILE_TRANSPARENCY = floatPreferencesKey("tile_transparency")
        private val KEY_BACKGROUND_IMAGE_URI = stringPreferencesKey("background_image_uri")
        private val KEY_BACKGROUND_STYLE = stringPreferencesKey("background_style")
        private val KEY_FIRST_RUN_DONE = booleanPreferencesKey("first_run_done")
        private val KEY_SHOW_MEDIA_LIVE_TILES = booleanPreferencesKey("show_media_live_tiles")
        private val KEY_LIVE_TILES_ENABLED = booleanPreferencesKey("live_tiles_enabled")
        private val KEY_ANIMATE_LIVE_TILES = booleanPreferencesKey("animate_live_tiles")
        private val KEY_LIVE_TILE_FREQUENCY = stringPreferencesKey("live_tile_animation_frequency")
        private val KEY_PAUSE_LIVE_TILES_HIDDEN = booleanPreferencesKey("pause_live_tiles_hidden")
        private val KEY_PAUSE_LIVE_TILES_BATTERY = booleanPreferencesKey("pause_live_tiles_battery_saver")
        private val KEY_DEFAULT_TILE_SIZE = stringPreferencesKey("default_tile_size")
        private val KEY_SHOW_APP_NAMES = booleanPreferencesKey("show_app_names")
        private val KEY_APP_ICON_PREFERENCE = stringPreferencesKey("app_icon_preference")
        private val KEY_SHOW_JUMP_LIST = booleanPreferencesKey("show_alphabet_jump_list")
        private val KEY_HIDDEN_APPS = stringSetPreferencesKey("hidden_apps")
        private val KEY_MEDIA_SHOW_ARTWORK = booleanPreferencesKey("media_show_artwork")
        private val KEY_MEDIA_SHOW_CONTROLS = booleanPreferencesKey("media_show_controls")
        private val KEY_MEDIA_SHOW_PROGRESS = booleanPreferencesKey("media_show_progress")
        private val KEY_LAUNCHER_ORIENTATION = stringPreferencesKey("launcher_orientation")
    }

    val pinnedTilesFlow: Flow<List<TileModel>?> = context.dataStore.data.map { prefs ->
        val jsonStr = prefs[KEY_PINNED_TILES]
        if (jsonStr.isNullOrEmpty()) null else parseTilesJson(jsonStr)
    }

    val settingsFlow: Flow<LauncherSettings> = context.dataStore.data.map { prefs ->
        LauncherSettings(
            accentColor = prefs[KEY_ACCENT_COLOR] ?: 0xFF0078D7L,
            darkTheme = prefs[KEY_DARK_THEME] ?: true,
            showMoreTiles = prefs[KEY_SHOW_MORE_TILES] ?: false,
            tileTransparency = prefs[KEY_TILE_TRANSPARENCY] ?: 0.0f,
            backgroundImageUri = prefs[KEY_BACKGROUND_IMAGE_URI],
            backgroundStyle = enumOrDefault(
                prefs[KEY_BACKGROUND_STYLE],
                BackgroundStyle.FULL_SCREEN
            ),
            showMediaLiveTiles = prefs[KEY_SHOW_MEDIA_LIVE_TILES] ?: true,
            liveTilesEnabled = prefs[KEY_LIVE_TILES_ENABLED] ?: true,
            animateLiveTiles = prefs[KEY_ANIMATE_LIVE_TILES] ?: true,
            liveTileAnimationFrequency = enumOrDefault(
                prefs[KEY_LIVE_TILE_FREQUENCY],
                LiveTileAnimationFrequency.NORMAL
            ),
            pauseLiveTilesWhenHidden = prefs[KEY_PAUSE_LIVE_TILES_HIDDEN] ?: true,
            pauseLiveTilesInBatterySaver = prefs[KEY_PAUSE_LIVE_TILES_BATTERY] ?: true,
            defaultTileSize = enumOrDefault(prefs[KEY_DEFAULT_TILE_SIZE], TileSize.MEDIUM),
            showAppNames = prefs[KEY_SHOW_APP_NAMES] ?: true,
            appIconPreference = enumOrDefault(
                prefs[KEY_APP_ICON_PREFERENCE],
                AppIconPreference.AUTOMATIC
            ),
            showAlphabetJumpList = prefs[KEY_SHOW_JUMP_LIST] ?: true,
            hiddenApps = prefs[KEY_HIDDEN_APPS] ?: emptySet(),
            mediaShowArtwork = prefs[KEY_MEDIA_SHOW_ARTWORK] ?: true,
            mediaShowControls = prefs[KEY_MEDIA_SHOW_CONTROLS] ?: true,
            mediaShowProgress = prefs[KEY_MEDIA_SHOW_PROGRESS] ?: true,
            launcherOrientation = enumOrDefault(
                prefs[KEY_LAUNCHER_ORIENTATION],
                LauncherOrientation.PORTRAIT
            )
        )
    }

    val isFirstRunFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        !(prefs[KEY_FIRST_RUN_DONE] ?: false)
    }

    suspend fun savePinnedTiles(tiles: List<TileModel>) {
        val jsonStr = serializeTilesJson(tiles)
        context.dataStore.edit { prefs ->
            prefs[KEY_SCHEMA_VERSION] = CURRENT_SCHEMA_VERSION
            prefs[KEY_PINNED_TILES] = jsonStr
            prefs[KEY_FIRST_RUN_DONE] = true
        }
    }

    suspend fun updateAccentColor(colorLong: Long) {
        context.dataStore.edit { prefs -> prefs[KEY_ACCENT_COLOR] = colorLong }
    }

    suspend fun updateTheme(dark: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_DARK_THEME] = dark }
    }

    suspend fun updateShowMoreTiles(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_SHOW_MORE_TILES] = enabled }
    }

    suspend fun updateTileTransparency(transparency: Float) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TILE_TRANSPARENCY] = transparency.coerceIn(0.0f, 1.0f)
        }
    }

    suspend fun updateBackgroundImageUri(uriString: String?) {
        context.dataStore.edit { prefs ->
            if (uriString != null) prefs[KEY_BACKGROUND_IMAGE_URI] = uriString
            else prefs.remove(KEY_BACKGROUND_IMAGE_URI)
        }
    }

    suspend fun updateBackgroundStyle(style: BackgroundStyle) {
        context.dataStore.edit { prefs -> prefs[KEY_BACKGROUND_STYLE] = style.name }
    }

    suspend fun updateShowMediaLiveTiles(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_SHOW_MEDIA_LIVE_TILES] = enabled }
    }

    suspend fun updateLiveTilesEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_LIVE_TILES_ENABLED] = enabled }
    }

    suspend fun updateAnimateLiveTiles(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_ANIMATE_LIVE_TILES] = enabled }
    }

    suspend fun updateLiveTileAnimationFrequency(frequency: LiveTileAnimationFrequency) {
        context.dataStore.edit { prefs -> prefs[KEY_LIVE_TILE_FREQUENCY] = frequency.name }
    }

    suspend fun updatePauseLiveTilesWhenHidden(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_PAUSE_LIVE_TILES_HIDDEN] = enabled }
    }

    suspend fun updatePauseLiveTilesInBatterySaver(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_PAUSE_LIVE_TILES_BATTERY] = enabled }
    }

    suspend fun updateDefaultTileSize(size: TileSize) {
        context.dataStore.edit { prefs -> prefs[KEY_DEFAULT_TILE_SIZE] = size.name }
    }

    suspend fun updateShowAppNames(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_SHOW_APP_NAMES] = enabled }
    }

    suspend fun updateAppIconPreference(preference: AppIconPreference) {
        context.dataStore.edit { prefs -> prefs[KEY_APP_ICON_PREFERENCE] = preference.name }
    }

    suspend fun updateShowAlphabetJumpList(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_SHOW_JUMP_LIST] = enabled }
    }

    suspend fun updateHiddenApps(packages: Set<String>) {
        context.dataStore.edit { prefs -> prefs[KEY_HIDDEN_APPS] = packages }
    }

    suspend fun updateMediaShowArtwork(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_MEDIA_SHOW_ARTWORK] = enabled }
    }

    suspend fun updateMediaShowControls(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_MEDIA_SHOW_CONTROLS] = enabled }
    }

    suspend fun updateMediaShowProgress(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_MEDIA_SHOW_PROGRESS] = enabled }
    }

    suspend fun updateLauncherOrientation(orientation: LauncherOrientation) {
        context.dataStore.edit { prefs -> prefs[KEY_LAUNCHER_ORIENTATION] = orientation.name }
    }

    /** Clears every persisted launcher preference, restoring factory defaults. */
    suspend fun resetAllSettings() {
        context.dataStore.edit { prefs -> prefs.clear() }
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(raw: String?, default: T): T {
        if (raw == null) return default
        return try {
            enumValueOf<T>(raw)
        } catch (_: Exception) {
            default
        }
    }

    private fun serializeTilesJson(tiles: List<TileModel>): String {
        val root = JSONObject()
        root.put("schemaVersion", CURRENT_SCHEMA_VERSION)

        val array = JSONArray()
        tiles.forEachIndexed { index, tile ->
            val obj = JSONObject().apply {
                put("id", tile.id)
                put("pkg", tile.packageName)
                put("act", tile.activityName)
                put("lbl", tile.label)
                put("size", tile.size.name)
                put("col", tile.col)
                put("row", tile.row)
                put("order", if (tile.order != 0) tile.order else index)
                if (tile.customColor != null) put("color", tile.customColor)
                if (tile.customLabel != null) put("clbl", tile.customLabel)
                if (tile.iconMode != null) put("imode", tile.iconMode)
                if (tile.customIconId != null) put("cid", tile.customIconId)
                if (!tile.isAvailable) put("avail", false)
                if (tile.weatherLat != null && tile.weatherLon != null) {
                    put("wlat", tile.weatherLat)
                    put("wlon", tile.weatherLon)
                }
                if (tile.weatherTimezone != null) put("wtz", tile.weatherTimezone)
                if (tile.weatherLocationId != null) put("wlid", tile.weatherLocationId)
            }
            array.put(obj)
        }
        root.put("tiles", array)
        return root.toString()
    }

    private fun parseTilesJson(jsonStr: String): List<TileModel> {
        val list = mutableListOf<TileModel>()
        try {
            val (schemaVersion, array) = if (jsonStr.trim().startsWith("{")) {
                val root = JSONObject(jsonStr)
                val version = root.optInt("schemaVersion", 1)
                val arr = root.optJSONArray("tiles") ?: JSONArray()
                Pair(version, arr)
            } else {
                Pair(1, JSONArray(jsonStr))
            }

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val sizeStr = obj.optString("size", "MEDIUM")
                val size = try {
                    TileSize.valueOf(sizeStr)
                } catch (_: Exception) {
                    TileSize.MEDIUM
                }
                list.add(
                    TileModel(
                        id = obj.getString("id"),
                        packageName = obj.getString("pkg"),
                        activityName = obj.optString("act", ""),
                        label = obj.getString("lbl"),
                        size = size,
                        col = obj.getInt("col"),
                        row = obj.getInt("row"),
                        order = obj.optInt("order", i),
                        customColor = if (obj.has("color")) obj.getLong("color") else null,
                        customLabel = if (obj.has("clbl")) obj.getString("clbl") else null,
                        iconMode = if (obj.has("imode")) obj.getString("imode") else null,
                        customIconId = if (obj.has("cid")) obj.getString("cid") else null,
                        isAvailable = obj.optBoolean("avail", true),
                        weatherLat = if (obj.has("wlat")) obj.optDouble("wlat") else null,
                        weatherLon = if (obj.has("wlon")) obj.optDouble("wlon") else null,
                        weatherTimezone = if (obj.has("wtz")) obj.getString("wtz") else null,
                        weatherLocationId = if (obj.has("wlid")) obj.getString("wlid") else null
                    )
                )
            }
        } catch (_: Exception) {
            return emptyList()
        }
        return list
    }
}
