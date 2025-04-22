package jarvay.workpaper.data.album

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "albums")
data class Album(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val albumId: Long = 0,
    val name: String,
    val coverUri: String? = null,
    @ColumnInfo(defaultValue = "null")
    val dirs: List<String>? = null,
)