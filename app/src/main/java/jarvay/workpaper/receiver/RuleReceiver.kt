package jarvay.workpaper.receiver

import android.app.AlarmManager
import android.app.Service.ALARM_SERVICE
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.blankj.utilcode.util.LogUtils
import dagger.hilt.android.AndroidEntryPoint
import jarvay.workpaper.AlarmType
import jarvay.workpaper.Workpaper
import jarvay.workpaper.data.album.AlbumWithWallpapers
import jarvay.workpaper.data.preferences.RunningPreferencesKeys
import jarvay.workpaper.data.preferences.RunningPreferencesRepository
import jarvay.workpaper.data.rule.Rule
import jarvay.workpaper.data.rule.RuleRepository
import jarvay.workpaper.data.wallpaper.Wallpaper
import jarvay.workpaper.others.LOG_TAG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
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

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val ruleId = intent.getLongExtra(RULE_ID_KEY, -1)
        val ruleWithRelation = ruleRepository.findRuleById(ruleId)

        if (ruleWithRelation == null) {
            LogUtils.i(LOG_TAG, "Can not find rule where id=$ruleId")
            return
        }

        workpaper.currentRuleId.value = ruleId

        workpaper.cancelAlarm(type = AlarmType.REPEAT)

        workpaper.wallpapers =
            ruleWithRelation.albums.fold<AlbumWithWallpapers, MutableList<Wallpaper>>(
                mutableListOf()
            ) { acc, album ->
                acc.addAll(album.wallpapers.map { it })
                acc
            }.apply {
                if (ruleWithRelation.rule.random) shuffle()
            }

        MainScope().launch(Dispatchers.IO) {
            runningPreferencesRepository.update(RunningPreferencesKeys.LAST_INDEX, -1)

            sendWallpaperBroadcast(context)
            if (ruleWithRelation.rule.changeByTiming) {
                startRepeatAlarm(context, ruleWithRelation.rule)
            }
        }

        val i = Intent(context, NextRuleReceiver::class.java)
        context.sendBroadcast(i)
    }

    private suspend fun sendWallpaperBroadcast(context: Context) {
        val i = Intent(context, WallpaperReceiver::class.java)
        val nextWallpaper = workpaper.generateNextWallpaper()
        if (nextWallpaper != null) {
            workpaper.setNextWallpaper(nextWallpaper)
        }
        context.sendBroadcast(i)
    }

    private fun startRepeatAlarm(context: Context, rule: Rule) {
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
            LogUtils.i(LOG_TAG, "startRepeatAlarm")
        } catch (e: SecurityException) {
            LogUtils.e(LOG_TAG, "Can not set wallpaper alarm", e.toString())
        }
    }

    companion object {
        const val RULE_ID_KEY = "ruleId"
    }
}