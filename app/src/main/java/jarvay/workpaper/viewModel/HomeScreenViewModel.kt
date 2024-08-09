package jarvay.workpaper.viewModel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jarvay.workpaper.Workpaper
import jarvay.workpaper.data.preferences.RunningPreferencesKeys
import jarvay.workpaper.data.preferences.RunningPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    val runningPreferencesRepository: RunningPreferencesRepository,
    private val workpaper: Workpaper
) : ViewModel() {
    @ApplicationContext
    lateinit var context: Context;

    val runningPreferences = runningPreferencesRepository.runningPreferencesFlow.asLiveData()

    fun start(scope: CoroutineScope) {
        scope.launch {
            runningPreferencesRepository.apply {
                update(RunningPreferencesKeys.RUNNING, true)
            }
        }
        workpaper.start(scope)
    }

    fun stop(scope: CoroutineScope) {
        workpaper.stop(
            scope,
            runningPreferencesRepository
        )
        scope.launch {
            runningPreferencesRepository.apply {
                update(RunningPreferencesKeys.RUNNING, false)
            }
        }
    }
}