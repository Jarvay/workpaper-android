package jarvay.workpaper.data.preferences

import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

data class DefaultPreferences(
    var currentRuleId: Long,
    var nextRuleId: Long,
    var lastIndex: Int,
    var lastWallpaper: String,
)

data object DefaultPreferencesKeys {
    val CURRENT_RULE_ID = longPreferencesKey("currentRuleId")
    val NEXT_RULE_ID = longPreferencesKey("nextRuleId")
    val LAST_INDEX = intPreferencesKey("lastIndex")
    val LAST_WALLPAPER = stringPreferencesKey("lastWallpaper")

    val SETTINGS = stringPreferencesKey("settings")
}