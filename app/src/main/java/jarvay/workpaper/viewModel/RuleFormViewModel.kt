package jarvay.workpaper.viewModel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jarvay.workpaper.data.rule.Rule
import jarvay.workpaper.data.rule.RuleRepository
import jarvay.workpaper.data.style.StyleRepository
import jarvay.workpaper.others.STATE_IN_STATED
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RuleFormViewModel @Inject constructor(
    private val repository: RuleRepository,
    styleRepository: StyleRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val ruleId: String? = savedStateHandle.get<String>(RULE_ID_SAVED_STATE_KEY)

    val ruleWithRelation = if (ruleId != null) repository.findRuleById(ruleId.toLong()) else null

    val styles = styleRepository.allStyles.stateIn(
        viewModelScope,
        STATE_IN_STATED,
        emptyList()
    )

    fun insert(item: Rule) {
        viewModelScope.launch {
            repository.insert(item)
        }
    }

    fun update(item: Rule) {
        viewModelScope.launch {
            repository.update(item)
        }
    }

    fun exists(startHour: Int, startMinute: Int, days: List<Int>, ruleId: Long? = null): Boolean {
        return repository.exists(startHour, startMinute, days, ruleId)
    }

    companion object {
        private const val RULE_ID_SAVED_STATE_KEY = "ruleId"
    }
}