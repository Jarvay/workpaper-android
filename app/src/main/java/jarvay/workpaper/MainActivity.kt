package jarvay.workpaper

import android.app.ActivityManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import jarvay.workpaper.compose.WorkpaperApp
import jarvay.workpaper.ui.theme.WorkpaperTheme
import jarvay.workpaper.viewModel.MainActivityViewModel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainActivityViewModel by viewModels()

    @Inject
    lateinit var workpaper: Workpaper

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("viewModel", viewModel.toString())

        val settings = viewModel.settings
        settings.observe(this) {
            Log.d("settings update", it.toString())

            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            activityManager.let { manager ->
                manager.appTasks.forEach { task ->
                    task?.setExcludeFromRecents(it.hideInRecentTask)
                }
            }
        }

        viewModel.runningPreferences.observe(this) {
            if (it.running) {
                workpaper.stop(
                    GlobalScope,
                    runningPreferencesRepository = viewModel.runningPreferencesRepository
                )
                workpaper.start(GlobalScope)
            }
        }

        enableEdgeToEdge()
        setContent {
            WorkpaperTheme {
                WorkpaperApp()
            }
        }
    }
}