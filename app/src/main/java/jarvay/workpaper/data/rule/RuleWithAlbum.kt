package jarvay.workpaper.data.rule

import androidx.room.Embedded
import androidx.room.Relation
import jarvay.workpaper.data.album.Album

data class RuleWithAlbum(
    @Embedded val rule: Rule,
    @Relation(
        parentColumn = "albumId",
        entityColumn = "id",
    ) val album: Album
)

data class RuleWithAlbumToSort(
    var ruleWithAlbum
    : RuleWithAlbum,
    var sortValue: Long,
    var day: Int,
)