package jarvay.workpaper.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

const val DEFAULT_PREFERENCES_DATA_STORE_NAME = "defaultPreferencesDataStore"
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = DEFAULT_PREFERENCES_DATA_STORE_NAME)
