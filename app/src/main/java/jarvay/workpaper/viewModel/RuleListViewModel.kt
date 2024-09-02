package jarvay.workpaper.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jarvay.workpaper.Workpaper
import jarvay.workpaper.data.preferences.RunningPreferencesRepository
import jarvay.workpaper.data.rule.Rule
import jarvay.workpaper.data.rule.RuleAlbums
import jarvay.workpaper.data.rule.RuleRepository
import jarvay.workpaper.others.STATE_IN_STATED
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RuleListViewModel @Inject constructor(
    private val repository: RuleRepository,
    private val runningPreferencesRepository: RunningPreferencesRepository,
    val workpaper: Workpaper
) : ViewModel() {
    val allRules = repository.allRules.map {
        it.entries.map { e ->
            RuleAlbums(
                rule = e.key,
                albums = e.value
            )
        }
    }.stateIn(
        viewModelScope,
        STATE_IN_STATED,
        emptyList()
    )

    val runningPreferences = runningPreferencesRepository.runningPreferencesFlow.stateIn(
        viewModelScope,
        STATE_IN_STATED,
        null
    )

    val currentRuleAlbums = workpaper.currentRuleAlbums.stateIn(
        viewModelScope,
        STATE_IN_STATED,
        null
    )
    val nextRuleAlbums = workpaper.nextRuleAlbums.stateIn(
        viewModelScope,
        STATE_IN_STATED,
        null
    )

    fun deleteRule(item: Rule) {
        viewModelScope.launch {
            repository.delete(item)
        }
    }
}