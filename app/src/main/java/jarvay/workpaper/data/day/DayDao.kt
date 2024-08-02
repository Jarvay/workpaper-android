package jarvay.workpaper.data.day

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DayDao {
    @Query("SELECT * FROM days")
    fun findAll(): Flow<List<Day>>

    @Query("SELECT * FROM days WHERE id= :id ")
    fun findById(id: Long): Flow<Day>

    @Insert
    suspend fun insert(item: Day)
}