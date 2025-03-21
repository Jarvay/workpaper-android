package jarvay.workpaper.service

import android.content.Intent
import android.os.Build
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.coroutineScope
import dagger.hilt.android.AndroidEntryPoint
import jarvay.workpaper.Workpaper
import jarvay.workpaper.data.preferences.RunningPreferencesRepository
import jarvay.workpaper.data.preferences.SettingsPreferencesRepository
import jarvay.workpaper.data.rule.RuleRepository
import jarvay.workpaper.data.rule.RuleWithRelationToSort
import jarvay.workpaper.others.NotificationHelper
import jarvay.workpaper.others.prevRule
import jarvay.workpaper.receiver.NextRuleReceiver
import jarvay.workpaper.receiver.RuleReceiver
import jarvay.workpaper.receiver.UpdateActionWidgetReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@AndroidEntryPoint
@Singleton
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
        lifecycle.coroutineScope.launch {
            if (workpaper.currentRuleId.value > -1) return@launch

            val forcedRuleId: Long =
                settingsPreferencesRepository.settingsPreferencesFlow.first().forcedUsedRuleId
            val forcedRuleAlbums = ruleRepository.findRuleById(forcedRuleId)

            val list = ruleRepository.allRules.first()

            val prevRule = if (forcedRuleAlbums != null) {
                RuleWithRelationToSort(
                    ruleWithRelation = forcedRuleAlbums,
                    sortValue = 0,
                    day = 0
                )
            } else {
                prevRule(list)
            }

            val settings = settingsPreferencesRepository.settingsPreferencesFlow.first()
            if (prevRule != null && settings.startWithPrevRule) {
                val prevRuleIntent = Intent(this@WorkpaperService, RuleReceiver::class.java)
                prevRuleIntent.putExtra(
                    RuleReceiver.RULE_ID_KEY,
                    prevRule.ruleWithRelation.rule.ruleId
                )
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

        lifecycle.coroutineScope.launch {
            workpaper.nextWallpaper.value = null
            val intent = Intent(this@WorkpaperService, UpdateActionWidgetReceiver::class.java)
            sendBroadcast(intent)
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
    }
}