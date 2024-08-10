package jarvay.workpaper.viewModel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jarvay.workpaper.data.preferences.RunningPreferencesRepository
import javax.inject.Inject


@HiltViewModel
class WorkpaperViewModel @Inject constructor(
    val runningPreferencesRepository: RunningPreferencesRepository,
) : ViewModel() {
    @ApplicationContext
    lateinit var context: Context;

    val runningPreferences = runningPreferencesRepository.runningPreferencesFlow.asLiveData()
}