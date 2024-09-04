package jarvay.workpaper.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import jarvay.workpaper.Workpaper
import jarvay.workpaper.data.preferences.SettingsPreferencesRepository
import jarvay.workpaper.others.NotificationHelper
import jarvay.workpaper.others.setBigPicture
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NotificationReceiver : BroadcastReceiver() {
    @Inject
    lateinit var settingsPreferencesRepository: SettingsPreferencesRepository

    @Inject
    lateinit var workpaper: Workpaper

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @OptIn(DelicateCoroutinesApi::class)
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(javaClass.simpleName, "onReceive")

        if (context == null || intent == null) return

        GlobalScope.launch {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val settingsPreferences = settingsPreferencesRepository.settingsPreferencesFlow.first()
            val notificationBuilder = notificationHelper.notificationBuilder(
                ongoing = settingsPreferences.notificationOngoing
            )

            val titleAndText = notificationHelper.titleAndText()
            notificationBuilder.setContentTitle(titleAndText.first)
                .setContentText(titleAndText.second)

            val showChangeNow = workpaper.nextRuleTime >= workpaper.nextWallpaperTime

            workpaper.getNextWallpaper()?.let {
                val bitmapPair =
                    notificationHelper.createPictures(it.contentUri) ?: return@launch
                notificationBuilder.setBigPicture(
                    background = bitmapPair.first,
                    bitmap = bitmapPair.second,
                    context = context,
                    showChangeNow = showChangeNow
                )
            }

            val notification = notificationBuilder.build()
            notificationManager.notify(NotificationHelper.NOTIFICATION_ID, notification)
        }
    }

    companion object {
        const val ACTION_NOTIFICATION_UPDATE = "workpaper_action_notification_update"
    }
}