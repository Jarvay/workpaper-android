package jarvay.workpaper.data.wallpaper

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

enum class WallpaperType(val value: Int) {
    IMAGE(1),
    VIDEO(2)
}

@Entity(tableName = "wallpapers")
data class Wallpaper(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val wallpaperId: Long = 0,
    val albumId: Long = -1,
    val contentUri: String,
    @ColumnInfo(defaultValue = "IMAGE")
    val type: WallpaperType,
    @ColumnInfo(defaultValue = "NULL")
    val ratio: Float?,
)