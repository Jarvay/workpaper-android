package jarvay.workpaper.viewModel

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jarvay.workpaper.data.album.Album
import jarvay.workpaper.data.album.AlbumRepository
import jarvay.workpaper.others.STATE_IN_STATED
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    private val repository: AlbumRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val albumId: String = savedStateHandle.get<String>(ALBUM_ID_SAVED_STATE_KEY)!!

    val album = repository.getAlbum(albumId = albumId.toLong()).stateIn(
        viewModelScope,
        STATE_IN_STATED,
        null
    )


    fun deleteWallpaperUris(context: Context, uris: List<String>) {
        if (album.value != null) {
            viewModelScope.launch {
                val wallpaperUris = album.value!!.wallpaperUris.filter { !uris.contains(it) }

                repository.update(album.value!!.copy(wallpaperUris = wallpaperUris))
                uris.forEach {
                    updatePermissions(context, it.toUri(), repository.allAlbums.first())
                }
            }
        }
    }

    fun update(item: Album) {
        viewModelScope.launch {
            repository.update(item)
        }
    }

    companion object {
        private const val ALBUM_ID_SAVED_STATE_KEY = "albumId"

        fun updatePermissions(context: Context, contentUri: Uri, albums: List<Album>) {
            val used = albums.any {
                it.wallpaperUris.contains(contentUri.toString())
            }
            if (!used) {
                val flags: Int =
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                val contentResolver = context.contentResolver
                contentResolver.releasePersistableUriPermission(contentUri, flags)
            }
        }
    }
}