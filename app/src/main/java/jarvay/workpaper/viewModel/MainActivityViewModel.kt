package jarvay.workpaper.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import jarvay.workpaper.data.preferences.RunningPreferencesRepository
import jarvay.workpaper.data.preferences.SettingsPreferencesRepository
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    settingsPreferencesRepository: SettingsPreferencesRepository,
    val runningPreferencesRepository: RunningPreferencesRepository
) : ViewModel() {
    val settings = settingsPreferencesRepository.settingsPreferencesFlow.asLiveData()

    val runningPreferences = runningPreferencesRepository.runningPreferencesFlow.asLiveData()
}