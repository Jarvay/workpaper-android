package jarvay.workpaper.viewModel

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jarvay.workpaper.data.album.Album
import jarvay.workpaper.data.album.AlbumRepository
import jarvay.workpaper.data.wallpaper.Wallpaper
import jarvay.workpaper.data.wallpaper.WallpaperRepository
import jarvay.workpaper.others.STATE_IN_STATED
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    private val repository: AlbumRepository,
    private val wallpaperRepository: WallpaperRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val albumId: String = savedStateHandle.get<String>(ALBUM_ID_SAVED_STATE_KEY)!!

    val album = repository.getAlbum(albumId = albumId.toLong()).stateIn(
        viewModelScope,
        STATE_IN_STATED,
        null
    )

    val loading = MutableStateFlow(false)

    fun deleteWallpapers(wallpaperIds: List<Long>) {
        if (album.value != null) {
            viewModelScope.launch {
                wallpaperRepository.delete(wallpaperIds)
            }
        }
    }

    fun addWallpapers(contentUris: List<String>) {
        viewModelScope.launch {
            val wallpapers = contentUris.map {
                Wallpaper(albumId = albumId.toLong(), contentUri = it)
            }
            wallpaperRepository.insert(wallpapers)
        }
    }

    fun addWallpapersFromFolder(file: DocumentFile) {
        val result = getImagesInDir(file)
        val newUris = mutableListOf<String>()
        newUris.addAll(result)
        addWallpapers(newUris)
    }

    fun update(item: Album) {
        viewModelScope.launch {
            repository.update(item)
        }
    }

    private fun getImagesInDir(
        documentFile: DocumentFile,
        result: MutableList<String> = mutableListOf()
    ): MutableList<String> {
        for (item in documentFile.listFiles()) {
            if (item.isDirectory) {
                getImagesInDir(item, result)
            } else if (item.isFile) {
                val mimeType = item.type ?: continue
                if (!mimeType.startsWith("image/")) continue
                result.add(item.uri.toString())
            }
        }

        return result
    }

    companion object {
        private const val ALBUM_ID_SAVED_STATE_KEY = "albumId"

        fun releasePermissions(context: Context, contentUri: Uri) {
            val flags: Int =
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            val contentResolver = context.contentResolver
            contentResolver.releasePersistableUriPermission(contentUri, flags)
        }
    }
}