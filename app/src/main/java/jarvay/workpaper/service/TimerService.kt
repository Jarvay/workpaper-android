package jarvay.workpaper.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import jarvay.workpaper.data.album.AlbumDao
import jarvay.workpaper.others.DEFAULT_WALLPAPER_CHANGE_INTERVAL
import jarvay.workpaper.others.SharePreferenceKey
import jarvay.workpaper.others.getSettings
import jarvay.workpaper.receiver.WallpaperReceiver
import java.util.Date
import javax.inject.Inject

@AndroidEntryPoint
class TimerService @Inject constructor() : Service() {
    @Inject
    lateinit var albumDao: AlbumDao

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val sp = getSharedPreferences(SharePreferenceKey.SHARED_PREFERENCE_NAME, MODE_PRIVATE)

        val i = Intent(this, WallpaperReceiver::class.java)
        i.setAction(WallpaperReceiver.ACTION_UPDATE_WALLPAPER)
        sendBroadcast(i)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, i, PendingIntent.FLAG_IMMUTABLE)

        val settings = getSettings(this)
        val interval = settings.interval * 1000
        val alarmManager: AlarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        try {
            alarmManager.setRepeating(
                AlarmManager.RTC,
                Date().time,
                interval.toLong(),
                pendingIntent
            )
        } catch (e: SecurityException) {
            Log.e(javaClass.simpleName, e.toString())
        }

        val stopIntent = Intent(this, TimerService::class.java)
        stopService(stopIntent)

        return super.onStartCommand(intent, flags, startId)
    }
}