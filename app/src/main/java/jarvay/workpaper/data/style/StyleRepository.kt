package jarvay.workpaper.data.style

import androidx.annotation.WorkerThread
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StyleRepository @Inject constructor(
    private val styleDao: StyleDao,
) {
    val allStyles = styleDao.findAllFlow()

    fun findById(id: Long): Style? {
        return styleDao.findById(id)
    }

    @WorkerThread
    suspend fun insert(item: Style) {
        styleDao.insert(item)
    }

    @WorkerThread
    suspend fun update(item: Style) {
        styleDao.update(item)
    }

    @WorkerThread
    suspend fun delete(item: Style) {
        styleDao.delete(item)
    }

    fun exists(name: String): Boolean {
        return styleDao.exists(name)
    }
}