package jarvay.workpaper.viewModel

import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jarvay.workpaper.data.preferences.DEFAULT_SETTINGS
import jarvay.workpaper.data.preferences.SettingsPreferencesRepository
import jarvay.workpaper.others.STATE_IN_STATED
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsPreferencesRepository
) : ViewModel() {
    var settings = repository.settingsPreferencesFlow.stateIn(
        viewModelScope,
        STATE_IN_STATED,
        DEFAULT_SETTINGS
    )

    fun <T> update(key: Preferences.Key<T>, value: T) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.update(
                key = key,
                value = value
            )
        }
    }
}