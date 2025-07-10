package jarvay.workpaper.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jarvay.workpaper.Workpaper
import jarvay.workpaper.data.preferences.RunningPreferencesRepository
import jarvay.workpaper.others.STATE_IN_STATED
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject


@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    val runningPreferencesRepository: RunningPreferencesRepository,
    private val workpaper: Workpaper,
) : ViewModel() {
    val runningPreferences = runningPreferencesRepository.runningPreferencesFlow.stateIn(
        viewModelScope,
        STATE_IN_STATED,
        null
    )

    suspend fun start() = workpaper.start()

    suspend fun stop() = workpaper.stop()
}