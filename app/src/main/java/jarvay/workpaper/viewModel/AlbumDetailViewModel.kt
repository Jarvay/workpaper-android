package jarvay.workpaper.viewModel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jarvay.workpaper.data.album.Album
import jarvay.workpaper.data.album.AlbumRepository
import jarvay.workpaper.others.STATE_IN_STATED
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    private val repository: AlbumRepository,
    savedStateHandle: SavedStateHandle,
) :
    ViewModel() {
    private val albumId: String = savedStateHandle.get<String>(ALBUM_ID_SAVED_STATE_KEY)!!

    val album = repository.getAlbum(albumId = albumId.toLong()).stateIn(
        viewModelScope,
        STATE_IN_STATED,
        null
    )

    fun update(item: Album) {
        viewModelScope.launch {
            repository.update(item)
        }
    }

    companion object {
        private const val ALBUM_ID_SAVED_STATE_KEY = "albumId"
    }
}