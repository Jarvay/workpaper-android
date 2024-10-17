package jarvay.workpaper.data.style

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "styles")
data class Style(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val styleId: Long = 0,
    var name: String,
    var blurRadius: Int = 0,
    var noisePercent: Int = 0,
    var brightness: Int = 50,
    var contrast: Int = 50,
    var saturation: Int = 50,
)