package jarvay.workpaper

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import jarvay.workpaper.data.preferences.RunningPreferences
import jarvay.workpaper.data.preferences.RunningPreferencesKeys
import jarvay.workpaper.data.preferences.RunningPreferencesRepository
import jarvay.workpaper.receiver.RuleReceiver
import jarvay.workpaper.receiver.WallpaperReceiver
import jarvay.workpaper.service.ScheduleService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

enum class AlarmType(val value: Int) {
    REPEAT(1),
    RULE(2)
}

class Workpaper(private val context: Context) {
    fun start(scope: CoroutineScope) {
        scope.launch {
            val i = Intent(context, ScheduleService::class.java)
            context.startService(i)
        }
    }

    fun stop(scope: CoroutineScope, runningPreferencesRepository: RunningPreferencesRepository) {
        Log.d(javaClass.simpleName, "stop")

        cancelAllAlarm()

        scope.launch {
            runningPreferencesRepository.apply {
                update(RunningPreferencesKeys.CURRENT_RULE_ID, -1)
                update(RunningPreferencesKeys.LAST_INDEX, -1)
                update(RunningPreferencesKeys.LAST_WALLPAPER, "")
                update(RunningPreferencesKeys.NEXT_RULE_ID, -1)
            }
        }
    }

    fun cancelAllAlarm() {
        AlarmType.entries.forEach {
            cancelAlarm(it)
        }
    }

    fun getIntent(type: AlarmType): Intent {
        val i = Intent()
        return i.apply {
            when (type) {
                AlarmType.REPEAT -> {
                    i.setClass(context, WallpaperReceiver::class.java)
                }

                AlarmType.RULE -> {
                    i.setClass(context, RuleReceiver::class.java)
                }
            }
        }
    }

    fun getPendingIntent(type: AlarmType): PendingIntent {
        return PendingIntent.getBroadcast(
            context,
            type.value,
            getIntent(type = type),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun cancelAlarm(type: AlarmType) {
        Log.d("cancelAlarm", type.toString())

        val pendingIntent = getPendingIntent(type)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    fun isRunning(
        runningPreferences: Flow<RunningPreferences>,
    ): Boolean {
        return runBlocking {
            runningPreferences.first().currentRuleId > -1
        }
    }

    fun isAlarmExist(type: AlarmType): Boolean {
        val intent = getIntent(type)
        return PendingIntent.getBroadcast(
            context,
            type.value,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) != null
    }
}