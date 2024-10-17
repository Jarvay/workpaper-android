package jarvay.workpaper.viewModel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jarvay.workpaper.data.style.Style
import jarvay.workpaper.data.style.StyleRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StyleFormViewModel @Inject constructor(
    private val repository: StyleRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val styleId: String? = savedStateHandle.get<String>(STYLE_ID_SAVED_STATE_KEY)

    val style = repository.findById(styleId?.toLong() ?: -1)

    fun insert(item: Style) {
        viewModelScope.launch {
            repository.insert(item)
        }
    }

    fun update(item: Style) {
        viewModelScope.launch {
            repository.update(item)
        }
    }

    companion object {
        private const val STYLE_ID_SAVED_STATE_KEY = "styleId"
    }
}