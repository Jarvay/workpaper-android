package jarvay.workpaper.others

import android.content.Context
import android.content.SharedPreferences
import android.icu.util.Calendar
import android.widget.Toast
import androidx.annotation.StringRes
import com.google.gson.Gson
import jarvay.workpaper.data.DEFAULT_SETTINGS
import jarvay.workpaper.data.Settings
import jarvay.workpaper.data.rule.Rule
import jarvay.workpaper.data.rule.RuleWithAlbum

fun formatTime(hour: Int, minute: Int): String {
    var hourString = hour.toString()
    hourString = if (hourString.length == 1) "0${hourString}" else hourString
    var minuteString = minute.toString()
    minuteString = if (minuteString.length == 1) "0${minuteString}" else minuteString
    return "${hourString}:${minuteString}"
}

fun nextDayOfWeek(current: Int): Int {
    return if (current + 1 == 7) 1 else current + 1
}

fun prevDayOfWeek(current: Int): Int {
    return if (current - 1 == 1) 7 else current - 1
}

fun getCalendarWithRule(rule: Rule): Calendar {
    val calendar = Calendar.getInstance()
    return calendar.apply {
        set(Calendar.HOUR_OF_DAY, rule.startHour)
        set(Calendar.MINUTE, rule.startMinute)
        set(Calendar.SECOND, 0)
    }
}

fun findRuleByDayOfWeek(
    ruleWithAlbums: List<RuleWithAlbum>,
    dayOfWeek: Int,
    filter: (RuleWithAlbum, Int) -> Boolean,
    compare: (RuleWithAlbum, RuleWithAlbum) -> RuleWithAlbum
): RuleWithAlbum? {
    var currentRule: RuleWithAlbum? = null
    ruleWithAlbums.filter { r ->
        r.rule.days.contains(dayOfWeek)
    }.filter {
        filter(it, dayOfWeek)
    }.forEach { r ->
        if (currentRule == null) {
            currentRule = r
        }

        currentRule = compare(currentRule!!, r)
    }

    return currentRule
}


fun prevRule(ruleWithAlbums: List<RuleWithAlbum>): RuleWithAlbum? {
    val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
    var calendar = Calendar.getInstance()
    val filter: (RuleWithAlbum, Int) -> Boolean = filter@{ r: RuleWithAlbum, d: Int ->
        if (d != Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
            return@filter true
        }
        calendar = getCalendarWithRule(r.rule)
        calendar.before(Calendar.getInstance())
    }
    val compare: (RuleWithAlbum, RuleWithAlbum) -> RuleWithAlbum = { current, next ->
        val currentCalendar = getCalendarWithRule(current.rule)
        val nextCalendar = getCalendarWithRule(next.rule)

        if (currentCalendar.after(nextCalendar)) current else next
    }

    var prevRule: RuleWithAlbum? =
        findRuleByDayOfWeek(
            ruleWithAlbums = ruleWithAlbums,
            dayOfWeek = dayOfWeek,
            filter = filter,
            compare = compare
        )
    var currentDayOfWeek = prevDayOfWeek(dayOfWeek)
    while (prevRule == null && currentDayOfWeek != dayOfWeek) {
        prevRule = findRuleByDayOfWeek(
            ruleWithAlbums,
            currentDayOfWeek,
            filter = filter,
            compare = compare
        )
        currentDayOfWeek = prevDayOfWeek(currentDayOfWeek)
    }

    return prevRule
}

fun nextRule(ruleWithAlbums: List<RuleWithAlbum>): RuleWithAlbum? {
    val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
    var calendar = Calendar.getInstance()
    val filter: (RuleWithAlbum, Int) -> Boolean = filter@{ r, d ->
        if (d != Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
            return@filter true
        }
        calendar = getCalendarWithRule(r.rule)
        calendar.after(Calendar.getInstance())
    }
    val compare: (RuleWithAlbum, RuleWithAlbum) -> RuleWithAlbum = { current, next ->
        val currentCalendar = getCalendarWithRule(current.rule)
        val nextCalendar = getCalendarWithRule(next.rule)

        if (currentCalendar.before(nextCalendar)) current else next
    }

    var nextRule: RuleWithAlbum? =
        findRuleByDayOfWeek(
            ruleWithAlbums = ruleWithAlbums,
            dayOfWeek = dayOfWeek,
            filter = filter,
            compare = compare
        )
    var currentDayOfWeek = nextDayOfWeek(dayOfWeek)
    while (nextRule == null && currentDayOfWeek != dayOfWeek) {
        nextRule = findRuleByDayOfWeek(
            ruleWithAlbums,
            currentDayOfWeek,
            filter = filter,
            compare = compare
        )
        currentDayOfWeek = nextDayOfWeek(currentDayOfWeek)
    }

    return nextRule
}

fun defaultSharedPreferences(context: Context): SharedPreferences {
    return context.getSharedPreferences(
        SharePreferenceKey.SHARED_PREFERENCE_NAME,
        Context.MODE_PRIVATE
    )
}

fun getSettings(context: Context): Settings {
    val sp = defaultSharedPreferences(context)
    val gson = Gson()

    val settingsStr = sp.getString(SharePreferenceKey.SETTINGS_KEY, null) ?: return DEFAULT_SETTINGS

    return gson.fromJson(settingsStr, Settings::class.java)
}

fun showToast(context: Context, @StringRes strId: Int) {
    Toast.makeText(context, strId, Toast.LENGTH_SHORT).show()
}