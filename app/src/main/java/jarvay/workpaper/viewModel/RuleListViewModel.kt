package jarvay.workpaper.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jarvay.workpaper.data.preferences.DefaultPreferencesRepository
import jarvay.workpaper.data.rule.Rule
import jarvay.workpaper.data.rule.RuleRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RuleListViewModel @Inject constructor(
    private val repository: RuleRepository,
    private val defaultPreferencesRepository: DefaultPreferencesRepository
) : ViewModel() {
    val allRules = repository.allRules.asLiveData()

    val defaultPreferences = defaultPreferencesRepository.defaultPreferencesFlow.asLiveData()

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