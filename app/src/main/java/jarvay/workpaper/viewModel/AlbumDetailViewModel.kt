package jarvay.workpaper.viewModel

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.size.Size
import dagger.hilt.android.lifecycle.HiltViewModel
import jarvay.workpaper.data.album.Album
import jarvay.workpaper.data.album.AlbumRepository
import jarvay.workpaper.data.wallpaper.Wallpaper
import jarvay.workpaper.data.wallpaper.WallpaperRepository
import jarvay.workpaper.others.STATE_IN_STATED
import jarvay.workpaper.others.SUPPORTED_WALLPAPER_TYPES_PREFIX
import jarvay.workpaper.others.wallpaperType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
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

    val albumWithWallpapers = repository.getAlbumWithWallpapers(albumId = albumId.toLong()).stateIn(
        viewModelScope, STATE_IN_STATED, null
    )
    val album = repository.getAlbum(albumId = albumId.toLong()).stateIn(
        viewModelScope, STATE_IN_STATED, null
    )

    val loading = MutableStateFlow(false)

    fun deleteWallpapers(wallpaperIds: List<Long>) {
        if (albumWithWallpapers.value != null) {
            MainScope().launch {
                wallpaperRepository.delete(wallpaperIds)
            }
        }
    }

    fun emptyAlbum() {
        MainScope().launch {
            wallpaperRepository.deleteByAlbumId(albumId = albumId.toLong())
        }
    }

    private fun addWallpapers(wallpapers: List<Wallpaper>) {
        MainScope().launch {
            wallpaperRepository.insert(wallpapers)
        }
    }

    fun addFromUris(context: Context, uris: List<Uri>) {
        loading.value = true
        val takeFlags: Int =
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION

        MainScope().launch(Dispatchers.IO) {
            val chunks = uris.chunked(WALLPAPER_INSERT_CHUNK_SIZE)
            chunks.forEach { chunk ->
                val newWallpapers = mutableListOf<Wallpaper>()

                chunk.forEach { uri ->
                    context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                    val permissions = context.contentResolver.persistedUriPermissions
                    if (permissions.any { it.uri == uri }) {
                        val file = DocumentFile.fromSingleUri(context, uri)
                        file?.let {
                            val ratio = getImageRatio(context, uri)
                            newWallpapers.add(
                                Wallpaper(
                                    contentUri = uri.toString(),
                                    type = wallpaperType(it.type ?: ""),
                                    albumId = albumId.toLong(),
                                    ratio = ratio
                                )
                            )
                        }
                    }
                }

                addWallpapers(newWallpapers)
            }
            loading.value = false
        }
    }

    fun addWallpapersFromFolder(context: Context, file: DocumentFile) {
        loading.value = true
        MainScope().launch(Dispatchers.IO) {
            val chunks = getWallpapersInDir(context, file).chunked(WALLPAPER_INSERT_CHUNK_SIZE)
            chunks.forEach { chunk ->
                val wallpapers = mutableListOf<Wallpaper>()
                chunk.forEach { wallpaper ->
                    wallpapers.add(
                        wallpaper.copy(
                            albumId = albumId.toLong(),
                            ratio = getImageRatio(context = context, wallpaper.contentUri.toUri())
                        )
                    )
                }
                addWallpapers(wallpapers = wallpapers)
            }
            loading.value = false
        }
    }

    fun updateWallpapersByDirs(context: Context) {
        albumWithWallpapers.value?.album?.let {
            val dirs = it.dirs ?: emptyList()
            if (dirs.isEmpty()) return

            MainScope().launch(Dispatchers.IO) {
                loading.value = true
                val oldWallpapers = albumWithWallpapers.value?.wallpapers ?: emptyList()
                val newWallpapers = mutableListOf<Wallpaper>()
                for (dir in dirs) {
                    val documentFile = DocumentFile.fromTreeUri(context, dir.toUri()) ?: continue
                    newWallpapers.addAll(
                        getWallpapersInDir(
                            context,
                            documentFile,
                            skipGettingRatio = true
                        )
                    )
                }

                if (oldWallpapers.isNotEmpty()) {
                    val toDeleteIds = mutableListOf<Long>()
                    oldWallpapers.forEach { old ->
                        val shouldDelete =
                            newWallpapers.none { new -> new.contentUri == old.contentUri }
                        if (shouldDelete) {
                            toDeleteIds.add(old.wallpaperId)
                        }
                    }
                    deleteWallpapers(toDeleteIds)
                }

                val toAddList = mutableListOf<Wallpaper>()
                newWallpapers.forEach { new ->
                    val shouldAdd = oldWallpapers.none { old -> old.contentUri == new.contentUri }
                    if (shouldAdd) {
                        toAddList.add(new)
                    }
                }
                val chunks = toAddList.chunked(WALLPAPER_INSERT_CHUNK_SIZE)
                chunks.forEach { chunk ->
                    val wallpapers = mutableListOf<Wallpaper>()
                    chunk.forEach { wallpaper ->
                        wallpapers.add(
                            wallpaper.copy(
                                albumId = albumId.toLong(),
                                ratio = getImageRatio(
                                    context = context,
                                    wallpaper.contentUri.toUri()
                                )
                            )
                        )
                    }

                    addWallpapers(wallpapers)
                }

                loading.value = false
            }
        }
    }

    fun update(item: Album) {
        viewModelScope.launch {
            repository.update(item)
        }
    }

    fun addDir(uri: String) {
        album.value?.let {
            val dirs = (it.dirs ?: emptyList()).toMutableList()
            if (!dirs.contains(uri)) {
                dirs.add(uri)
            }
            update(it.copy(dirs = dirs))
        }
    }

    fun removeDir(uri: String) {
        album.value?.let {
            val dirs = (it.dirs ?: emptyList()).toMutableList()
            dirs.remove(uri)
            update(it.copy(dirs = dirs))
        }
    }

    private suspend fun getWallpapersInDir(
        context: Context,
        documentFile: DocumentFile,
        result: MutableList<Wallpaper> = mutableListOf(),
        skipGettingRatio: Boolean = false
    ): MutableList<Wallpaper> {
        for (item in documentFile.listFiles()) {
            if (item.isDirectory) {
                getWallpapersInDir(context, item, result)
            } else if (item.isFile) {
                val ratio = if (skipGettingRatio) null else getImageRatio(context, item.uri)
                val mimeType = item.type ?: continue

                val supported = SUPPORTED_WALLPAPER_TYPES_PREFIX.any {
                    mimeType.startsWith(it)
                }
                if (!supported) continue
                result.add(
                    Wallpaper(
                        contentUri = item.uri.toString(),
                        type = wallpaperType(mimeType),
                        ratio = ratio
                    )
                )
            }
        }

        return result
    }

    suspend fun getImageRatio(context: Context, uri: Uri): Float? {
        val imageLoader = ImageLoader(context)
        val request = ImageRequest.Builder(context).data(uri).size(Size.ORIGINAL).build()

        return try {
            val result = imageLoader.execute(request)
            val width = result.image!!.width.toFloat()
            val height = result.image!!.height.toFloat()
            width / height
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        private const val ALBUM_ID_SAVED_STATE_KEY = "albumId"

        private const val WALLPAPER_INSERT_CHUNK_SIZE = 5

        fun releasePermissions(context: Context, contentUri: Uri) {
            val flags: Int =
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            val contentResolver = context.contentResolver
            contentResolver.releasePersistableUriPermission(contentUri, flags)
        }
    }
}