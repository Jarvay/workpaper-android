package jarvay.workpaper

import android.app.ActivityManager
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import jarvay.workpaper.compose.WorkpaperApp
import jarvay.workpaper.data.preferences.RunningPreferencesRepository
import jarvay.workpaper.others.getUriByDownloadId
import jarvay.workpaper.others.installApk
import jarvay.workpaper.ui.theme.WorkpaperTheme
import jarvay.workpaper.viewModel.MainActivityViewModel
import javax.inject.Inject
import javax.inject.Singleton

@AndroidEntryPoint
@Singleton
class MainActivity : ComponentActivity() {
    private val viewModel: MainActivityViewModel by viewModels()

    @Inject
    lateinit var runningPreferencesRepository: RunningPreferencesRepository

    @Inject
    lateinit var workpaper: Workpaper

    private var apkDownloadId: Long? = null

    private val downloadStartReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (context == null || intent == null) return
            val id = intent.getLongExtra(APK_DOWNLOAD_ID_KEY, -1)
            if (id > -1) {
                apkDownloadId = id
            }
        }
    }

    private val downloadCompleteReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null || context == null) return
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (id == apkDownloadId) {
                val uri = getUriByDownloadId(this@MainActivity, id)
                installApk(uri = uri, context = this@MainActivity)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        registerDownloadStartListener()
        registerDownloadCompleteListener()

        enableEdgeToEdge()

        viewModel.settings.observe(this) {
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            activityManager.let { manager ->
                manager.appTasks.forEach { task ->
                    task?.setExcludeFromRecents(it.hideInRecentTask)
                }
            }
        }

        viewModel.settings.observe(this) {
            setContent {
                WorkpaperTheme(
                    dynamicColor = it.enableDynamicColor
                ) {
                    WorkpaperApp(mainActivityViewModel = viewModel)
                }
            }
        }
    }

    private fun registerDownloadStartListener() {
        val intentFilter = IntentFilter(ACTION_APK_DOWNLOAD_ID)

        ContextCompat.registerReceiver(
            this,
            downloadStartReceiver,
            intentFilter,
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    private fun registerDownloadCompleteListener() {
        val intentFilter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)

        ContextCompat.registerReceiver(
            this,
            downloadCompleteReceiver,
            intentFilter,
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(downloadStartReceiver)
        unregisterReceiver(downloadCompleteReceiver)
    }

    companion object {
        const val ACTION_APK_DOWNLOAD_ID = "actionApkDownloadId"
        const val APK_DOWNLOAD_ID_KEY = "apkDownloadId"
    }
}