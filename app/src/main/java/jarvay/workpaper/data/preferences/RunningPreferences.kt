package jarvay.workpaper.data.preferences

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

data class RunningPreferences(
    var currentRuleId: Long,
    var nextRuleId: Long,
    var lastIndex: Int,
    var lastWallpaper: String,
    var running: Boolean,
)

data object RunningPreferencesKeys {
    val CURRENT_RULE_ID = longPreferencesKey("currentRuleId")
    val NEXT_RULE_ID = longPreferencesKey("nextRuleId")
    val LAST_INDEX = intPreferencesKey("lastIndex")
    val LAST_WALLPAPER = stringPreferencesKey("lastWallpaper")
    val RUNNING = booleanPreferencesKey("running")
}