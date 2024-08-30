package jarvay.workpaper.data.rule

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface RuleAlbumRelationDao {
    @Insert
    suspend fun insert(item: RuleAlbumRelation)

    @Insert
    suspend fun insert(items: List<RuleAlbumRelation>)

    @Delete
    suspend fun delete(item: RuleAlbumRelation)

    @Query("SELECT EXISTS (SELECT * FROM rule_album_relations WHERE albumId = :albumId )")
    fun exists(albumId: Long): Boolean

    @Query("DELETE FROM rule_album_relations WHERE ruleId = :ruleId")
    fun deleteByRuleId(ruleId: Long)
}