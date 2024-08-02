package jarvay.workpaper.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import jarvay.workpaper.data.day.RuleDao
import jarvay.workpaper.others.SharePreferenceKey
import jarvay.workpaper.others.defaultSharedPreferences
import jarvay.workpaper.others.getSettings
import jarvay.workpaper.others.prevRule
import jarvay.workpaper.receiver.NextRuleReceiver
import jarvay.workpaper.receiver.WallpaperReceiver
import javax.inject.Inject

@AndroidEntryPoint
class ScheduleService @Inject constructor() : Service() {
    @Inject
    lateinit var ruleDao: RuleDao

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val settings = getSettings(this)

        val rules = ruleDao.findAll()

        val prevRule = prevRule(rules)

        Log.d(javaClass.simpleName, prevRule.toString())


        if (prevRule != null && settings.startWithPrevRule) {
            defaultSharedPreferences(this).edit().apply {
                putLong(SharePreferenceKey.CURRENT_ALBUM_ID_KEY, prevRule.rule.albumId)
                putInt(SharePreferenceKey.LAST_INDEX_KEY, 0)
            }.apply()
            val wallpaperIntent = Intent(this, WallpaperReceiver::class.java)
            sendBroadcast(wallpaperIntent)
        }

        val nextRuleIntent = Intent(this, NextRuleReceiver::class.java)
        sendBroadcast(nextRuleIntent)

        val timerServiceIntent = Intent(this, TimerService::class.java)
        startService(timerServiceIntent)

        return super.onStartCommand(intent, flags, startId)
    }
}