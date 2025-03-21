package jarvay.workpaper.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import jarvay.workpaper.Workpaper
import jarvay.workpaper.data.preferences.RunningPreferencesRepository
import jarvay.workpaper.data.preferences.SettingsPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class GenWallpaperReceiver : BroadcastReceiver() {
    @Inject
    lateinit var workpaper: Workpaper

    @Inject
    lateinit var runningPreferencesRepository: RunningPreferencesRepository

    @Inject
    lateinit var settingsPreferencesRepository: SettingsPreferencesRepository

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null || context == null) return

        if (intent.action != ACTION_NEXT_WALLPAPER) return

        MainScope().launch(Dispatchers.IO) {
            val next = workpaper.generateNextWallpaper() ?: return@launch
            workpaper.setNextWallpaper(
                next.copy(
                    isManual = true
                )
            )

            val settings = settingsPreferencesRepository.settingsPreferencesFlow.first()
            if (settings.enableNotification) {
                val notificationIntent = Intent(context, NotificationReceiver::class.java)
                notificationIntent.setAction(NotificationReceiver.ACTION_NOTIFICATION_UPDATE)
                context.sendBroadcast(notificationIntent)
            }
        }
    }

    companion object {
        const val ACTION_NEXT_WALLPAPER = "workpaper_action_next_wallpaper"
    }
}