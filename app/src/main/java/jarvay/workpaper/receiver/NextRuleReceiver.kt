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
import jarvay.workpaper.Workpaper
import jarvay.workpaper.data.preferences.RunningPreferencesRepository
import jarvay.workpaper.data.preferences.SettingsPreferencesRepository
import jarvay.workpaper.data.rule.RuleDao
import jarvay.workpaper.data.rule.RuleRepository
import jarvay.workpaper.data.rule.RuleWithRelationToSort
import jarvay.workpaper.others.getCalendarWithRule
import jarvay.workpaper.others.nextRule
import jarvay.workpaper.receiver.RuleReceiver.Companion.RULE_ID_KEY
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Date
import javax.inject.Inject

@AndroidEntryPoint
class NextRuleReceiver : BroadcastReceiver() {
    @Inject
    lateinit var ruleDao: RuleDao

    @Inject
    lateinit var ruleRepository: RuleRepository

    @Inject
    lateinit var runningPreferencesRepository: RunningPreferencesRepository

    @Inject
    lateinit var settingsPreferencesRepository: SettingsPreferencesRepository

    @Inject
    lateinit var workpaper: Workpaper

    override fun onReceive(context: Context?, intent: Intent?) {
        val forcedRuleId: Long = runBlocking {
            settingsPreferencesRepository.settingsPreferencesFlow.first().forcedUsedRuleId
        }
        val forcedRuleWithRelation = ruleRepository.findRuleById(forcedRuleId)

        val list = ruleDao.findAll()
        val nextRule = if (forcedRuleWithRelation != null) {
            RuleWithRelationToSort(
                ruleWithRelation = forcedRuleWithRelation,
                sortValue = 0,
                day = 0
            )
        } else {
            nextRule(list)
        }
        Log.d("nextRule", nextRule.toString())

        if (intent == null || context == null || nextRule == null) return

        val calendar = getCalendarWithRule(nextRule.ruleWithRelation.rule, nextRule.day)
        val now = Calendar.getInstance().timeInMillis
        if (calendar.timeInMillis <= now) {
            calendar.add(Calendar.DATE, 7)
        }
        Log.d("nextRule time", Date(calendar.timeInMillis).toString())
        Log.d("nextRule timeInMillis", calendar.timeInMillis.toString())
        Log.d("now timeInMillis", now.toString())

        workpaper.nextRuleId.value = nextRule.ruleWithRelation.rule.ruleId

        val i = Intent(context, RuleReceiver::class.java)
        i.putExtra(RULE_ID_KEY, nextRule.ruleWithRelation.rule.ruleId)
        val pendingIntent: PendingIntent = PendingIntent.getBroadcast(
            context,
            AlarmType.RULE.value,
            i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

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

        workpaper.nextRuleTime = calendar.timeInMillis
    }
}