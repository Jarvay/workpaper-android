package jarvay.workpaper

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import jarvay.workpaper.receiver.RuleReceiver
import jarvay.workpaper.receiver.WallpaperReceiver
import jarvay.workpaper.service.ScheduleService

enum class AlarmType(val value: Int) {
    REPEAT(1),
    RULE(2)
}

data object Workpaper {
    fun start(context: Context) {
        val i = Intent(context, ScheduleService::class.java)
        context.startService(i)
    }

    fun cancelAllAlarm(context: Context) {
        AlarmType.entries.forEach {
            cancelAlarm(it, context)
        }
    }

    private fun getIntent(type: AlarmType, context: Context): Intent {
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

     fun getPendingIntent(type: AlarmType, context: Context): PendingIntent {
        return PendingIntent.getBroadcast(
            context,
            type.value,
            getIntent(type = type, context = context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun cancelAlarm(type: AlarmType, context: Context) {
        Log.d("cancelAlarm", type.toString())

        val pendingIntent = getPendingIntent(type, context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }

    fun isAlarmExist(type: AlarmType, context: Context): Boolean {
        val intent = getIntent(type, context)
        return PendingIntent.getService(
            context,
            type.value,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) != null
    }
}