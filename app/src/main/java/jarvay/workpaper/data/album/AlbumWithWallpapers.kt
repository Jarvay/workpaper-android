package jarvay.workpaper.data.album

import androidx.room.Embedded
import androidx.room.Relation
import jarvay.workpaper.data.wallpaper.Wallpaper

data class AlbumWithWallpapers(
    @Embedded val album: Album,
    @Relation(
        parentColumn = "id",
        entityColumn = "albumId"
    )
    val wallpapers: List<Wallpaper>
)