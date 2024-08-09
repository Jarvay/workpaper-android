package jarvay.workpaper.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SettingsPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    val settingsPreferencesFlow: Flow<SettingsPreferences> = dataStore.data.map { preferences ->
        val startWithPrevRule = preferences[SettingsPreferencesKeys.START_WITH_PREV_RULE]
            ?: DEFAULT_SETTINGS.startWithPrevRule
        val enableNotification = preferences[SettingsPreferencesKeys.ENABLE_NOTIFICATION]
            ?: DEFAULT_SETTINGS.enableNotification
        val alsoSetLockWallpaper = preferences[SettingsPreferencesKeys.ALSO_SET_LOCK_WALLPAPER]
            ?: DEFAULT_SETTINGS.alsoSetLockWallpaper
        val hideInRecentTask = preferences[SettingsPreferencesKeys.HIDE_IN_RECENT_TASK]
            ?: DEFAULT_SETTINGS.hideInRecentTask
        SettingsPreferences(
            startWithPrevRule = startWithPrevRule,
            enableNotification = enableNotification,
            alsoSetLockWallpaper = alsoSetLockWallpaper,
            hideInRecentTask = hideInRecentTask,
        )
    }

    suspend fun <T> update(key: Preferences.Key<T>, value: T) {
        dataStore.edit {
            it[key] = value
        }
    }
}