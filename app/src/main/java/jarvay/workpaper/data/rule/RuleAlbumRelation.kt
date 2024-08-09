package jarvay.workpaper.data.rule

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import jarvay.workpaper.data.album.Album

@Entity(tableName = "rule_album_relations")
data class RuleAlbumRelation(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val relationId: Long = 0,
    val ruleId: Long,
    val albumId: Long
)

data class RuleAlbums(
    var rule: Rule,
    var albums: List<Album>
)

data class RuleAlbumsToSort(
    var ruleAlbums: RuleAlbums,
    var sortValue: Long,
    var day: Int,
)