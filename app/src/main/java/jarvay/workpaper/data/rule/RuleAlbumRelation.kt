package jarvay.workpaper.data.rule

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rule_album_relations")
data class RuleAlbumRelation(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val relationId: Long = 0,
    val ruleId: Long,
    val albumId: Long
)

data class RuleWithRelationToSort(
    var ruleWithRelation: RuleWithRelation,
    var sortValue: Long,
    var day: Int,
)