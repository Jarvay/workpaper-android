package jarvay.workpaper.data.preferences

import androidx.datastore.preferences.core.booleanPreferencesKey

data class SettingsPreferences(
    var startWithPrevRule: Boolean,
    var enableNotification: Boolean,
    var alsoSetLockWallpaper: Boolean,
    var hideInRecentTask: Boolean,
    var autoCheckUpdate: Boolean,
) {

    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }

    override fun hashCode(): Int {
        var result = startWithPrevRule.hashCode()
        result = 31 * result + enableNotification.hashCode()
        result = 31 * result + alsoSetLockWallpaper.hashCode()
        return result
    }
}

data object SettingsPreferencesKeys {
    val START_WITH_PREV_RULE = booleanPreferencesKey("startWithPrevRule")
    val ENABLE_NOTIFICATION = booleanPreferencesKey("enableNotification")
    val ALSO_SET_LOCK_WALLPAPER = booleanPreferencesKey("alsoSetLockWallpaper")
    val HIDE_IN_RECENT_TASK = booleanPreferencesKey("hideInRecentTask")
    val AUTO_CHECK_UPDATE = booleanPreferencesKey("autoCheckUpdate")
}

val DEFAULT_SETTINGS =
    SettingsPreferences(
        startWithPrevRule = true,
        enableNotification = false,
        alsoSetLockWallpaper = false,
        hideInRecentTask = false,
        autoCheckUpdate = false,
    )