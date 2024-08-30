package jarvay.workpaper.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SettingsPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    val settingsPreferencesFlow: Flow<SettingsPreferences> =
        dataStore.data.map { preferences ->
            SettingsPreferences(
                startWithPrevRule = preferences[SettingsPreferencesKeys.START_WITH_PREV_RULE]
                    ?: DEFAULT_SETTINGS.startWithPrevRule,
                enableNotification = preferences[SettingsPreferencesKeys.ENABLE_NOTIFICATION]
                    ?: DEFAULT_SETTINGS.enableNotification,
                notificationOngoing = preferences[SettingsPreferencesKeys.NOTIFICATION_ONGOING]
                    ?: DEFAULT_SETTINGS.notificationOngoing,
                alsoSetLockWallpaper = preferences[SettingsPreferencesKeys.ALSO_SET_LOCK_WALLPAPER]
                    ?: DEFAULT_SETTINGS.alsoSetLockWallpaper,
                hideInRecentTask = preferences[SettingsPreferencesKeys.HIDE_IN_RECENT_TASK]
                    ?: DEFAULT_SETTINGS.hideInRecentTask,
                autoCheckUpdate = preferences[SettingsPreferencesKeys.AUTO_CHECK_UPDATE]
                    ?: DEFAULT_SETTINGS.autoCheckUpdate,
                enableDynamicColor = preferences[SettingsPreferencesKeys.ENABLE_DYNAMIC_COLOR]
                    ?: DEFAULT_SETTINGS.enableDynamicColor
            )
        }.distinctUntilChanged { old, new ->
            old == new
        }

    suspend fun <T> update(key: Preferences.Key<T>, value: T) {
        dataStore.edit {
            it[key] = value
        }
    }
}