package jarvay.workpaper.data.rule

import android.util.Log
import androidx.annotation.WorkerThread
import androidx.room.withTransaction
import jarvay.workpaper.data.AppDatabase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RuleRepository @Inject constructor(
    private val ruleDao: RuleDao,
) {

    @Inject
    lateinit var appDatabase: AppDatabase

    @Inject
    lateinit var ruleAlbumRelationDao: RuleAlbumRelationDao

    val allRules = ruleDao.findAllFlow();

    fun getRuleFlow(ruleId: Long) = ruleDao.findByIdFlow(ruleId)

    fun getRule(ruleId: Long) = ruleDao.findById(ruleId)

    fun getRuleWithAlbums(ruleId: Long): RuleAlbums? {
        val ruleAlbums = ruleDao.findWithAlbumsById(ruleId)
        return if (ruleAlbums?.isNotEmpty() == true) ruleAlbums.entries.map {
            RuleAlbums(it.key, it.value)
        }.first() else null
    }

    fun isAlbumUsing(albumId: Long) = ruleAlbumRelationDao.exists(albumId)

    @WorkerThread
    suspend fun insert(item: Rule) {
        appDatabase.withTransaction {
            val ruleId = ruleDao.insert(item)
            Log.d("ruleId", ruleId.toString())

            val ruleAlbumRelations = item.albumIds.map {
                RuleAlbumRelation(
                    ruleId = ruleId,
                    albumId = it
                )
            }

            ruleAlbumRelationDao.insert(ruleAlbumRelations)
        }
    }

    @WorkerThread
    suspend fun update(item: Rule) {
        Log.d(javaClass.simpleName, item.toString())
        appDatabase.withTransaction {
            ruleDao.update(item)
            ruleAlbumRelationDao.deleteByRuleId(item.ruleId)

            val ruleAlbumRelations = item.albumIds.map {
                RuleAlbumRelation(
                    ruleId = item.ruleId,
                    albumId = it
                )
            }

            ruleAlbumRelationDao.insert(ruleAlbumRelations)
        }
    }

    @WorkerThread
    suspend fun delete(item: Rule) {
        appDatabase.withTransaction {
            ruleDao.delete(item)

            ruleAlbumRelationDao.deleteByRuleId(item.ruleId)
        }
    }

    fun exists(startHour: Int, startMinute: Int, days: List<Int>, ruleId: Long? = null): Boolean {
        return ruleDao.findAll().map {
            RuleAlbums(
                rule = it.key,
                albums = it.value
            )
        }.find {
            it.rule.days.any { d -> days.contains(d) }
                    && it.rule.startHour == startHour
                    && it.rule.startMinute == startMinute
                    && (ruleId == null || (it.rule.ruleId != ruleId))
        } != null
    }
}