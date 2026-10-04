package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.LauncherSettings
import com.example.model.TileModel
import com.example.model.TileSize
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "win10_launcher_prefs")

class LauncherPreferences(private val context: Context) {

    companion object {
        private val KEY_PINNED_TILES = stringPreferencesKey("pinned_tiles_json")
        private val KEY_ACCENT_COLOR = longPreferencesKey("accent_color_long")
        private val KEY_SHOW_MORE_TILES = booleanPreferencesKey("show_more_tiles")
        private val KEY_FIRST_RUN_DONE = booleanPreferencesKey("first_run_done")
    }

    val pinnedTilesFlow: Flow<List<TileModel>?> = context.dataStore.data.map { prefs ->
        val jsonStr = prefs[KEY_PINNED_TILES]
        if (jsonStr.isNullOrEmpty()) null else parseTilesJson(jsonStr)
    }

    val settingsFlow: Flow<LauncherSettings> = context.dataStore.data.map { prefs ->
        LauncherSettings(
            accentColor = prefs[KEY_ACCENT_COLOR] ?: 0xFF0078D7L,
            darkTheme = true,
            showMoreTiles = prefs[KEY_SHOW_MORE_TILES] ?: false
        )
    }

    val isFirstRunFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        !(prefs[KEY_FIRST_RUN_DONE] ?: false)
    }

    suspend fun savePinnedTiles(tiles: List<TileModel>) {
        val jsonStr = serializeTilesJson(tiles)
        context.dataStore.edit { prefs ->
            prefs[KEY_PINNED_TILES] = jsonStr
            prefs[KEY_FIRST_RUN_DONE] = true
        }
    }

    suspend fun updateAccentColor(colorLong: Long) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ACCENT_COLOR] = colorLong
        }
    }

    suspend fun updateShowMoreTiles(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SHOW_MORE_TILES] = enabled
        }
    }

    private fun serializeTilesJson(tiles: List<TileModel>): String {
        val array = JSONArray()
        for (tile in tiles) {
            val obj = JSONObject().apply {
                put("id", tile.id)
                put("pkg", tile.packageName)
                put("act", tile.activityName)
                put("lbl", tile.label)
                put("size", tile.size.name)
                put("col", tile.col)
                put("row", tile.row)
                if (tile.customColor != null) put("color", tile.customColor)
                if (tile.customLabel != null) put("clbl", tile.customLabel)
            }
            array.put(obj)
        }
        return array.toString()
    }

    private fun parseTilesJson(jsonStr: String): List<TileModel> {
        val list = mutableListOf<TileModel>()
        try {
            val array = JSONArray(jsonStr)
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
                        customColor = if (obj.has("color")) obj.getLong("color") else null,
                        customLabel = if (obj.has("clbl")) obj.getString("clbl") else null
                    )
                )
            }
        } catch (_: Exception) {
            return emptyList()
        }
        return list
    }
}
