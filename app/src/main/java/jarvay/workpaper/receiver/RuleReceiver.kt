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
import jarvay.workpaper.data.day.RuleDao
import jarvay.workpaper.data.preferences.DefaultPreferencesKeys
import jarvay.workpaper.data.preferences.DefaultPreferencesRepository
import jarvay.workpaper.data.rule.Rule
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class RuleReceiver : BroadcastReceiver() {
    @Inject
    lateinit var ruleDao: RuleDao

    @Inject
    lateinit var defaultPreferencesRepository: DefaultPreferencesRepository

    @OptIn(DelicateCoroutinesApi::class)
    override fun onReceive(context: Context?, intent: Intent?) {
        val ruleId = intent?.getLongExtra(RULE_ID_KEY, -1) ?: -1
        Log.d(javaClass.simpleName, ruleId.toString())
        val r = ruleDao.findWithAlbumById(ruleId)

        if (ruleId > -1 && r != null) {
            context?.let {
                Workpaper.cancelAlarm(type = AlarmType.REPEAT, context = context)

//                val sp = defaultSharedPreferences(context)
//                sp.edit().apply {
//                    putInt(SharedPreferencesKey.LAST_INDEX_KEY, -1)
//                    putLong(SharedPreferencesKey.CURRENT_RULE_ID_KEY, ruleId)
//                }.apply()

                startRepeatAlarm(context, r.rule)

                Log.d("defaultPreferencesRepository", defaultPreferencesRepository.toString())
                defaultPreferencesRepository.let {
                    it.apply {
                        GlobalScope.launch {
                            update(
                                DefaultPreferencesKeys.CURRENT_RULE_ID,
                                ruleId
                            )
                            update(
                                DefaultPreferencesKeys.LAST_INDEX,
                                -1
                            )
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
        val pendingIntent = Workpaper.getPendingIntent(AlarmType.REPEAT, context)

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