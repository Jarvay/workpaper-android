package jarvay.workpaper.data.day

import android.icu.util.Calendar
import androidx.annotation.StringRes
import jarvay.workpaper.R

enum class DayValue(val day: Int, @StringRes val textId: Int) {
    MONDAY(Calendar.MONDAY, R.string.monday),
    TUESDAY(Calendar.TUESDAY, R.string.tuesday),
    WEDNESDAY(Calendar.WEDNESDAY, R.string.wednesday),
    THURSDAY(Calendar.THURSDAY, R.string.thursday),
    FRIDAY(Calendar.FRIDAY, R.string.friday),
    SATURDAY(Calendar.SATURDAY, R.string.saturday),
    SUNDAY(Calendar.SUNDAY, R.string.sunday)
}