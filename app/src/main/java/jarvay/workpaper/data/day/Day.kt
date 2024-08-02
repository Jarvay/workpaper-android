package jarvay.workpaper.data.day

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "days")
data class Day(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val dayId: Long = 0, val days: List<Int>
) {
    fun dayValues(): List<DayValue> {
        return DayValue.entries.filter { days.contains(it.day) }
    }
}