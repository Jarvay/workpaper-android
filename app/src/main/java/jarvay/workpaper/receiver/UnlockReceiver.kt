package jarvay.workpaper.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.blankj.utilcode.util.LogUtils
import dagger.hilt.android.AndroidEntryPoint
import jarvay.workpaper.Workpaper
import jarvay.workpaper.data.preferences.RunningPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class UnlockReceiver : BroadcastReceiver() {
    @Inject
    lateinit var runningPreferencesRepository: RunningPreferencesRepository

    @Inject
    lateinit var workpaper: Workpaper

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val action = intent.action
        if (action != Intent.ACTION_USER_PRESENT) return

        MainScope().launch(Dispatchers.Default) {
            if (!workpaper.isRunning()) return@launch

            val ruleAlbums = workpaper.currentRuleWithRelation.first()

            if (ruleAlbums === null) {
                LogUtils.i(javaClass.simpleName, "Current rule is null, skip")
                return@launch
            }

            if (ruleAlbums.rule.changeWhileUnlock) {
                val i = Intent(context, WallpaperReceiver::class.java)
                context.sendBroadcast(i)
            }
        }
    }
}