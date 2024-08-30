package jarvay.workpaper.viewModel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jarvay.workpaper.data.preferences.RunningPreferencesRepository
import jarvay.workpaper.others.STATE_IN_STATED
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject


@HiltViewModel
class WorkpaperViewModel @Inject constructor(
    val runningPreferencesRepository: RunningPreferencesRepository,
) : ViewModel() {
    @ApplicationContext
    lateinit var context: Context;

    val runningPreferences = runningPreferencesRepository.runningPreferencesFlow.stateIn(
        viewModelScope,
        STATE_IN_STATED,
        null
    )
}