package jarvay.workpaper.others

import android.os.Build
import androidx.annotation.StringRes
import jarvay.workpaper.data.day.DayValue
import kotlinx.coroutines.flow.SharingStarted

data class DayOption(@StringRes val labelId: Int, val value: Int)

val dayOptions = listOf(
    DayOption(DayValue.MONDAY.textId, DayValue.MONDAY.day),
    DayOption(DayValue.TUESDAY.textId, DayValue.TUESDAY.day),
    DayOption(DayValue.WEDNESDAY.textId, DayValue.WEDNESDAY.day),
    DayOption(DayValue.THURSDAY.textId, DayValue.THURSDAY.day),
    DayOption(DayValue.FRIDAY.textId, DayValue.FRIDAY.day),
    DayOption(DayValue.SATURDAY.textId, DayValue.SATURDAY.day),
    DayOption(DayValue.SUNDAY.textId, DayValue.SUNDAY.day),
)

const val MAX_WALLPAPER_HEIGHT = 2400
const val MAX_WALLPAPER_WIDTH = 1200

const val DEFAULT_WALLPAPER_CHANGE_INTERVAL = 15

val STATE_IN_STATED = SharingStarted.WhileSubscribed(5000)

val MAX_PERSISTED_URI_GRANTS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) 512 else 128