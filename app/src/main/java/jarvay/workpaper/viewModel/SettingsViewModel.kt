package jarvay.workpaper.viewModel

import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import jarvay.workpaper.data.preferences.SettingsPreferencesRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsPreferencesRepository
) : ViewModel() {
    private val gson = Gson()

    var settings = repository.settingsPreferencesFlow.asLiveData()

    fun <T> update(key: Preferences.Key<T>, value: T) {
        viewModelScope.launch {
            repository.update(
                key = key,
                value = value
            )
        }
    }
}