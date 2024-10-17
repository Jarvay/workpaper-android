package jarvay.workpaper.data.rule

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import jarvay.workpaper.data.album.Album
import jarvay.workpaper.data.album.AlbumWithWallpapers
import jarvay.workpaper.data.style.Style

data class RuleWithRelation(
    @Embedded
    val rule: Rule,

    @Relation(
        entity = Album::class,
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            RuleAlbumRelation::class,
            parentColumn = "ruleId",
            entityColumn = "albumId"
        )
    )
    val albums: List<AlbumWithWallpapers> = emptyList(),

    @Relation(
        parentColumn = "styleId",
        entityColumn = "id"
    )
    val style: Style? = null
)