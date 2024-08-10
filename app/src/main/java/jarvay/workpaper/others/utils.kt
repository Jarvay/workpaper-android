package jarvay.workpaper.others

import android.content.Context
import android.graphics.BitmapFactory
import android.icu.util.Calendar
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import jarvay.workpaper.data.preferences.RunningPreferences
import jarvay.workpaper.data.preferences.RunningPreferencesKeys
import jarvay.workpaper.data.rule.Rule
import jarvay.workpaper.data.rule.RuleAlbums
import jarvay.workpaper.data.rule.RuleAlbumsToSort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.FileNotFoundException
import java.io.IOException

fun formatTime(hour: Int, minute: Int): String {
    var hourString = hour.toString()
    hourString = if (hourString.length == 1) "0${hourString}" else hourString
    var minuteString = minute.toString()
    minuteString = if (minuteString.length == 1) "0${minuteString}" else minuteString
    return "${hourString}:${minuteString}"
}

fun nextDayOfWeek(current: Int): Int {
    return if (current + 1 == 8) 1 else current + 1
}

fun prevDayOfWeek(current: Int): Int {
    return if (current - 1 == 0) 7 else current - 1
}

fun getCalendarWithRule(rule: Rule, dayOfWeek: Int): Calendar {
    val calendar = Calendar.getInstance()
    return calendar.apply {
        set(Calendar.DAY_OF_WEEK, dayOfWeek)
        set(Calendar.HOUR_OF_DAY, rule.startHour)
        set(Calendar.MINUTE, rule.startMinute)
        set(Calendar.SECOND, 0)
    }
}

fun findRule(
    ruleWithAlbums: List<RuleAlbums>,
    finder: (List<RuleAlbumsToSort>) -> RuleAlbumsToSort?,
): RuleAlbumsToSort? {
    val rules = ArrayList<RuleAlbumsToSort>()
    ruleWithAlbums.forEach {
        it.rule.days.forEach { day ->
            val sortValue = day * 24 * 60 + it.rule.startHour * 60 + it.rule.startMinute
            rules.add(
                RuleAlbumsToSort(
                    ruleAlbums = it.copy(),
                    sortValue = sortValue.toLong(),
                    day = day
                )
            )
        }
    }

    val list = rules.apply {
        Log.d("rules", rules.toString())
    }
    return finder(list)
}

fun currentMinute(): Int {
    val calendar = Calendar.getInstance()
    return calendar.get(Calendar.DAY_OF_WEEK) * 24 * 60 + calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(
        Calendar.MINUTE
    )
}

fun prevRule(list: List<RuleAlbums>): RuleAlbumsToSort? {
    return findRule(ruleWithAlbums = list, finder = { l ->
        val sortedList = l.sortedByDescending { it.sortValue }
        val result = sortedList.find {
            it.sortValue <= currentMinute()
        }
        result ?: if (sortedList.isNotEmpty()) sortedList[0] else null
    })
}

fun nextRule(list: List<RuleAlbums>): RuleAlbumsToSort? {
    return findRule(ruleWithAlbums = list, finder = { l ->
        val sortedList = l.sortedBy { it.sortValue }
        val result = sortedList.find {
            it.sortValue >= currentMinute()
        }
        result ?: if (sortedList.isNotEmpty()) sortedList[0] else null
    })
}

fun showToast(context: Context, text: String) {
    Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
}

fun showToast(context: Context, @StringRes strId: Int) {
    showToast(context, context.getString(strId))
}

fun getSize(context: Context, fileStrUri: String): Size {
    var size = Size(-1, -1)
    try {
        context.contentResolver.openInputStream(fileStrUri.toUri()).use { inputStream ->
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true // 只解析边界，不加载图片到内存
            BitmapFactory.decodeStream(inputStream, null, options)
            size = Size(options.outWidth, options.outHeight)
        }
    } catch (e: FileNotFoundException) {
        e.printStackTrace()
    } catch (e: IOException) {
        e.printStackTrace()
    }

    return size
}

fun runningPreferencesFlow(dataStore: DataStore<Preferences>): Flow<RunningPreferences> {
    return dataStore.data.map { preferences ->
        val currentRuleId = preferences[RunningPreferencesKeys.CURRENT_RULE_ID] ?: -1
        val lastIndex = preferences[RunningPreferencesKeys.LAST_INDEX] ?: -1
        val lastWallpaper = preferences[RunningPreferencesKeys.LAST_WALLPAPER] ?: ""
        val nextRuleId = preferences[RunningPreferencesKeys.NEXT_RULE_ID] ?: -1
        val running = preferences[RunningPreferencesKeys.RUNNING] ?: false

        RunningPreferences(
            currentRuleId = currentRuleId,
            lastIndex = lastIndex,
            lastWallpaper = lastWallpaper,
            nextRuleId = nextRuleId,
            running = running
        )
    }
}