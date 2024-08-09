package jarvay.workpaper.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jarvay.workpaper.data.preferences.RunningPreferencesRepository
import jarvay.workpaper.data.rule.Rule
import jarvay.workpaper.data.rule.RuleAlbums
import jarvay.workpaper.data.rule.RuleRepository
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RuleListViewModel @Inject constructor(
    private val repository: RuleRepository,
    private val runningPreferencesRepository: RunningPreferencesRepository
) : ViewModel() {
    val allRules = repository.allRules.map {
        it.entries.map { e ->
            RuleAlbums(
                rule = e.key,
                albums = e.value
            )
        }
    }.asLiveData()

    val defaultPreferences = runningPreferencesRepository.runningPreferencesFlow.asLiveData()

    fun insert(item: Rule) {
        viewModelScope.launch {
            repository.insert(item)
        }
    }

    fun deleteRule(item: Rule) {
        viewModelScope.launch {
            repository.delete(item)
        }
    }
}