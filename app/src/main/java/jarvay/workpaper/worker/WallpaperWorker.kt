package jarvay.workpaper.worker

import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.net.toUri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import jarvay.workpaper.Workpaper
import jarvay.workpaper.data.preferences.RunningPreferences
import jarvay.workpaper.data.preferences.RunningPreferencesKeys
import jarvay.workpaper.data.preferences.RunningPreferencesRepository
import jarvay.workpaper.data.preferences.SettingsPreferences
import jarvay.workpaper.data.preferences.SettingsPreferencesRepository
import jarvay.workpaper.data.rule.RuleRepository
import jarvay.workpaper.others.bitmapFromContentUri
import jarvay.workpaper.others.getWallpaperSize
import jarvay.workpaper.others.info
import jarvay.workpaper.others.scaleFixedRatio
import jarvay.workpaper.receiver.NotificationReceiver
import kotlinx.coroutines.flow.first
import java.util.Calendar
import javax.inject.Inject

@HiltWorker
class WallpaperWorker @AssistedInject constructor(
    @Assisted val context: Context,
    @Assisted params: WorkerParameters,
) : CoroutineWorker(context, params) {
    @Inject
    lateinit var workpaper: Workpaper

    @Inject
    lateinit var ruleRepository: RuleRepository

    @Inject
    lateinit var runningPreferencesRepository: RunningPreferencesRepository

    @Inject
    lateinit var settingPreferencesRepository: SettingsPreferencesRepository

    private fun setWallpaper(wallpaper: String, settings: SettingsPreferences) {
        Log.d(javaClass.simpleName, settings.toString())

        val wallpaperUri = wallpaper.toUri()

        var bitmap = bitmapFromContentUri(wallpaperUri, applicationContext) ?: return

        val wallpaperSize = getWallpaperSize(applicationContext)

        if (bitmap.width > wallpaperSize.width || bitmap.height > wallpaperSize.height) {
            bitmap = bitmap.scaleFixedRatio(
                targetWidth = wallpaperSize.width,
                targetHeight = wallpaperSize.height
            )
        }
        Log.d("Wallpaper bitmap", bitmap.info())

        val wallpaperManager = WallpaperManager.getInstance(applicationContext)
        if (!settings.alsoSetLockWallpaper) {
            Log.d(javaClass.simpleName, "set system wallpaper only")
            wallpaperManager.setBitmap(bitmap, null, false, WallpaperManager.FLAG_SYSTEM)
        } else {
            Log.d(javaClass.simpleName, "set both wallpaper")
            wallpaperManager.setBitmap(bitmap)
        }
    }

    private fun sendNotification() {
        val intent = Intent(context, NotificationReceiver::class.java)
        intent.setAction(NotificationReceiver.ACTION_NOTIFICATION_UPDATE)
        context.sendBroadcast(intent)
    }

    override suspend fun doWork(): Result {
        Log.d(javaClass.simpleName, "start")
        Log.d(javaClass.simpleName, Thread.currentThread().name)

        val runningPreferences: RunningPreferences =
            runningPreferencesRepository.runningPreferencesFlow.first()

        val settings = settingPreferencesRepository.settingsPreferencesFlow.first()

        Log.d(javaClass.simpleName, runningPreferences.toString())
        val lastWallpaper = runningPreferences.lastWallpaper

        val ruleWithAlbums = workpaper.currentRuleAlbums.value ?: return Result.failure()

        val nextWallpaper = workpaper.getNextWallpaper() ?: return Result.failure()
        if (lastWallpaper != nextWallpaper.contentUri) {
            setWallpaper(wallpaper = nextWallpaper.contentUri, settings = settings)
        }

        runningPreferencesRepository.apply {
            update(RunningPreferencesKeys.LAST_INDEX, nextWallpaper.index)
            update(RunningPreferencesKeys.LAST_WALLPAPER, nextWallpaper.contentUri)
        }

        if (workpaper.getNextWallpaper()?.isManual == false && ruleWithAlbums.rule.changeByTiming) {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.MINUTE, ruleWithAlbums.rule.interval)
            workpaper.nextWallpaperTime = calendar.timeInMillis
        } else if(!ruleWithAlbums.rule.changeByTiming) {
            workpaper.nextWallpaperTime = 0
        }

        if (settings.enableNotification) {
            sendNotification()
        }

        return Result.success()
    }
}