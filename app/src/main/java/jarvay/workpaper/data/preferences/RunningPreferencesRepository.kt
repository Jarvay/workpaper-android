package jarvay.workpaper.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import jarvay.workpaper.others.runningPreferencesFlow
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class RunningPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    val runningPreferencesFlow: Flow<RunningPreferences> = runningPreferencesFlow(dataStore = dataStore)

    suspend fun <T> update(key: Preferences.Key<T>, value: T) {
        dataStore.edit {
            it[key] = value
        }
    }
}