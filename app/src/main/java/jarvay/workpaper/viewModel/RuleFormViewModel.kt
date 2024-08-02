package jarvay.workpaper.viewModel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jarvay.workpaper.data.album.AlbumRepository
import jarvay.workpaper.data.rule.Rule
import jarvay.workpaper.data.rule.RuleRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RuleFormViewModel @Inject constructor(
    private val repository: RuleRepository,
    albumRepository: AlbumRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val ruleId: String? = savedStateHandle.get<String>(RULE_ID_SAVED_STATE_KEY)

    val rule = if (ruleId != null) repository.getRuleWithAlbum(ruleId.toLong()) else null

    val allAlbums = albumRepository.allAlbums.asLiveData()

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

    companion object {
        private const val RULE_ID_SAVED_STATE_KEY = "ruleId"
    }
}