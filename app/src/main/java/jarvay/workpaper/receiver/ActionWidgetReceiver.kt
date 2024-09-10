package jarvay.workpaper.receiver

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import dagger.hilt.android.AndroidEntryPoint
import jarvay.workpaper.Workpaper
import jarvay.workpaper.glance.ActionWidget
import javax.inject.Inject

@AndroidEntryPoint
class ActionWidgetReceiver : GlanceAppWidgetReceiver() {
    @Inject
    lateinit var actionWidget: ActionWidget

    override val glanceAppWidget: GlanceAppWidget
        get() = actionWidget
}