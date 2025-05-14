package jarvay.workpaper.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.blankj.utilcode.util.LogUtils
import dagger.hilt.android.AndroidEntryPoint
import jarvay.workpaper.Workpaper
import jarvay.workpaper.others.LOG_TAG
import jarvay.workpaper.worker.WallpaperWorker
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class WallpaperReceiver : BroadcastReceiver() {
    @Inject
    lateinit var workpaper: Workpaper

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        MainScope().launch {
            val isChangeByManual = intent.getBooleanExtra(FLAG_CHANGE_BY_MANUAL, false)
            if (isChangeByManual) {
                val next = workpaper.getNextWallpaper()
                if (next == null) {
                    LogUtils.w(LOG_TAG, "Next wallpaper is null")
                    return@launch
                }
                workpaper.setNextWallpaper(next.copy(isManual = true))
            }

            val workManager = WorkManager.getInstance(context)
            if (workpaper.lastWallpaperWorkerId != null) {
                val lastWorkInfo = runBlocking {
                    workManager.getWorkInfoByIdFlow(workpaper.lastWallpaperWorkerId!!).first()
                }
                if (!lastWorkInfo.state.isFinished) {
                    LogUtils.i(LOG_TAG, "Last work not finished, skip")
                    return@launch
                }
            }

            val wallpaperWorkRequest: WorkRequest = OneTimeWorkRequestBuilder<WallpaperWorker>()
                .build()
            workpaper.lastWallpaperWorkerId = wallpaperWorkRequest.id
            WorkManager.getInstance(context).enqueue(wallpaperWorkRequest)
            LogUtils.i(LOG_TAG, "WallpaperWork enqueue")
        }
    }

    companion object {
        const val FLAG_CHANGE_BY_MANUAL = "flagChangeByManual"
    }
}