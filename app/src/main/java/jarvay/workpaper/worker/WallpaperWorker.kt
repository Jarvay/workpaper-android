package jarvay.workpaper.worker

import android.app.WallpaperManager
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import android.util.Log
import androidx.core.net.toUri
import androidx.hilt.work.HiltWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import jarvay.workpaper.data.preferences.RunningPreferences
import jarvay.workpaper.data.preferences.RunningPreferencesKeys
import jarvay.workpaper.data.preferences.RunningPreferencesRepository
import jarvay.workpaper.data.preferences.SettingsPreferencesRepository
import jarvay.workpaper.data.rule.RuleDao
import jarvay.workpaper.data.rule.RuleRepository
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.random.Random

@HiltWorker
class WallpaperWorker @AssistedInject constructor(
    @Assisted val appContext: Context,
    @Assisted params: WorkerParameters,
    private val ruleDao: RuleDao,
    private val ruleRepository: RuleRepository,
    private val runningPreferencesRepository: RunningPreferencesRepository,
    private val settingPreferencesRepository: SettingsPreferencesRepository
) : Worker(appContext, params) {

    private fun nextIndex(list: List<String>, currentIndex: Int): Int {
        return if (currentIndex + 1 >= list.size) 0 else currentIndex + 1
    }

    private fun setWallpaper(wallpaper: String) {
        val settings = runBlocking {
            settingPreferencesRepository.settingsPreferencesFlow.first()
        }

        Log.d(javaClass.simpleName, settings.toString())

        val wallpaperUri = wallpaper.toUri()
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val source =
                    ImageDecoder.createSource(applicationContext.contentResolver, wallpaperUri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.isMutableRequired = true
                }
            } catch (e: Exception) {
                applicationContext.contentResolver.openInputStream(wallpaperUri)
                    ?.use { inputStream ->
                        val options = BitmapFactory.Options().apply {
                            inMutable = true
                        }
                        BitmapFactory.decodeStream(inputStream, null, options)
                    }
            }
        } else {
            applicationContext.contentResolver.openInputStream(wallpaperUri)
                ?.use { inputStream ->
                    val options = BitmapFactory.Options().apply {
                        inMutable = true
                    }
                    BitmapFactory.decodeStream(inputStream, null, options)
                }
        }
        val wallpaperManager = WallpaperManager.getInstance(applicationContext)
        if (!settings.alsoSetLockWallpaper) {
            Log.d(javaClass.simpleName, "set system wallpaper only")
            wallpaperManager.setBitmap(bitmap, null, false, WallpaperManager.FLAG_SYSTEM)
        } else {
            Log.d(javaClass.simpleName, "set both wallpaper")
            wallpaperManager.setBitmap(bitmap)
        }

    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun doWork(): Result {
        Log.d(javaClass.simpleName, "start")
        val runningPreferences: RunningPreferences = runBlocking {
            runningPreferencesRepository.runningPreferencesFlow.first()
        }

        Log.d(javaClass.simpleName, runningPreferences.toString())
        val index = runningPreferences.lastIndex ?: 0
        val ruleId = runningPreferences.currentRuleId ?: -1
        val lastWallpaper = runningPreferences.lastWallpaper

        if (ruleId < 0) return Result.failure()

        val ruleWithAlbums = ruleRepository.getRuleWithAlbums(ruleId)

        ruleWithAlbums?.let {
            val albums = ruleWithAlbums.albums
            val wallpaperUris: MutableList<String> = mutableListOf()
            albums.forEach {
                wallpaperUris.addAll(it.wallpaperUris)
            }

            Log.d(javaClass.simpleName, albums.toString())

            if (wallpaperUris.isEmpty()) return Result.failure()

            var nextIndex = nextIndex(wallpaperUris, index)
            if (ruleWithAlbums.rule.random) {
                do {
                    nextIndex = Random.nextInt(0, wallpaperUris.size - 1)
                } while (nextIndex == index)
            }

            val wallpaper = wallpaperUris[nextIndex]
            if (lastWallpaper != wallpaper) {
                setWallpaper(wallpaper = wallpaper)
            }

            runningPreferencesRepository.apply {
                GlobalScope.launch {
                    update(RunningPreferencesKeys.LAST_INDEX, nextIndex)
                    update(RunningPreferencesKeys.LAST_WALLPAPER, wallpaper)
                }
            }
        }

        return Result.success()
    }
}