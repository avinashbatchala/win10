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
import androidx.datastore.preferences.preferencesDataStore
import com.ab.model.LauncherSettings
import com.ab.model.TileModel
import com.ab.model.TileSize
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "win10_launcher_prefs")

class LauncherPreferences(private val context: Context) {

    companion object {
        const val CURRENT_SCHEMA_VERSION = 1

        private val KEY_SCHEMA_VERSION = intPreferencesKey("layout_schema_version")
        private val KEY_PINNED_TILES = stringPreferencesKey("pinned_tiles_json")
        private val KEY_ACCENT_COLOR = longPreferencesKey("accent_color_long")
        private val KEY_SHOW_MORE_TILES = booleanPreferencesKey("show_more_tiles")
        private val KEY_DARK_THEME = booleanPreferencesKey("dark_theme")
        private val KEY_TILE_TRANSPARENCY = floatPreferencesKey("tile_transparency")
        private val KEY_BACKGROUND_IMAGE_URI = stringPreferencesKey("background_image_uri")
        private val KEY_FIRST_RUN_DONE = booleanPreferencesKey("first_run_done")
        private val KEY_SHOW_MEDIA_LIVE_TILES = booleanPreferencesKey("show_media_live_tiles")
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
            showMediaLiveTiles = prefs[KEY_SHOW_MEDIA_LIVE_TILES] ?: true
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
        context.dataStore.edit { prefs ->
            prefs[KEY_ACCENT_COLOR] = colorLong
        }
    }

    suspend fun updateTheme(dark: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DARK_THEME] = dark
        }
    }

    suspend fun updateShowMoreTiles(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SHOW_MORE_TILES] = enabled
        }
    }

    suspend fun updateTileTransparency(transparency: Float) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TILE_TRANSPARENCY] = transparency.coerceIn(0.0f, 1.0f)
        }
    }

    suspend fun updateBackgroundImageUri(uriString: String?) {
        context.dataStore.edit { prefs ->
            if (uriString != null) {
                prefs[KEY_BACKGROUND_IMAGE_URI] = uriString
            } else {
                prefs.remove(KEY_BACKGROUND_IMAGE_URI)
            }
        }
    }

    suspend fun updateShowMediaLiveTiles(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SHOW_MEDIA_LIVE_TILES] = enabled
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
                        customIconId = if (obj.has("cid")) obj.getString("cid") else null
                    )
                )
            }
        } catch (_: Exception) {
            return emptyList()
        }
        return list
    }
}
