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
import jarvay.workpaper.data.style.StyleRepository
import jarvay.workpaper.others.audioManager
import jarvay.workpaper.others.bitmapFromContentUri
import jarvay.workpaper.others.blur
import jarvay.workpaper.others.centerCrop
import jarvay.workpaper.others.effect
import jarvay.workpaper.others.getScreenSize
import jarvay.workpaper.others.getWallpaperSize
import jarvay.workpaper.others.info
import jarvay.workpaper.others.noise
import jarvay.workpaper.others.scaleFixedRatio
import jarvay.workpaper.receiver.NotificationReceiver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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
    lateinit var styleRepository: StyleRepository

    @Inject
    lateinit var runningPreferencesRepository: RunningPreferencesRepository

    @Inject
    lateinit var settingPreferencesRepository: SettingsPreferencesRepository

    private suspend fun setWallpaper(wallpaper: String, settings: SettingsPreferences) {
        workpaper.settingWallpaper.value = true
        Log.d(
            "context.audioManager().isMusicActive",
            (context.audioManager().isMusicActive).toString()
        )
        if (context.audioManager().isMusicActive && settings.disableWhenPlayingAudio) {
            return
        }

        val lastWallpaper =
            runningPreferencesRepository.runningPreferencesFlow.first().lastWallpaper
        if (lastWallpaper == wallpaper && !settings.useLiveWallpaper) return

        Log.d(javaClass.simpleName, settings.toString())

        val wallpaperUri = wallpaper.toUri()

        var bitmap = bitmapFromContentUri(wallpaperUri, applicationContext)

        val wallpaperSize = getWallpaperSize(applicationContext)

        Log.d("settings.useLiveWallpaper", settings.useLiveWallpaper.toString())

        bitmap?.let {
            Log.d("Wallpaper bitmap", it.info())

            val defaultStyle = styleRepository.findById(settings.defaultStyleId)
            val ruleWithRelation = runBlocking {
                workpaper.currentRuleWithRelation.first()
            }
            if (ruleWithRelation == null) return

            if (!ruleWithRelation.rule.noStyle) {
                val style = defaultStyle ?: ruleWithRelation.style
                style?.let {
                    if (style.blurRadius > 0) {
                        bitmap = bitmap!!.blur(style.blurRadius)
                    }
                    if (style.noisePercent > 0) {
                        bitmap = bitmap!!.noise(style.noisePercent)
                    }
                    bitmap = bitmap!!.effect(
                        brightness = style.brightness,
                        contrast = style.contrast,
                        saturation = style.saturation
                    )
                }
            }

            bitmap = it.scaleFixedRatio(
                targetWidth = wallpaperSize.width * if (settings.wallpaperScrollable) 2 else 1,
                targetHeight = wallpaperSize.height,
                useMin = !settings.useLiveWallpaper,
            )

            val wallpaperManager = WallpaperManager.getInstance(applicationContext)
            if (!settings.useLiveWallpaper) {
                if (settings.wallpaperScrollable) {
                    val screenSize = getScreenSize(context)
                    val targetRatio = (screenSize.width * 2).toFloat() / screenSize.height.toFloat()
                    val currentRatio = bitmap!!.width.toFloat() / bitmap!!.height.toFloat()
                    if (currentRatio > targetRatio) {
                        val targetHeight =
                            if (bitmap!!.height > screenSize.height) screenSize.height else bitmap!!.height
                        val targetWidth = (targetHeight * targetRatio).toInt()
                        bitmap = bitmap!!.centerCrop(
                            targetWidth = targetWidth,
                            targetHeight = targetHeight
                        )
                    }

                    wallpaperManager.setWallpaperOffsetSteps(
                        0f,
                        0f
                    )
                }

                if (!settings.alsoSetLockWallpaper) {
                    Log.d(javaClass.simpleName, "set system wallpaper only")
                    wallpaperManager.setBitmap(bitmap, null, false, WallpaperManager.FLAG_SYSTEM)
                } else {
                    Log.d(javaClass.simpleName, "set both wallpaper")
                    wallpaperManager.setBitmap(bitmap)
                }
            } else {
                while (!workpaper.liveWallpaperEngineCreated) {
                    Thread.sleep(100)
                }
                workpaper.currentBitmap.value = bitmap
            }
        }
        workpaper.settingWallpaper.value = false
    }

    private fun sendNotification() {
        val intent = Intent(context, NotificationReceiver::class.java)
        intent.setAction(NotificationReceiver.ACTION_NOTIFICATION_UPDATE)
        context.sendBroadcast(intent)
    }

    override suspend fun doWork(): Result {
        Log.d(javaClass.simpleName, "start")
        Log.d(javaClass.simpleName, Thread.currentThread().name)

        if (workpaper.settingWallpaper.value) {
            return Result.failure()
        }

        val runningPreferences: RunningPreferences =
            runningPreferencesRepository.runningPreferencesFlow.first()

        val settings = settingPreferencesRepository.settingsPreferencesFlow.first()

        Log.d(javaClass.simpleName, runningPreferences.toString())

        val ruleWithRelation = workpaper.currentRuleWithRelation.first() ?: return Result.failure()

        val nextWallpaper = workpaper.getNextWallpaper() ?: return Result.failure()

        workpaper.apply {
            generateNextWallpaper()?.let {
                setNextWallpaper(it)
            }
        }

        setWallpaper(wallpaper = nextWallpaper.contentUri, settings = settings)

        runningPreferencesRepository.apply {
            update(RunningPreferencesKeys.LAST_INDEX, nextWallpaper.index)
            update(RunningPreferencesKeys.LAST_WALLPAPER, nextWallpaper.contentUri)
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