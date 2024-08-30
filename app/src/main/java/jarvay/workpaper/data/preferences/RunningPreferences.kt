package jarvay.workpaper.data.preferences

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

data class RunningPreferences(
    var lastIndex: Int,
    var lastWallpaper: String,
    var running: Boolean,
)

data object RunningPreferencesKeys {
    val LAST_INDEX = intPreferencesKey("lastIndex")
    val LAST_WALLPAPER = stringPreferencesKey("lastWallpaper")
    val RUNNING = booleanPreferencesKey("running")
}