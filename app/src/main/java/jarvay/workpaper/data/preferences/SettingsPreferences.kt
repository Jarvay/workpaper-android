package jarvay.workpaper.data.preferences

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import jarvay.workpaper.others.GestureEvent

data class SettingsPreferences(
    var startWithPrevRule: Boolean,
    var enableNotification: Boolean,
    var notificationOngoing: Boolean,
    var alsoSetLockWallpaper: Boolean,
    var hideInRecentTask: Boolean,
    var autoCheckUpdate: Boolean,
    var enableDynamicColor: Boolean,
    var disableWhenPlayingAudio: Boolean,
    var useLiveWallpaper: Boolean,
    var liveWallpaperTransition: Boolean,
    var defaultStyleId: Long,
    var forcedUsedRuleId: Long,
    var doubleTapEvent: String,
) {

    override fun equals(other: Any?): Boolean {
        return hashCode() == other.hashCode()
    }

    override fun hashCode(): Int {
        var result = startWithPrevRule.hashCode()
        result = 31 * result + enableNotification.hashCode()
        result = 31 * result + notificationOngoing.hashCode()
        result = 31 * result + alsoSetLockWallpaper.hashCode()
        result = 31 * result + hideInRecentTask.hashCode()
        result = 31 * result + autoCheckUpdate.hashCode()
        result = 31 * result + enableDynamicColor.hashCode()
        result = 31 * result + disableWhenPlayingAudio.hashCode()
        result = 31 * result + useLiveWallpaper.hashCode()
        result = 31 * result + liveWallpaperTransition.hashCode()
        result = 31 * result + defaultStyleId.hashCode()
        result = 31 * result + forcedUsedRuleId.hashCode()
        result = 31 * result + doubleTapEvent.hashCode()
        return result
    }
}

data object SettingsPreferencesKeys {
    val START_WITH_PREV_RULE = booleanPreferencesKey("startWithPrevRule")
    val ENABLE_NOTIFICATION = booleanPreferencesKey("enableNotification")
    val NOTIFICATION_ONGOING = booleanPreferencesKey("notificationOngoing")
    val ALSO_SET_LOCK_WALLPAPER = booleanPreferencesKey("alsoSetLockWallpaper")
    val HIDE_IN_RECENT_TASK = booleanPreferencesKey("hideInRecentTask")
    val ENABLE_DYNAMIC_COLOR = booleanPreferencesKey("enableDynamicColor")
    val AUTO_CHECK_UPDATE = booleanPreferencesKey("autoCheckUpdate")
    val DISABLE_WHEN_PLAYING_AUDIO = booleanPreferencesKey("disableWhenPlayingAudio")
    val USE_LIVE_WALLPAPER = booleanPreferencesKey("useLiveWallpaper")
    val LIVE_WALLPAPER_TRANSITION = booleanPreferencesKey("liveWallpaperTransition")
    val DEFAULT_STYLE_ID = longPreferencesKey("defaultStyleId")
    val FORCED_USED_RULE_ID = longPreferencesKey("forcedUsedRuleId")
    val DOUBLE_TAP_EVENT = stringPreferencesKey("doubleTapEvent")
}

val DEFAULT_SETTINGS =
    SettingsPreferences(
        startWithPrevRule = true,
        enableNotification = false,
        notificationOngoing = false,
        alsoSetLockWallpaper = false,
        hideInRecentTask = false,
        autoCheckUpdate = false,
        enableDynamicColor = false,
        disableWhenPlayingAudio = false,
        useLiveWallpaper = false,
        liveWallpaperTransition = false,
        defaultStyleId = -1,
        forcedUsedRuleId = -1,
        doubleTapEvent = GestureEvent.CHANGE_WALLPAPER.name
    )