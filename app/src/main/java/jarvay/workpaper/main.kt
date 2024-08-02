package jarvay.workpaper

import android.content.Context
import android.content.Intent
import jarvay.workpaper.service.ScheduleService

fun start(context: Context) {
    val i = Intent(context, ScheduleService::class.java)
    context.startService(i)
}