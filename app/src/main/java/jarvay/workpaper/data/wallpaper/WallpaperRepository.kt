package jarvay.workpaper.data.wallpaper

import androidx.annotation.WorkerThread
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WallpaperRepository @Inject constructor(
    private val wallpaperDao: WallpaperDao
) {
    @WorkerThread
    suspend fun insert(item: Wallpaper) {
        wallpaperDao.insert(item)
    }

    @WorkerThread
    suspend fun insert(item: List<Wallpaper>) {
        wallpaperDao.insert(item)
    }

    @WorkerThread
    suspend fun update(item: Wallpaper) {
        wallpaperDao.update(item)
    }

    @WorkerThread
    suspend fun delete(item: Wallpaper) {
        wallpaperDao.delete(item)
    }

    @WorkerThread
    suspend fun delete(ids: List<Long>) {
        wallpaperDao.delete(ids)
    }

    fun existsByContentUri(contentUri: String): Boolean {
        return wallpaperDao.existsByContentUri(contentUri)
    }
}