package jarvay.workpaper.data.day

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import jarvay.workpaper.data.rule.Rule
import jarvay.workpaper.data.rule.RuleWithAlbum
import kotlinx.coroutines.flow.Flow

@Dao
interface RuleDao {
    @Query("SELECT * FROM rules")
    fun findAll(): List<RuleWithAlbum>

    @Query("SELECT * FROM rules")
    fun findAllFlow(): Flow<List<RuleWithAlbum>>

    @Query("SELECT * FROM rules")
    fun findAllWithAlbum(): Flow<List<RuleWithAlbum>>

    @Query("SELECT * FROM rules WHERE id= :id ")
    fun findByIdFlow(id: Long): Flow<Rule>

    @Query("SELECT * FROM rules WHERE id= :id ")
    fun findWithAlbumById(id: Long): RuleWithAlbum

    @Query("SELECT * FROM rules WHERE albumId= :albumId ")
    fun findByAlbumId(albumId: Long): Rule?

    @Insert
    suspend fun insert(item: Rule)

    @Update
    suspend fun update(item: Rule)

    @Delete
    suspend fun delete(item: Rule)
}