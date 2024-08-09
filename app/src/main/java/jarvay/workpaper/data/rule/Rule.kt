package jarvay.workpaper.data.rule

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rules")
data class Rule(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val ruleId: Long = 0,
    var days: List<Int>,
    var startHour: Int,
    var startMinute: Int,
    var albumIds: List<Long>,
    var changeByTiming: Boolean,
    var changeWhileUnlock: Boolean,
    var random: Boolean = false,
    var interval: Int = 15,
)