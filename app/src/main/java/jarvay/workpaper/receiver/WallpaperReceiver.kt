package jarvay.workpaper.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import jarvay.workpaper.worker.WallpaperWorker

class WallpaperReceiver : BroadcastReceiver() {
    override fun onReceive(p0: Context?, p1: Intent?) {
        Log.d(javaClass.simpleName, p1.toString())

        p0?.let {
            val wallpaperWorkRequest: WorkRequest = OneTimeWorkRequestBuilder<WallpaperWorker>()
                .build()
            WorkManager.getInstance(p0).enqueue(wallpaperWorkRequest)
        }
    }

    companion object {
        const val ACTION_UPDATE_WALLPAPER = "actionUpdateWallpaper"
    }
}