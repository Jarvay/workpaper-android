package jarvay.workpaper.viewModel

import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jarvay.workpaper.data.preferences.DEFAULT_SETTINGS
import jarvay.workpaper.data.preferences.SettingsPreferencesRepository
import jarvay.workpaper.data.style.Style
import jarvay.workpaper.data.style.StyleRepository
import jarvay.workpaper.others.STATE_IN_STATED
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StyleListViewModel @Inject constructor(
    private val repository: StyleRepository,
    private val settingsPreferencesRepository: SettingsPreferencesRepository,
) : ViewModel() {
    val allStyles = repository.allStyles.stateIn(
        viewModelScope,
        STATE_IN_STATED,
        emptyList()
    )

    var settings = settingsPreferencesRepository.settingsPreferencesFlow.stateIn(
        viewModelScope,
        STATE_IN_STATED,
        DEFAULT_SETTINGS
    )

    fun delete(item: Style) {
        viewModelScope.launch {
            repository.delete(item)
        }
    }

    fun <T> updateSettingsItem(key: Preferences.Key<T>, value: T) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsPreferencesRepository.update(
                key = key,
                value = value
            )
        }
    }
}