package jarvay.workpaper.receiver

import android.app.AlarmManager
import android.app.Service.ALARM_SERVICE
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import jarvay.workpaper.AlarmType
import jarvay.workpaper.Workpaper
import jarvay.workpaper.data.preferences.RunningPreferencesKeys
import jarvay.workpaper.data.preferences.RunningPreferencesRepository
import jarvay.workpaper.data.rule.Rule
import jarvay.workpaper.data.rule.RuleRepository
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class RuleReceiver : BroadcastReceiver() {
    @Inject
    lateinit var ruleRepository: RuleRepository

    @Inject
    lateinit var runningPreferencesRepository: RunningPreferencesRepository

    @Inject
    lateinit var workpaper: Workpaper

    @OptIn(DelicateCoroutinesApi::class)
    override fun onReceive(context: Context?, intent: Intent?) {
        val ruleId = intent?.getLongExtra(RULE_ID_KEY, -1) ?: -1
        Log.d(javaClass.simpleName, ruleId.toString())
        val r = ruleRepository.getRuleWithAlbums(ruleId)

        if (ruleId > -1 && r != null) {
            context?.let {
                workpaper.cancelAlarm(type = AlarmType.REPEAT)

                if (r.rule.changeByTiming) {
                    startRepeatAlarm(context, r.rule)
                }

                Log.d("defaultPreferencesRepository", runningPreferencesRepository.toString())
                runningPreferencesRepository.let {
                    it.apply {
                        GlobalScope.launch {
                            update(RunningPreferencesKeys.CURRENT_RULE_ID, ruleId)
                            update(RunningPreferencesKeys.LAST_INDEX, -1)
                        }
                    }
                }
            }
        }

        val startNextRule = intent?.getBooleanExtra(ADD_NEXT_RULE_TO_ALARM_FLAG, true) ?: true

        if (startNextRule) {
            val i = Intent(context, NextRuleReceiver::class.java)
            context?.sendBroadcast(i)
        }
    }

    private fun startRepeatAlarm(context: Context, rule: Rule) {
        val i = Intent(context, WallpaperReceiver::class.java)
        context.sendBroadcast(i)
        val pendingIntent = workpaper.getPendingIntent(AlarmType.REPEAT)

        val interval = rule.interval * 60 * 1000
        val alarmManager: AlarmManager = context.getSystemService(ALARM_SERVICE) as AlarmManager
        try {
            alarmManager.setRepeating(
                AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + interval,
                interval.toLong(),
                pendingIntent
            )
            Log.d("alarmManager.setRepeating", interval.toString())
        } catch (e: SecurityException) {
            Log.e(javaClass.simpleName, e.toString())
        }
    }

    companion object {
        const val RULE_ID_KEY = "ruleId"
        const val ADD_NEXT_RULE_TO_ALARM_FLAG = "addNextRuleToAlarm"
    }
}