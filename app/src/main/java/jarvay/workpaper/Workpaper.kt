package jarvay.workpaper

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import jarvay.workpaper.data.preferences.RunningPreferencesKeys
import jarvay.workpaper.data.preferences.RunningPreferencesRepository
import jarvay.workpaper.data.preferences.SettingsPreferencesRepository
import jarvay.workpaper.data.rule.RuleRepository
import jarvay.workpaper.data.rule.RuleWithRelation
import jarvay.workpaper.data.style.StyleRepository
import jarvay.workpaper.data.wallpaper.Wallpaper
import jarvay.workpaper.data.wallpaper.WallpaperType
import jarvay.workpaper.others.bitmapFromContentUri
import jarvay.workpaper.others.blur
import jarvay.workpaper.others.coverBitmapFromContentUri
import jarvay.workpaper.others.effect
import jarvay.workpaper.others.noise
import jarvay.workpaper.others.scaleFixedRatio
import jarvay.workpaper.receiver.RuleReceiver
import jarvay.workpaper.receiver.UpdateActionWidgetReceiver
import jarvay.workpaper.receiver.WallpaperReceiver
import jarvay.workpaper.service.LiveWallpaperService
import jarvay.workpaper.service.WorkpaperService
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

enum class AlarmType(val value: Int) {
    REPEAT(1),
    RULE(2)
}

data class NextWallpaper(
    var index: Int,
    var wallpaper: Wallpaper,
    var isManual: Boolean,
)

@Singleton
class Workpaper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    @Inject
    lateinit var ruleRepository: RuleRepository

    @Inject
    lateinit var runningPreferencesRepository: RunningPreferencesRepository

    @Inject
    lateinit var settingsPreferencesRepository: SettingsPreferencesRepository

    @Inject
    lateinit var styleRepository: StyleRepository

    val currentRuleId: MutableStateFlow<Long> = MutableStateFlow(-1)
    val currentRuleWithRelation = MutableStateFlow<RuleWithRelation?>(null)
    var nextRuleWithRelation: MutableStateFlow<RuleWithRelation?> = MutableStateFlow(null)

    var nextWallpaper: MutableStateFlow<NextWallpaper?> = MutableStateFlow(null)
    var nextWallpaperTime: Long = 0
    var nextRuleTime: Long = 0
    val nextWallpaperBitmap: MutableStateFlow<Bitmap?> = MutableStateFlow(null)

    var wallpapers: List<Wallpaper> = emptyList()

    var lastWallpaperWorkerId: UUID? = null

    val imageUri = MutableStateFlow<String?>(null)
    val videoUri = MutableStateFlow<String?>(null)

    init {
        MainScope().launch {
            currentRuleId.collect {
                currentRuleWithRelation.value = ruleRepository.findRuleById(ruleId = it)
            }
        }
    }

    suspend fun restart() {
        stop()

        start()
    }

    fun start() {
        Log.d(javaClass.simpleName, "start")

        MainScope().launch {
            if (settingsPreferencesRepository.settingsPreferencesFlow.first().useLiveWallpaper) {
                context.startActivity(
                    Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
                        .putExtra(
                            WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                            ComponentName(
                                context,
                                LiveWallpaperService::class.java
                            )
                        )
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } else {
                val i = Intent(context, WorkpaperService::class.java)
                context.startService(i)
            }
        }
    }

    suspend fun stop() {
        Log.d(javaClass.simpleName, "stop")

        currentRuleId.value = -1
        nextRuleWithRelation.value = null

        imageUri.value = null
        videoUri.value = null

        cancelAllAlarm()

        runningPreferencesRepository.apply {
            update(RunningPreferencesKeys.LAST_INDEX, -1)
            update(RunningPreferencesKeys.LAST_WALLPAPER, "")
            update(RunningPreferencesKeys.CURRENT_VIDEO_CONTENT_URI, "")
        }

        nextWallpaper.value = null
        nextWallpaperBitmap.value = null
        val i = Intent(context, UpdateActionWidgetReceiver::class.java)
        context.sendBroadcast(i)

        val intent = Intent(context, WorkpaperService::class.java)
        context.stopService(intent)
    }

    private fun cancelAllAlarm() {
        AlarmType.entries.forEach {
            cancelAlarm(it)
        }
    }

    private fun getIntent(type: AlarmType): Intent {
        val i = Intent()
        return i.apply {
            when (type) {
                AlarmType.REPEAT -> {
                    i.setClass(context, WallpaperReceiver::class.java)
                }

                AlarmType.RULE -> {
                    i.setClass(context, RuleReceiver::class.java)
                }
            }
        }
    }

    fun getPendingIntent(type: AlarmType): PendingIntent {
        return PendingIntent.getBroadcast(
            context,
            type.value,
            getIntent(type = type),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun cancelAlarm(type: AlarmType) {
        Log.d("cancelAlarm", type.toString())

        val pendingIntent = getPendingIntent(type)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    suspend fun isRunning(
    ): Boolean {
        return runningPreferencesRepository.runningPreferencesFlow.first().running
    }

    suspend fun getNextWallpaper(): NextWallpaper? {
        return if (nextWallpaper.value == null) generateNextWallpaper() else nextWallpaper.value
    }

    fun setNextWallpaper(next: NextWallpaper) {
        if (nextWallpaper.value?.wallpaper == next.wallpaper) return
        nextWallpaper.value = next

        var bitmap = when (next.wallpaper.type) {
            WallpaperType.IMAGE -> bitmapFromContentUri(next.wallpaper.contentUri.toUri(), context)
            WallpaperType.VIDEO -> coverBitmapFromContentUri(
                next.wallpaper.contentUri.toUri(),
                context
            )
        }
        if (bitmap != null) {
            bitmap = bitmap.scaleFixedRatio(320, 320)
            nextWallpaperBitmap.value = bitmap

            val intent = Intent(context, UpdateActionWidgetReceiver::class.java)
            context.sendBroadcast(intent)
        }
    }

    suspend fun generateNextWallpaper(
        startIndex: Int? = null,
        isManual: Boolean = false,
        ruleId: Long? = null
    ): NextWallpaper? {
        val runningPreferences = runningPreferencesRepository.runningPreferencesFlow.first()

        Log.d(javaClass.simpleName, runningPreferences.toString())
        val index = startIndex ?: nextWallpaper.value?.index ?: -1
        val tmpRuleId = ruleId ?: this.currentRuleId.value

        if (tmpRuleId < 0) return null

        if (wallpapers.isEmpty()) return null

        val nextIndex = nextIndex(index)

        return NextWallpaper(
            index = nextIndex,
            wallpaper = wallpapers[nextIndex],
            isManual = isManual,
        )
    }

    suspend fun handleBitmapStyle(bitmap: Bitmap): Bitmap {
        var result = bitmap.copy(Bitmap.Config.ARGB_8888, true)

        val settings = settingsPreferencesRepository.settingsPreferencesFlow.first()

        val defaultStyle = styleRepository.findById(settings.defaultStyleId)
        val ruleWithRelation = currentRuleWithRelation.first() ?: return result

        if (!ruleWithRelation.rule.noStyle) {
            val style = defaultStyle ?: ruleWithRelation.style
            style?.let {
                if (style.blurRadius > 0) {
                    result = bitmap.blur(style.blurRadius)
                }
                if (style.noisePercent > 0) {
                    result = bitmap.noise(style.noisePercent)
                }
                result = result.effect(
                    brightness = style.brightness,
                    contrast = style.contrast,
                    saturation = style.saturation
                )
            }
        }

        return result
    }

    private fun nextIndex(currentIndex: Int): Int {
        val ruleWithRelation = runBlocking {
            currentRuleWithRelation.first()
        }

        if (currentIndex + 1 >= wallpapers.size) {
            if (ruleWithRelation?.rule?.random == true) {
                wallpapers = wallpapers.shuffled()
            }
            return 0
        } else {
            return currentIndex + 1
        }
    }
}