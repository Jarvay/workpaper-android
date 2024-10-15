package jarvay.workpaper.data.rule

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import jarvay.workpaper.others.DEFAULT_WALLPAPER_CHANGE_INTERVAL
import jarvay.workpaper.others.dayOptions

@Entity(tableName = "rules")
data class Rule(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val ruleId: Long = 0,
    var days: List<Int> = dayOptions.map { it.value },
    var startHour: Int = 0,
    var startMinute: Int = 0,
    var albumIds: List<Long> = emptyList(),
    var changeByTiming: Boolean = true,
    var changeWhileUnlock: Boolean = false,
    var random: Boolean = false,
    var interval: Int = DEFAULT_WALLPAPER_CHANGE_INTERVAL,
    @ColumnInfo(defaultValue = "-1")
    var styleId: Long = -1,
    @ColumnInfo(defaultValue = "0")
    var noStyle: Boolean = false,
)