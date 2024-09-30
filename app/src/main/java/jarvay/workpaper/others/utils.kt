package jarvay.workpaper.others

import android.app.DownloadManager
import android.app.DownloadManager.Request
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import jarvay.workpaper.R
import jarvay.workpaper.data.preferences.RunningPreferences
import jarvay.workpaper.data.preferences.RunningPreferencesKeys
import jarvay.workpaper.data.rule.Rule
import jarvay.workpaper.data.rule.RuleAlbums
import jarvay.workpaper.data.rule.RuleAlbumsToSort
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
    ruleWithAlbums: List<RuleAlbums>,
    finder: (List<RuleAlbumsToSort>) -> RuleAlbumsToSort?,
): RuleAlbumsToSort? {
    val rules = ArrayList<RuleAlbumsToSort>()
    ruleWithAlbums.forEach {
        it.rule.days.forEach { day ->
            val minute = day * 24 * 60 + it.rule.startHour * 60 + it.rule.startMinute
            val millis = minute * 60 * 1000
            rules.add(
                RuleAlbumsToSort(
                    ruleAlbums = it.copy(),
                    sortValue = millis.toLong(),
                    day = day
                )
            )
        }
    }

    val list = rules.apply {
        Log.d("rules", rules.toString())
    }
    return finder(list)
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

fun prevRule(list: List<RuleAlbums>): RuleAlbumsToSort? {
    return findRule(ruleWithAlbums = list, finder = { l ->
        val sortedList = l.sortedByDescending { it.sortValue }
        val result = sortedList.find {
            it.sortValue <= currentMillis()
        }
        result ?: if (sortedList.isNotEmpty()) sortedList.first() else null
    })
}

fun nextRule(list: List<RuleAlbums>): RuleAlbumsToSort? {
    val currentMillis = currentMillis()
    return findRule(ruleWithAlbums = list, finder = { l ->
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

fun getSize(context: Context, fileStrUri: String): Size? {
    try {
        context.contentResolver.openInputStream(fileStrUri.toUri()).use { inputStream ->
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeStream(inputStream, null, options)
            return Size(options.outWidth, options.outHeight)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}

fun getScreenSize(context: Context): Size {
    val dm = context.resources.displayMetrics
    return Size(dm.widthPixels, dm.heightPixels)
}

fun getWallpaperSize(context: Context): Size {
    val screenSize = getScreenSize(context)
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

        RunningPreferences(
            lastIndex = lastIndex,
            lastWallpaper = lastWallpaper,
            running = running,
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

    Log.d("request", request.toString())

    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    val id = downloadManager.enqueue(request)
    Log.d("download id", id.toString())
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
            Log.e("install apk failed", e.toString())
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