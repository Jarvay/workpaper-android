package jarvay.workpaper.data.album

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AlbumDao {
    @Query("SELECT * FROM albums ORDER BY id ASC")
    fun findAll(): Flow<List<Album>>

    @Query("SELECT * FROM albums WHERE id=:id ")
    fun findFlowById(id: Long): Flow<Album>

    @Query("SELECT * FROM albums WHERE id=:id ")
    fun findById(id: Long): Album?

    @Query("SELECT EXISTS (SELECT * FROM albums WHERE name=:name )")
    fun existsByName(name: String): Boolean

    @Query("SELECT EXISTS (SELECT * FROM albums WHERE name=:name AND id != :albumId)")
    fun exists(name: String, albumId: Long): Boolean

    @Insert
    suspend fun insert(item: Album)

    @Update
    suspend fun update(item: Album)

    @Delete
    suspend fun delete(item: Album)
}