package jarvay.workpaper.data.wallpaper

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wallpapers")
data class Wallpaper(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val wallpaperId: Long = 0,
    val albumId: Long,
    val contentUri: String,
)