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
import jarvay.workpaper.data.album.Album
import jarvay.workpaper.data.preferences.RunningPreferencesKeys
import jarvay.workpaper.data.preferences.RunningPreferencesRepository
import jarvay.workpaper.data.rule.Rule
import jarvay.workpaper.data.rule.RuleRepository
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
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
        if (context == null || intent == null) return

        val ruleId = intent.getLongExtra(RULE_ID_KEY, -1)
        Log.d(javaClass.simpleName, ruleId.toString())
        val ruleAlbums = ruleRepository.getRuleWithAlbums(ruleId)

        workpaper.currentRuleAlbums.value = ruleAlbums

        if (ruleId > -1 && ruleAlbums != null) {
            workpaper.cancelAlarm(type = AlarmType.REPEAT)

            workpaper.wallpaperContentUris =
                ruleAlbums.albums.fold<Album, MutableList<String>>(mutableListOf()) { acc, album ->
                    acc.addAll(album.wallpaperUris)
                    acc
                }.apply {
                    if (ruleAlbums.rule.random) shuffle()
                }


            Log.d("defaultPreferencesRepository", runningPreferencesRepository.toString())
            GlobalScope.launch(Dispatchers.IO) {
                runningPreferencesRepository.update(RunningPreferencesKeys.LAST_INDEX, -1)

                sendWallpaperBroadcast(context)
                if (ruleAlbums.rule.changeByTiming) {
                    startRepeatAlarm(context, ruleAlbums.rule)
                }
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
            Log.d("alarmManager.setRepeating", interval.toString())
        } catch (e: SecurityException) {
            Log.e(javaClass.simpleName, e.toString())
        }
    }

    companion object {
        const val RULE_ID_KEY = "ruleId"
    }
}