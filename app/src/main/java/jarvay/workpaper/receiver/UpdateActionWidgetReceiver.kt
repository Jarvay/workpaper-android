package jarvay.workpaper.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.AndroidEntryPoint
import jarvay.workpaper.glance.ActionWidget
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class UpdateActionWidgetReceiver : BroadcastReceiver() {
    @Inject
    lateinit var actionWidget: ActionWidget

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        MainScope().launch {
            actionWidget.updateAll(context)
        }
    }
}