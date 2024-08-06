package jarvay.workpaper.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import jarvay.workpaper.AlarmType
import jarvay.workpaper.data.day.RuleDao
import jarvay.workpaper.data.preferences.DefaultPreferencesKeys
import jarvay.workpaper.data.preferences.DefaultPreferencesRepository
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
    lateinit var defaultPreferencesRepository: DefaultPreferencesRepository

    @OptIn(DelicateCoroutinesApi::class)
    override fun onReceive(context: Context?, intent: Intent?) {
        val nextRule = nextRule(ruleDao.findAll())
        if (intent == null || context == null || nextRule == null) return

        Log.d("nextRule", nextRule.toString())

        val calendar = getCalendarWithRule(nextRule.ruleWithAlbum.rule, nextRule.day)
        val i = Intent(context, RuleReceiver::class.java)
        i.putExtra(RULE_ID_KEY, nextRule.ruleWithAlbum.rule.ruleId)
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
            defaultPreferencesRepository.update(
                DefaultPreferencesKeys.NEXT_RULE_ID,
                nextRule.ruleWithAlbum.rule.ruleId
            )
        }
    }
}