package jarvay.workpaper

import android.app.Application
import android.content.Intent
import android.content.IntentFilter
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.blankj.utilcode.util.LogUtils
import dagger.hilt.android.HiltAndroidApp
import jarvay.workpaper.data.preferences.RunningPreferencesRepository
import jarvay.workpaper.data.preferences.SettingsPreferencesRepository
import jarvay.workpaper.receiver.UnlockReceiver
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject


@HiltAndroidApp
class MainApplication : Application(), Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var runningPreferencesRepository: RunningPreferencesRepository

    @Inject
    lateinit var settingsPreferencesRepository: SettingsPreferencesRepository

    @Inject
    lateinit var workpaper: Workpaper

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) android.util.Log.DEBUG else android.util.Log.ERROR)
            .build()

    private fun initLogUtils() {
        LogUtils.getConfig().apply {
            saveDays = 7
            setConsoleSwitch(true)
            isLog2FileSwitch = runBlocking {
                settingsPreferencesRepository.settingsPreferencesFlow.first().enableLog
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        MainScope().launch {
            settingsPreferencesRepository.settingsPreferencesFlow.collect {
                LogUtils.getConfig().apply {
                    isLog2FileSwitch = it.enableLog
                }
            }
        }

        initLogUtils()

        val unlockReceiver = UnlockReceiver()
        val unlockFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(unlockReceiver, unlockFilter)

        MainScope().launch {
            val runningPreferences = runningPreferencesRepository.runningPreferencesFlow.first()
            val running = runningPreferences.running
            if (running) {
                workpaper.start()
            }
        }
    }
}
