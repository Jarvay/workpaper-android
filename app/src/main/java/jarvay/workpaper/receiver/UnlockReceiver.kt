package jarvay.workpaper.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import jarvay.workpaper.Workpaper
import jarvay.workpaper.data.preferences.RunningPreferencesRepository
import jarvay.workpaper.data.rule.RuleRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class UnlockReceiver : BroadcastReceiver() {
    @Inject
    lateinit var ruleRepository: RuleRepository

    @Inject
    lateinit var runningPreferencesRepository: RunningPreferencesRepository

    @Inject
    lateinit var workpaper: Workpaper

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(
            javaClass.simpleName,
            listOf("onReceive", intent.toString(), intent?.action).joinToString(", ")
        )

        if (context == null || intent == null) return

        val action = intent.action
        if (action != Intent.ACTION_USER_PRESENT) return

        if (!workpaper.isRunning(runningPreferencesRepository.runningPreferencesFlow)) return

        val currentRuleId = runBlocking {
            runningPreferencesRepository.runningPreferencesFlow.first().currentRuleId
        }
        Log.d("currentRuleId", currentRuleId.toString())
        val ruleAlbums = ruleRepository.getRuleWithAlbums(ruleId = currentRuleId) ?: return

        if (ruleAlbums.rule.changeWhileUnlock) {
            val i = Intent(context, WallpaperReceiver::class.java)
            context.sendBroadcast(i)
        }
    }
}