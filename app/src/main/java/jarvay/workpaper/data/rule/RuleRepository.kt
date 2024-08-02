package jarvay.workpaper.data.rule

import android.util.Log
import androidx.annotation.WorkerThread
import jarvay.workpaper.data.day.RuleDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RuleRepository @Inject constructor(private val ruleDao: RuleDao) {

    val allRules = ruleDao.findAllFlow();
    val allRulesWithAlbum: Flow<List<RuleWithAlbum>> = ruleDao.findAllWithAlbum();

    fun getRule(ruleId: Long) = ruleDao.findByIdFlow(ruleId)
    fun getRuleWithAlbum(ruleId: Long) = ruleDao.findWithAlbumById(ruleId)

    fun getRuleByAlbumId(albumId: Long) = ruleDao.findByAlbumId(albumId)

    @WorkerThread
    suspend fun insert(item: Rule) {
        ruleDao.insert(item)
    }

    @WorkerThread
    suspend fun update(item: Rule) {
        Log.d(javaClass.simpleName, item.toString())
        ruleDao.update(item)
    }

    @WorkerThread
    suspend fun delete(item: Rule) {
        ruleDao.delete(item)
    }

    companion object {
        @Volatile
        private var instance: RuleRepository? = null

        fun getInstance(ruleDao: RuleDao) =
            instance ?: synchronized(this) {
                instance ?: RuleRepository(ruleDao).also { instance = it }
            }
    }
}