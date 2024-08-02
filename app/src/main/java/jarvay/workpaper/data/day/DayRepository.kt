package jarvay.workpaper.data.day

import androidx.annotation.WorkerThread
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DayRepository @Inject constructor(
    private val dayDao: DayDao,
    private val ruleDao: RuleDao
) {

    val allDays: Flow<List<Day>> = dayDao.findAll()

    fun getDay(dayId: Long) = dayDao.findById(dayId)

    @WorkerThread
    suspend fun insert(day: Day) {
        dayDao.insert(day)
    }
}