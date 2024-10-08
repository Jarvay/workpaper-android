package jarvay.workpaper.data.rule

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import jarvay.workpaper.data.album.AlbumWithWallpapers
import kotlinx.coroutines.flow.Flow

@Dao
interface RuleDao {
    @Query(
        "SELECT rules.*, albums.* FROM rules " +
                "JOIN rule_album_relations ON rules.id = rule_album_relations.ruleId " +
                "JOIN albums ON rule_album_relations.albumId = albums.id " +
                "ORDER BY rules.id ASC"
    )
    fun findAll(): Map<Rule, List<AlbumWithWallpapers>>

    @Query(
        "SELECT rules.*, albums.* FROM rules " +
                "JOIN rule_album_relations ON rules.id = rule_album_relations.ruleId " +
                "JOIN albums ON rule_album_relations.albumId = albums.id " +
                "ORDER BY rules.id ASC"
    )
    fun findAllFlow(): Flow<Map<Rule, List<AlbumWithWallpapers>>>

    @Query("SELECT * FROM rules WHERE id= :id ")
    fun findByIdFlow(id: Long): Flow<Rule>

    @Query("SELECT * FROM rules WHERE id= :id ")
    fun findById(id: Long): Rule?

    @Query(
        "SELECT rules.*, albums.* FROM rules " +
                "JOIN rule_album_relations ON rules.id = rule_album_relations.ruleId " +
                "JOIN albums ON rule_album_relations.albumId = albums.id " +
                "WHERE rules.id = :id"
    )
    fun findWithAlbumsById(id: Long): Map<Rule, List<AlbumWithWallpapers>>?

    @Insert
    suspend fun insert(item: Rule): Long

    @Update
    suspend fun update(item: Rule)

    @Delete
    suspend fun delete(item: Rule)
}