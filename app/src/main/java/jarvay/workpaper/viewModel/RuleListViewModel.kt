package jarvay.workpaper.viewModel

import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jarvay.workpaper.Workpaper
import jarvay.workpaper.data.preferences.DEFAULT_SETTINGS
import jarvay.workpaper.data.preferences.RunningPreferencesRepository
import jarvay.workpaper.data.preferences.SettingsPreferencesRepository
import jarvay.workpaper.data.rule.Rule
import jarvay.workpaper.data.rule.RuleAlbums
import jarvay.workpaper.data.rule.RuleRepository
import jarvay.workpaper.others.STATE_IN_STATED
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RuleListViewModel @Inject constructor(
    private val repository: RuleRepository,
    private val settingRepository: SettingsPreferencesRepository,
    val runningPreferencesRepository: RunningPreferencesRepository,
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

    var settings = settingRepository.settingsPreferencesFlow.stateIn(
        viewModelScope,
        STATE_IN_STATED,
        DEFAULT_SETTINGS
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

    fun <T> updateSettingsItem(key: Preferences.Key<T>, value: T) {
        viewModelScope.launch(Dispatchers.IO) {
            settingRepository.update(
                key = key,
                value = value
            )
        }
    }
}