package jarvay.workpaper.viewModel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jarvay.workpaper.data.day.DayRepository
import jarvay.workpaper.data.rule.Rule
import jarvay.workpaper.data.rule.RuleRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DayDetailViewModel @Inject constructor(
    private val repository: DayRepository,
    private val ruleRepository: RuleRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val dayId: String = savedStateHandle.get<String>(DAY_ID_SAVED_STATE_KEY)!!

    val day = repository.getDay(dayId.toLong()).asLiveData()

    fun deleteRule(item: Rule) {
        viewModelScope.launch {
            ruleRepository.delete(item)
        }
    }

    companion object {
        private const val DAY_ID_SAVED_STATE_KEY = "dayId"
    }
}