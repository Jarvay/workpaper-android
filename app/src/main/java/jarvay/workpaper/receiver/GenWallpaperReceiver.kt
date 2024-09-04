package jarvay.workpaper.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import jarvay.workpaper.Workpaper
import jarvay.workpaper.data.preferences.RunningPreferencesRepository
import jarvay.workpaper.data.preferences.SettingsPreferencesRepository
import jarvay.workpaper.data.rule.RuleRepository
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class GenWallpaperReceiver : BroadcastReceiver() {
    @Inject
    lateinit var workpaper: Workpaper

    @Inject
    lateinit var runningPreferencesRepository: RunningPreferencesRepository

    @OptIn(DelicateCoroutinesApi::class)
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(javaClass.simpleName, intent.toString())

        if (intent == null || context == null) return

        if (intent.action != ACTION_NEXT_WALLPAPER) return

        GlobalScope.launch(Dispatchers.IO) {
            val next = workpaper.generateNextWallpaper() ?: return@launch
            workpaper.setNextWallpaper(
                next.copy(
                    isManual = true
                )
            )

            val notificationIntent = Intent(context, NotificationReceiver::class.java)
            notificationIntent.setAction(NotificationReceiver.ACTION_NOTIFICATION_UPDATE)
            context.sendBroadcast(notificationIntent)
        }
    }

    companion object {
        const val ACTION_NEXT_WALLPAPER = "workpaper_action_next_wallpaper"
    }
}