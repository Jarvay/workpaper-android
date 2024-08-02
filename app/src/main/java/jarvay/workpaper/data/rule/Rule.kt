package jarvay.workpaper.data.rule

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rules")
data class Rule(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val ruleId: Long = 0,
    val days: List<Int>,
    val startHour: Int,
    val startMinute: Int,
    val albumId: Long,
    val random: Boolean = false,
    val interval: Int = 15
)