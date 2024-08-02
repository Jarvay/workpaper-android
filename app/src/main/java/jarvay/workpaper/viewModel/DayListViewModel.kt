package jarvay.workpaper.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jarvay.workpaper.data.day.Day
import jarvay.workpaper.data.day.DayRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DayListViewModel @Inject constructor(private val repository: DayRepository) : ViewModel() {
    val allDays: LiveData<List<Day>> = repository.allDays.asLiveData();

    fun insert(item: Day) {
        viewModelScope.launch {
            repository.insert(item)
        }
    }
}