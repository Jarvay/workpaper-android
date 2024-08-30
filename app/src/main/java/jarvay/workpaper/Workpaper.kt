package jarvay.workpaper

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import jarvay.workpaper.data.preferences.RunningPreferencesKeys
import jarvay.workpaper.data.preferences.RunningPreferencesRepository
import jarvay.workpaper.data.rule.RuleAlbums
import jarvay.workpaper.data.rule.RuleRepository
import jarvay.workpaper.receiver.RuleReceiver
import jarvay.workpaper.receiver.WallpaperReceiver
import jarvay.workpaper.service.WorkpaperService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

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
) {
    @Inject
    lateinit var runningPreferencesRepository: RunningPreferencesRepository

    @Inject
    lateinit var ruleRepository: RuleRepository

    var currentRuleAlbums: MutableStateFlow<RuleAlbums?> = MutableStateFlow(null)
    var nextRuleAlbums: MutableStateFlow<RuleAlbums?> = MutableStateFlow(null)

    private var nextWallpaper: NextWallpaper? = null
    var nextWallpaperTime: Long = 0
    var nextRuleTime: Long = 0

    fun start() {
        Log.d(javaClass.simpleName, "start")
        val i = Intent(context, WorkpaperService::class.java)
        context.startService(i)
    }

    suspend fun stop() {
        Log.d(javaClass.simpleName, "stop")

        Log.d(javaClass.simpleName, runningPreferencesRepository.toString())

        currentRuleAlbums.value = null
        nextRuleAlbums.value = null

        cancelAllAlarm()

        val intent = Intent(context, WorkpaperService::class.java)
        context.stopService(intent)

        runningPreferencesRepository.apply {
            update(RunningPreferencesKeys.LAST_INDEX, -1)
            update(RunningPreferencesKeys.LAST_WALLPAPER, "")
        }
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
        return if (nextWallpaper == null) generateNextWallpaper() else nextWallpaper
    }

    fun setNextWallpaper(next: NextWallpaper) {
        nextWallpaper = next
    }

    suspend fun generateNextWallpaper(
        startIndex: Int? = null,
        isManual: Boolean = false,
        ruleId: Long? = null
    ): NextWallpaper? {
        val runningPreferences = runningPreferencesRepository.runningPreferencesFlow.first()

        Log.d(javaClass.simpleName, runningPreferences.toString())
        val index = startIndex ?: nextWallpaper?.index ?: -1
        val tmpRuleId = ruleId ?: this.currentRuleAlbums.value?.rule?.ruleId ?: -1

        if (tmpRuleId < 0) return null

        val ruleWithAlbums = ruleRepository.getRuleWithAlbums(tmpRuleId) ?: return null
        val albums = ruleWithAlbums.albums
        val wallpaperUris: MutableList<String> = mutableListOf()
        albums.forEach {
            wallpaperUris.addAll(it.wallpaperUris)
        }

        Log.d(javaClass.simpleName, albums.toString())

        if (wallpaperUris.isEmpty()) return null

        var nextIndex = nextIndex(wallpaperUris, index)
        if (ruleWithAlbums.rule.random && wallpaperUris.size > 1) {
            do {
                nextIndex = Random.nextInt(0, wallpaperUris.size - 1)
            } while (nextIndex == index)
        }

        return NextWallpaper(
            index = nextIndex,
            contentUri = wallpaperUris[nextIndex],
            isManual = isManual,
        )
    }

    private fun nextIndex(list: List<String>, currentIndex: Int): Int {
        return if (currentIndex + 1 >= list.size - 1) 0 else currentIndex + 1
    }
}