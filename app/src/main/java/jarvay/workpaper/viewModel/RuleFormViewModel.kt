package jarvay.workpaper.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import jarvay.workpaper.data.rule.Rule
import jarvay.workpaper.data.rule.RuleRepository
import jarvay.workpaper.data.style.StyleRepository
import jarvay.workpaper.others.STATE_IN_STATED
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = RuleFormViewModel.Factory::class)
class RuleFormViewModel @AssistedInject constructor(
    private val repository: RuleRepository,
    styleRepository: StyleRepository,
    @Assisted private val ruleId: Long?,
) : ViewModel() {
    val ruleWithRelation = if (ruleId != null) repository.findRuleById(ruleId) else null

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

    @AssistedFactory
    interface Factory {
        fun create(ruleId: Long?): RuleFormViewModel
    }
}