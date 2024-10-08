package jarvay.workpaper.receiver

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import dagger.hilt.android.AndroidEntryPoint
import jarvay.workpaper.glance.ActionWidget

@AndroidEntryPoint
class ActionWidgetReceiver : GlanceAppWidgetReceiver() {
//    @Inject
//    lateinit var workpaper: Workpaper

    override val glanceAppWidget: GlanceAppWidget
        get() = ActionWidget()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        Log.d(javaClass.simpleName, "onReceive")
    }
}