package jarvay.workpaper.data.wallpaper

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WallpaperDao {
    @Insert
    suspend fun insert(item: Wallpaper)

    @Insert
    suspend fun insert(item: List<Wallpaper>)

    @Update
    suspend fun update(item: Wallpaper)

    @Delete
    suspend fun delete(item: Wallpaper)

    @Query("DELETE FROM wallpapers where id in (:ids)")
    suspend fun delete(ids: List<Long>)

    @Query("SELECT EXISTS (SELECT * FROM wallpapers WHERE contentUri=:contentUri )")
    fun existsByContentUri(contentUri: String): Boolean
}