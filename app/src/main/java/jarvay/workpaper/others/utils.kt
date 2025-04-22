package jarvay.workpaper.others

import android.app.DownloadManager
import android.app.DownloadManager.Request
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.media.AudioManager
import android.net.Uri
import android.os.Environment
import android.util.Size
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.documentfile.provider.DocumentFile
import com.blankj.utilcode.util.LogUtils
import jarvay.workpaper.R
import jarvay.workpaper.data.preferences.RunningPreferences
import jarvay.workpaper.data.preferences.RunningPreferencesKeys
import jarvay.workpaper.data.rule.Rule
import jarvay.workpaper.data.rule.RuleWithRelation
import jarvay.workpaper.data.rule.RuleWithRelationToSort
import jarvay.workpaper.data.wallpaper.WallpaperType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar


fun formatTime(hour: Int, minute: Int): String {
    var hourString = hour.toString()
    hourString = if (hourString.length == 1) "0${hourString}" else hourString
    var minuteString = minute.toString()
    minuteString = if (minuteString.length == 1) "0${minuteString}" else minuteString
    return "${hourString}:${minuteString}"
}

fun getCalendarWithRule(rule: Rule, dayOfWeek: Int): Calendar {
    val calendar = Calendar.getInstance()
    return calendar.apply {
        set(Calendar.DAY_OF_WEEK, dayOfWeek)
        set(Calendar.HOUR_OF_DAY, rule.startHour)
        set(Calendar.MINUTE, rule.startMinute)
        set(Calendar.SECOND, 0)
    }
}

fun findRule(
    ruleWithRelationList: List<RuleWithRelation>,
    finder: (List<RuleWithRelationToSort>) -> RuleWithRelationToSort?,
): RuleWithRelationToSort? {
    val rules = ArrayList<RuleWithRelationToSort>()
    ruleWithRelationList.forEach {
        it.rule.days.forEach { day ->
            val minute = day * 24 * 60 + it.rule.startHour * 60 + it.rule.startMinute
            val millis = minute * 60 * 1000
            rules.add(
                RuleWithRelationToSort(
                    ruleWithRelation = it.copy(),
                    sortValue = millis.toLong(),
                    day = day
                )
            )
        }
    }

    return finder(rules)
}

fun currentMillis(): Long {
    val calendar = Calendar.getInstance()
    val minute =
        calendar.get(Calendar.DAY_OF_WEEK) * 24 * 60 + calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(
            Calendar.MINUTE
        )
    val millis =
        minute * 60 * 1000 + calendar.get(Calendar.SECOND) * 1000 + calendar.get(Calendar.MILLISECOND)
    return millis.toLong()
}

fun prevRule(list: List<RuleWithRelation>): RuleWithRelationToSort? {
    return findRule(ruleWithRelationList = list, finder = { l ->
        val sortedList = l.sortedByDescending { it.sortValue }
        val result = sortedList.find {
            it.sortValue <= currentMillis()
        }
        result ?: if (sortedList.isNotEmpty()) sortedList.first() else null
    })
}

fun nextRule(list: List<RuleWithRelation>): RuleWithRelationToSort? {
    val currentMillis = currentMillis()
    return findRule(ruleWithRelationList = list, finder = { l ->
        val sortedList = l.sortedBy { it.sortValue }
        val result = sortedList.find {
            it.sortValue >= currentMillis
        }
        result ?: if (sortedList.isNotEmpty()) sortedList.first() else null
    })
}

fun showToast(context: Context, text: String) {
    Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
}

fun showToast(context: Context, @StringRes strId: Int) {
    showToast(context, context.getString(strId))
}

fun getScreenSize(): Size {
    val dm = Resources.getSystem().displayMetrics
    return Size(dm.widthPixels, dm.heightPixels)
}

fun getWallpaperSize(): Size {
    val screenSize = getScreenSize()
    val ratio = screenSize.width.toFloat() / screenSize.height.toFloat()
    return if (screenSize.width > MAX_WALLPAPER_WIDTH) {
        Size(MAX_WALLPAPER_WIDTH, (MAX_WALLPAPER_WIDTH / ratio).toInt())
    } else if (screenSize.height > MAX_WALLPAPER_HEIGHT) {
        Size((MAX_WALLPAPER_HEIGHT * ratio).toInt(), MAX_WALLPAPER_HEIGHT)
    } else {
        screenSize
    }
}

fun runningPreferencesFlow(dataStore: DataStore<Preferences>): Flow<RunningPreferences> {
    return dataStore.data.map { preferences ->
        val lastIndex = preferences[RunningPreferencesKeys.LAST_INDEX] ?: -1
        val lastWallpaper = preferences[RunningPreferencesKeys.LAST_WALLPAPER] ?: ""
        val running = preferences[RunningPreferencesKeys.RUNNING] ?: false
        val currentVideoContentUri =
            preferences[RunningPreferencesKeys.CURRENT_VIDEO_CONTENT_URI] ?: ""

        RunningPreferences(
            lastIndex = lastIndex,
            lastWallpaper = lastWallpaper,
            running = running,
            currentVideoContentUri = currentVideoContentUri
        )
    }
}

fun download(url: String, context: Context): Long {
    val request = Request(Uri.parse(url)).apply {
        setNotificationVisibility(Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        setTitle(context.getString(R.string.download))
        setAllowedOverRoaming(false)
        setMimeType("application/vnd.android.package-archive")

        setDestinationInExternalPublicDir(
            Environment.DIRECTORY_DOWNLOADS,
            "workpaper.apk"
        )
    }

    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    val id = downloadManager.enqueue(request)
    return id
}

fun getUriByDownloadId(context: Context, id: Long): Uri? {
    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    return downloadManager.getUriForDownloadedFile(id)
}

fun installApk(uri: Uri?, context: Context) {
    uri.let {
        val intent = Intent()

        intent.action = Intent.ACTION_VIEW
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.setDataAndType(uri, "application/vnd.android.package-archive")

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            LogUtils.e("install apk failed", e.toString())
        }
    }
}

fun isToday(calendar: Calendar): Boolean {
    val now = Calendar.getInstance()
    return ((calendar[Calendar.YEAR] == now[Calendar.YEAR])
            && (calendar[Calendar.MONTH] == now[Calendar.MONTH])
            && (calendar[Calendar.DAY_OF_MONTH] == now[Calendar.DAY_OF_MONTH]))
}

fun Context.audioManager(): AudioManager {
    return getSystemService(Context.AUDIO_SERVICE) as AudioManager
}

fun wallpaperType(type: String): WallpaperType {
    return if (type.startsWith("video")) {
        WallpaperType.VIDEO
    } else {
        WallpaperType.IMAGE
    }
}

fun wechatComponentName(): ComponentName {
    return ComponentName("com.tencent.mm", "com.tencent.mm.ui.LauncherUI")
}

fun wechatIntent(toScan: Boolean = false): Intent {
    return Intent().apply {
        component = wechatComponentName()
        if (toScan) {
            putExtra("LauncherUI.Shortcut.LaunchType", "launch_type_scan_qrcode")
        }
        action = "android.intent.action.VIEW"
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
    }
}

fun Context.getOneWallpaperInDir(uri: Uri): DocumentFile? {
    val documentFile = DocumentFile.fromTreeUri(this, uri) ?: return null
    for (item in documentFile.listFiles()) {
        if (item.isFile) {
            val mimeType = item.type ?: continue
            val supported = SUPPORTED_WALLPAPER_TYPES_PREFIX.any {
                mimeType.startsWith(it)
            }
            if (!supported) continue

            return item
        }
    }

    return null
}