package jarvay.workpaper.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jarvay.workpaper.data.album.Album
import jarvay.workpaper.data.album.AlbumRepository
import jarvay.workpaper.data.rule.RuleRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlbumListViewModel @Inject constructor(
    private val repository: AlbumRepository,
    private val ruleRepository: RuleRepository
) :
    ViewModel() {
    val allAlbums: LiveData<List<Album>> = repository.allAlbums.asLiveData()

    fun insert(item: Album) {
        viewModelScope.launch {
            repository.insert(item)
        }
    }

    fun delete(item: Album) {
        viewModelScope.launch {
            repository.delete(item)
        }
    }

    fun isUsing(albumId: Long): Boolean {
        return ruleRepository.getRuleByAlbumId(albumId) != null
    }
}