package jarvay.workpaper.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jarvay.workpaper.data.album.Album
import jarvay.workpaper.data.album.AlbumRepository
import jarvay.workpaper.data.rule.RuleRepository
import jarvay.workpaper.others.STATE_IN_STATED
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlbumListViewModel @Inject constructor(
    private val repository: AlbumRepository,
    private val ruleRepository: RuleRepository
) : ViewModel() {
    val allAlbums = repository.allAlbums.stateIn(
        viewModelScope,
        STATE_IN_STATED,
        emptyList()
    )

    fun insert(item: Album) {
        viewModelScope.launch {
            repository.insert(item)
        }
    }

    fun update(item: Album) {
        viewModelScope.launch {
            repository.update(item)
        }
    }

    fun delete(item: Album) {
        viewModelScope.launch {
            repository.delete(item)
        }
    }

    fun isUsing(albumId: Long): Boolean {
        return ruleRepository.isAlbumUsing(albumId)
    }

    fun existsByName(name: String): Boolean {
        return repository.existsByName(name)
    }
}