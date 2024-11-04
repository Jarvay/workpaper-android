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
import jarvay.workpaper.others.SUPPORTED_WALLPAPER_TYPES_PREFIX
import jarvay.workpaper.others.wallpaperType
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

    fun addWallpapers(wallpapers: List<Wallpaper>) {
        viewModelScope.launch {
            wallpaperRepository.insert(wallpapers)
        }
    }

    fun addWallpapersFromFolder(file: DocumentFile, albumId: Long) {
        val result = getWallpapersInDir(file)
        addWallpapers(result.map {
            it.copy(albumId = albumId)
        })
    }

    fun update(item: Album) {
        viewModelScope.launch {
            repository.update(item)
        }
    }

    private fun getWallpapersInDir(
        documentFile: DocumentFile,
        result: MutableList<Wallpaper> = mutableListOf(),
    ): MutableList<Wallpaper> {
        for (item in documentFile.listFiles()) {
            if (item.isDirectory) {
                getWallpapersInDir(item, result)
            } else if (item.isFile) {
                val mimeType = item.type ?: continue
                val supported = SUPPORTED_WALLPAPER_TYPES_PREFIX.any {
                    mimeType.startsWith(it)
                }
                if (!supported) continue
                result.add(
                    Wallpaper(
                        contentUri = item.uri.toString(),
                        type = wallpaperType(mimeType),
                    )
                )
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