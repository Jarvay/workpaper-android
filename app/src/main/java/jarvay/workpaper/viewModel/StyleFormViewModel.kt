package jarvay.workpaper.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import jarvay.workpaper.data.style.Style
import jarvay.workpaper.data.style.StyleRepository
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = StyleFormViewModel.Factory::class)
class StyleFormViewModel @AssistedInject constructor(
    private val repository: StyleRepository,
    @Assisted private val styleId: Long?,
) : ViewModel() {
    val style = repository.findById(styleId ?: -1)

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

    @AssistedFactory
    interface Factory {
        fun create(styleId: Long?): StyleFormViewModel
    }
}