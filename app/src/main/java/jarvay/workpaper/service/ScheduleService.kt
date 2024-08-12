package jarvay.workpaper.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.asLiveData
import dagger.hilt.android.AndroidEntryPoint
import jarvay.workpaper.R
import jarvay.workpaper.Workpaper
import jarvay.workpaper.data.preferences.DEFAULT_SETTINGS
import jarvay.workpaper.data.preferences.RunningPreferencesRepository
import jarvay.workpaper.data.preferences.SettingsPreferences
import jarvay.workpaper.data.preferences.SettingsPreferencesRepository
import jarvay.workpaper.data.rule.RuleAlbums
import jarvay.workpaper.data.rule.RuleDao
import jarvay.workpaper.data.rule.RuleRepository
import jarvay.workpaper.others.prevRule
import jarvay.workpaper.receiver.NextRuleReceiver
import jarvay.workpaper.receiver.RuleReceiver
import javax.inject.Inject

@AndroidEntryPoint
class ScheduleService @Inject constructor() : Service() {
    @Inject
    lateinit var ruleDao: RuleDao

    @Inject
    lateinit var ruleRepository: RuleRepository

    @Inject
    lateinit var settingsPreferencesRepository: SettingsPreferencesRepository

    @Inject
    lateinit var runningPreferencesRepository: RunningPreferencesRepository

    lateinit var settings: SettingsPreferences

    @Inject
    lateinit var workpaper: Workpaper

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.app_name),
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

    }

    override fun onCreate() {
        super.onCreate()

        settings = settingsPreferencesRepository.settingsPreferencesFlow.asLiveData().value
            ?: DEFAULT_SETTINGS
        if (settings.enableNotification) {
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .build()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                createNotificationChannel()
                startForeground(ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK, notification)
            }

        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val rules = ruleDao.findAll().map {
            RuleAlbums(
                rule = it.key,
                albums = it.value
            )
        }

        val prevRule = prevRule(rules)

        Log.d("prev rule", prevRule.toString())

        if (prevRule != null && settings.startWithPrevRule) {
            val prevRuleIntent = Intent(this, RuleReceiver::class.java)
            prevRuleIntent.putExtra(RuleReceiver.ADD_NEXT_RULE_TO_ALARM_FLAG, false)
            prevRuleIntent.putExtra(RuleReceiver.RULE_ID_KEY, prevRule.ruleAlbums.rule.ruleId)
            sendBroadcast(prevRuleIntent)
        }

        val nextRuleIntent = Intent(this, NextRuleReceiver::class.java)
        sendBroadcast(nextRuleIntent)

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
    }

    companion object {
        const val CHANNEL_ID = "workpaper_channel"
    }

}