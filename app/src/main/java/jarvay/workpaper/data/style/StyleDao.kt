package jarvay.workpaper.data.style

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StyleDao {
    @Query("SELECT * FROM styles")
    fun findAllFlow(): Flow<List<Style>>

    @Query(
        """
        SELECT * FROM styles WHERE id = :id
        """
    )
    fun findFlowById(id: Long): Flow<Style>?

    @Query(
        """
        SELECT * FROM styles WHERE id = :id
        """
    )
    fun findById(id: Long): Style?

    @Insert
    suspend fun insert(item: Style): Long

    @Update
    suspend fun update(item: Style)

    @Delete
    suspend fun delete(item: Style)

    @Query("SELECT EXISTS (SELECT * FROM albums WHERE name=:name )")
    fun exists(name: String): Boolean
}