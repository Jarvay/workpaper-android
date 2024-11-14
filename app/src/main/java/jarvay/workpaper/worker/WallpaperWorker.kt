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
import jarvay.workpaper.data.wallpaper.Wallpaper
import jarvay.workpaper.data.wallpaper.WallpaperType
import jarvay.workpaper.others.audioManager
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
    lateinit var runningPreferencesRepository: RunningPreferencesRepository

    @Inject
    lateinit var settingPreferencesRepository: SettingsPreferencesRepository

    private suspend fun setImageWallpaper(
        wallpaper: Wallpaper,
        settings: SettingsPreferences
    ): Boolean {
        Log.d(
            "context.audioManager().isMusicActive",
            (context.audioManager().isMusicActive).toString()
        )
        if (context.audioManager().isMusicActive && settings.disableWhenPlayingAudio) {
            return false
        }

        val lastWallpaper =
            runningPreferencesRepository.runningPreferencesFlow.first().lastWallpaper
        if (lastWallpaper == wallpaper.contentUri && !settings.useLiveWallpaper) return false

        Log.d(javaClass.simpleName, settings.toString())

        val wallpaperUri = wallpaper.contentUri.toUri()

        var bitmap = bitmapFromContentUri(wallpaperUri, applicationContext)

        val wallpaperSize = getWallpaperSize()

        Log.d("settings.useLiveWallpaper", settings.useLiveWallpaper.toString())

        bitmap?.let {
            bitmap = it.scaleFixedRatio(
                targetWidth = wallpaperSize.width,
                targetHeight = wallpaperSize.height,
                useMin = !settings.useLiveWallpaper,
            )
            Log.d("Wallpaper bitmap", it.info())

            val wallpaperManager = WallpaperManager.getInstance(applicationContext)
            if (!settings.useLiveWallpaper) {
                bitmap = workpaper.handleBitmapStyle(bitmap!!)

                if (!settings.alsoSetLockWallpaper) {
                    Log.d(javaClass.simpleName, "set system wallpaper only")
                    wallpaperManager.setBitmap(bitmap, null, false, WallpaperManager.FLAG_SYSTEM)
                } else {
                    Log.d(javaClass.simpleName, "set both wallpaper")
                    wallpaperManager.setBitmap(bitmap)
                }
            } else {
                workpaper.imageUri.value = wallpaper.contentUri
            }
        }

        return true
    }

    private suspend fun setVideoWallpaper(wallpaper: Wallpaper) {
        val currentContentUri =
            runningPreferencesRepository.runningPreferencesFlow.first().currentVideoContentUri
        if (wallpaper.contentUri == currentContentUri) {
            return
        }

        runningPreferencesRepository.update(
            RunningPreferencesKeys.CURRENT_VIDEO_CONTENT_URI,
            wallpaper.contentUri
        )

        workpaper.videoUri.value = wallpaper.contentUri
        Log.d("setVideoWallpaper", "setVideoWallpaper")
        return
    }

    private fun sendNotification() {
        val intent = Intent(context, NotificationReceiver::class.java)
        intent.setAction(NotificationReceiver.ACTION_NOTIFICATION_UPDATE)
        context.sendBroadcast(intent)
    }

    override suspend fun doWork(): Result {
        Log.d(javaClass.simpleName, "WallpaperWorker start")
        Log.d(javaClass.simpleName, Thread.currentThread().name)

        val runningPreferences: RunningPreferences =
            runningPreferencesRepository.runningPreferencesFlow.first()

        val settings = settingPreferencesRepository.settingsPreferencesFlow.first()

        Log.d(javaClass.simpleName, runningPreferences.toString())

        val ruleWithRelation = workpaper.currentRuleWithRelation.first()
        if (ruleWithRelation == null) {
            Log.d(javaClass.simpleName, "Current rule is null")
            return Result.failure()
        }

        val nextWallpaper = workpaper.getNextWallpaper()
        if (nextWallpaper == null) {
            Log.d(javaClass.simpleName, "Next wallpaper is null")
            return Result.failure()
        }

        workpaper.apply {
            generateNextWallpaper()?.let {
                setNextWallpaper(it)
            }
        }

        val success: Boolean
        when (nextWallpaper.wallpaper.type) {
            WallpaperType.IMAGE -> {
                runningPreferencesRepository.update(
                    RunningPreferencesKeys.CURRENT_VIDEO_CONTENT_URI,
                    ""
                )
                success = setImageWallpaper(
                    wallpaper = nextWallpaper.wallpaper,
                    settings = settings
                )
            }

            WallpaperType.VIDEO -> {
                success = if (settings.useLiveWallpaper) {
                    setVideoWallpaper(wallpaper = nextWallpaper.wallpaper)
                    true
                } else false
            }
        }

        if (!success) {
            return Result.failure()
        }

        runningPreferencesRepository.apply {
            update(RunningPreferencesKeys.LAST_INDEX, nextWallpaper.index)
            update(RunningPreferencesKeys.LAST_WALLPAPER, nextWallpaper.wallpaper.contentUri)
        }

        if (!nextWallpaper.isManual && ruleWithRelation.rule.changeByTiming) {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.MINUTE, ruleWithRelation.rule.interval)
            workpaper.nextWallpaperTime = calendar.timeInMillis
        } else if (!ruleWithRelation.rule.changeByTiming) {
            workpaper.nextWallpaperTime = 0
        }

        if (settings.enableNotification) {
            sendNotification()
        }

        return Result.success()
    }
}