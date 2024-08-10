package jarvay.workpaper

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import jarvay.workpaper.compose.WorkpaperApp
import jarvay.workpaper.receiver.RuleReceiver
import jarvay.workpaper.ui.theme.WorkpaperTheme
import jarvay.workpaper.viewModel.MainActivityViewModel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainActivityViewModel by viewModels()

    @Inject
    lateinit var workpaper: Workpaper

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

        val running = viewModel.runningPreferences.value?.running ?: false
        val currentRuleId = viewModel.runningPreferences.value?.currentRuleId ?: -1;
        if (running && !workpaper.isAlarmExist(AlarmType.REPEAT) && currentRuleId > 0) {
            val intent = Intent(this, RuleReceiver::class.java)
            intent.putExtra(RuleReceiver.RULE_ID_KEY, currentRuleId)
        }

        enableEdgeToEdge()
        setContent {
            WorkpaperTheme {
                WorkpaperApp()
            }
        }
    }
}