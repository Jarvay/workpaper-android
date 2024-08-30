package jarvay.workpaper.data.album

import androidx.annotation.WorkerThread
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlbumRepository @Inject constructor(
    private val albumDao: AlbumDao
) {

    val allAlbums: Flow<List<Album>> = albumDao.findAll()

    fun getAlbum(albumId: Long) = albumDao.findFlowById(albumId)

    fun existsByName(name: String): Boolean {
        return albumDao.existsByName(name)
    }

    fun exists(name: String, albumId: Long): Boolean {
        return albumDao.exists(name, albumId)
    }

    @WorkerThread
    suspend fun insert(item: Album) {
        albumDao.insert(item)
    }

    @WorkerThread
    suspend fun update(item: Album) {
        albumDao.update(item)
    }

    @WorkerThread
    suspend fun delete(item: Album) {
        albumDao.delete(item)
    }
}