package jarvay.workpaper.service

import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.coroutineScope
import dagger.hilt.android.AndroidEntryPoint
import jarvay.workpaper.NextWallpaper
import jarvay.workpaper.Workpaper
import jarvay.workpaper.data.preferences.RunningPreferencesRepository
import jarvay.workpaper.data.preferences.SettingsPreferences
import jarvay.workpaper.data.preferences.SettingsPreferencesRepository
import jarvay.workpaper.data.rule.RuleAlbums
import jarvay.workpaper.data.rule.RuleRepository
import jarvay.workpaper.others.NotificationHelper
import jarvay.workpaper.others.prevRule
import jarvay.workpaper.receiver.NextRuleReceiver
import jarvay.workpaper.receiver.RuleReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class WorkpaperService @Inject constructor() : LifecycleService() {
    @Inject
    lateinit var ruleRepository: RuleRepository

    @Inject
    lateinit var settingsPreferencesRepository: SettingsPreferencesRepository

    @Inject
    lateinit var runningPreferencesRepository: RunningPreferencesRepository

    @Inject
    lateinit var workpaper: Workpaper

    @Inject
    lateinit var notificationHelper: NotificationHelper

    override fun onCreate() {
        super.onCreate()

        Log.d(javaClass.simpleName, Thread.currentThread().name)

        lifecycle.coroutineScope.launch(Dispatchers.Default) {
            val settings = settingsPreferencesRepository.settingsPreferencesFlow.first()
            if (settings.enableNotification) {
                val notification =
                    notificationHelper.notificationBuilder(
                        ongoing = settings.notificationOngoing
                    ).build()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    notificationHelper.createChannel()
                }
                startForeground(NotificationHelper.NOTIFICATION_ID, notification)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lifecycle.coroutineScope.launch(Dispatchers.Default) {
            val rules = ruleRepository.allRules.first().map {
                RuleAlbums(
                    rule = it.key,
                    albums = it.value
                )
            }

            val prevRule = prevRule(rules)

            Log.d("prev rule", prevRule.toString())

            val settings = settingsPreferencesRepository.settingsPreferencesFlow.first()
            if (prevRule != null && settings.startWithPrevRule) {
                val prevRuleIntent = Intent(this@WorkpaperService, RuleReceiver::class.java)
                prevRuleIntent.putExtra(RuleReceiver.RULE_ID_KEY, prevRule.ruleAlbums.rule.ruleId)
                sendBroadcast(prevRuleIntent)
            } else {
                val nextRuleIntent = Intent(this@WorkpaperService, NextRuleReceiver::class.java)
                sendBroadcast(nextRuleIntent)
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("onDestroy", javaClass.simpleName)

        lifecycle.coroutineScope.launch {
            workpaper.setNextWallpaper(
                NextWallpaper(
                    index = -1,
                    contentUri = "",
                    isManual = false,
                )
            )
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)

        Log.d("onTaskRemoved", javaClass.simpleName)
    }
}