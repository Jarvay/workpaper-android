package jarvay.workpaper.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DefaultPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    val defaultPreferencesFlow: Flow<DefaultPreferences> = dataStore.data.map { preferences ->
        val currentRuleId = preferences[DefaultPreferencesKeys.CURRENT_RULE_ID] ?: -1
        val lastIndex = preferences[DefaultPreferencesKeys.LAST_INDEX] ?: -1
        val lastWallpaper = preferences[DefaultPreferencesKeys.LAST_WALLPAPER] ?: ""
        val nextRuleId = preferences[DefaultPreferencesKeys.NEXT_RULE_ID] ?: -1

        DefaultPreferences(
            currentRuleId = currentRuleId,
            lastIndex = lastIndex,
            lastWallpaper = lastWallpaper,
            nextRuleId = nextRuleId
        )
    }

    suspend fun <T> update(key: Preferences.Key<T>, value: T) {
        dataStore.edit {
            it[key] = value
        }
    }

    suspend fun updateCurrentRuleId(value: Long) {
        dataStore.edit { preferences ->
            preferences[DefaultPreferencesKeys.CURRENT_RULE_ID] = value
        }
    }

    suspend fun updateLastIndex(value: Long) {
        dataStore.edit { preferences ->
            preferences[DefaultPreferencesKeys.CURRENT_RULE_ID] = value
        }
    }

    suspend fun updateLastWallpaper(value: Long) {
        dataStore.edit { preferences ->
            preferences[DefaultPreferencesKeys.CURRENT_RULE_ID] = value
        }
    }
}