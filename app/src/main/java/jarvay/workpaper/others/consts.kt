package jarvay.workpaper.others

import androidx.annotation.StringRes
import jarvay.workpaper.compose.day.DayOption
import jarvay.workpaper.data.day.DayValue

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

const val DEFAULT_WALLPAPER_CHANGE_INTERVAL = 5

sealed class SharePreferenceKey {
    companion object {
        const val SHARED_PREFERENCE_NAME = "workpaper"
        const val CURRENT_ALBUM_ID_KEY = "currentAlbumId"
        const val LAST_INDEX_KEY = "lastIndex"
        const val LAST_WALLPAPER = "lastWallpaper"
        const val SETTINGS_KEY = "settings"
        const val IS_RUNNING_KEY = "isRunning"
    }
}