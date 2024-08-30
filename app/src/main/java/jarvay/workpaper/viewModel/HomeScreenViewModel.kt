package jarvay.workpaper.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jarvay.workpaper.Workpaper
import jarvay.workpaper.data.preferences.RunningPreferencesKeys
import jarvay.workpaper.data.preferences.RunningPreferencesRepository
import jarvay.workpaper.others.STATE_IN_STATED
import jarvay.workpaper.request.RetrofitClient
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    val runningPreferencesRepository: RunningPreferencesRepository,
    private val workpaper: Workpaper,
) : ViewModel() {
    @Inject
    lateinit var retrofitClient: RetrofitClient

    val runningPreferences = runningPreferencesRepository.runningPreferencesFlow.stateIn(
        viewModelScope,
        STATE_IN_STATED,
        null
    )

    fun start() {
        viewModelScope.launch {
            runningPreferencesRepository.apply {
                update(RunningPreferencesKeys.RUNNING, true)
            }
        }
        workpaper.start()
    }

    fun stop() {
        viewModelScope.launch {
            workpaper.stop()

            runningPreferencesRepository.apply {
                update(RunningPreferencesKeys.RUNNING, false)
            }
        }
    }
}