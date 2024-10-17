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
import jarvay.workpaper.others.bitmapFromContentUri
import jarvay.workpaper.others.scaleFixedRatio
import jarvay.workpaper.receiver.RuleReceiver
import jarvay.workpaper.receiver.UpdateActionWidgetReceiver
import jarvay.workpaper.receiver.WallpaperReceiver
import jarvay.workpaper.service.LiveWallpaperService
import jarvay.workpaper.service.WorkpaperService
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

enum class AlarmType(val value: Int) {
    REPEAT(1),
    RULE(2)
}

data class NextWallpaper(
    var index: Int,
    var contentUri: String,
    var isManual: Boolean,
)

@Singleton
class Workpaper @Inject constructor(
    @ApplicationContext private val context: Context,
    val ruleRepository: RuleRepository,
) {
    @Inject
    lateinit var runningPreferencesRepository: RunningPreferencesRepository

    @Inject
    lateinit var settingsPreferencesRepository: SettingsPreferencesRepository

    var currentRuleId: MutableStateFlow<Long> = MutableStateFlow(-1)
    var currentRuleWithRelation: Flow<RuleWithRelation?> = MutableStateFlow(null)
    var nextRuleWithRelation: MutableStateFlow<RuleWithRelation?> = MutableStateFlow(null)

    var nextWallpaper: MutableStateFlow<NextWallpaper?> = MutableStateFlow(null)
    var nextWallpaperTime: Long = 0
    var nextRuleTime: Long = 0
    val nextWallpaperBitmap: MutableStateFlow<Bitmap?> = MutableStateFlow(null)

    val currentBitmap: MutableStateFlow<Bitmap?> = MutableStateFlow(null)

    var wallpaperContentUris: List<String> = emptyList()

    val settingWallpaper = MutableStateFlow(false)

    var liveWallpaperEngineCreated = false

    init {
        MainScope().launch {
            currentRuleId.collect {
                currentRuleWithRelation = ruleRepository.findRuleFlowById(it)!!.stateIn(
                    this
                )
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
            }

            val i = Intent(context, WorkpaperService::class.java)
            context.startService(i)
        }
    }

    suspend fun stop() {
        Log.d(javaClass.simpleName, "stop")

        currentRuleId.value = -1
        nextRuleWithRelation.value = null

        cancelAllAlarm()

        runningPreferencesRepository.apply {
            update(RunningPreferencesKeys.LAST_INDEX, -1)
            update(RunningPreferencesKeys.LAST_WALLPAPER, "")
        }

        currentBitmap.value = null

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
        if (nextWallpaper.value?.contentUri == next.contentUri) return
        nextWallpaper.value = next

        var bitmap = bitmapFromContentUri(next.contentUri.toUri(), context)
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

        if (wallpaperContentUris.isEmpty()) return null

        val nextIndex = nextIndex(index)

        return NextWallpaper(
            index = nextIndex,
            contentUri = wallpaperContentUris[nextIndex],
            isManual = isManual,
        )
    }

    private fun nextIndex(currentIndex: Int): Int {
        val ruleWithRelation = runBlocking {
            currentRuleWithRelation?.first()
        }

        if (currentIndex + 1 >= wallpaperContentUris.size) {
            if (ruleWithRelation?.rule?.random == true) {
                wallpaperContentUris = wallpaperContentUris.shuffled()
            }
            return 0
        } else {
            return currentIndex + 1
        }
    }
}