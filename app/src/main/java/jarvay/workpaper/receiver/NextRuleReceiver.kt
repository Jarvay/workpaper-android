package jarvay.workpaper.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.icu.util.Calendar
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import jarvay.workpaper.AlarmType
import jarvay.workpaper.data.preferences.RunningPreferencesKeys
import jarvay.workpaper.data.preferences.RunningPreferencesRepository
import jarvay.workpaper.data.rule.RuleAlbums
import jarvay.workpaper.data.rule.RuleDao
import jarvay.workpaper.others.getCalendarWithRule
import jarvay.workpaper.others.nextRule
import jarvay.workpaper.receiver.RuleReceiver.Companion.RULE_ID_KEY
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@AndroidEntryPoint
class NextRuleReceiver : BroadcastReceiver() {
    @Inject
    lateinit var ruleDao: RuleDao

    @Inject
    lateinit var runningPreferencesRepository: RunningPreferencesRepository

    @OptIn(DelicateCoroutinesApi::class)
    override fun onReceive(context: Context?, intent: Intent?) {
        val list = ruleDao.findAll().map {
            RuleAlbums(
                rule = it.key,
                albums = it.value
            )
        }
        val nextRule = nextRule(list)
        Log.d("nextRule", nextRule.toString())

        if (intent == null || context == null || nextRule == null) return

        val calendar = getCalendarWithRule(nextRule.ruleAlbums.rule, nextRule.day)
        if (calendar.before(Calendar.getInstance())) {
            calendar.add(Calendar.DATE, 7);
        }
        Log.d("nextRule time", Date(calendar.timeInMillis).toString())


        val i = Intent(context, RuleReceiver::class.java)
        i.putExtra(RULE_ID_KEY, nextRule.ruleAlbums.rule.ruleId)
        val pendingIntent: PendingIntent = PendingIntent.getBroadcast(
            context,
            AlarmType.RULE.value,
            i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        Log.d("calendar", Date(calendar.timeInMillis).toString())

        val alarmManager: AlarmManager =
            context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        try {
            alarmManager.setExact(
                AlarmManager.RTC,
                calendar.timeInMillis,
                pendingIntent
            )
            Log.d("Added next rule to alarm", nextRule.toString())
        } catch (e: SecurityException) {
            Log.e(javaClass.simpleName, e.toString())
        }

        // update preferences
        GlobalScope.launch {
            runningPreferencesRepository.update(
                RunningPreferencesKeys.NEXT_RULE_ID,
                nextRule.ruleAlbums.rule.ruleId
            )
        }
    }
}