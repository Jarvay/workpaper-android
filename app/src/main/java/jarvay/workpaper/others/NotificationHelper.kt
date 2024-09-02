package jarvay.workpaper.others

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Paint
import android.os.Build
import android.util.Size
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import jarvay.workpaper.R
import jarvay.workpaper.Workpaper
import jarvay.workpaper.receiver.GenWallpaperReceiver
import jarvay.workpaper.receiver.WallpaperReceiver
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

enum class NotificationActionType(val value: Int) {
    CHANGE_NEXT(1),
    CHANGE_TO_NEXT_NOW(1)
}

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext val context: Context
) {
    @Inject
    lateinit var workpaper: Workpaper

    fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.app_name),
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun notificationBuilder(ongoing: Boolean): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.app_name))
            .setOngoing(ongoing)
    }

    @SuppressLint("SimpleDateFormat")
    fun titleAndText(): Pair<String, String> {
        fun format(calendar: Calendar): String {
            val format =
                if (isToday(calendar)) SimpleDateFormat("HH:mm") else SimpleDateFormat("E HH:mm")

            return format.format(Date(calendar.timeInMillis))
        }

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = workpaper.nextWallpaperTime

        val nextRuleCalendar = Calendar.getInstance()
        nextRuleCalendar.timeInMillis = workpaper.nextRuleTime

        val text = context.getString(
            R.string.notify_next_rule_at_about,
            format(nextRuleCalendar)
        )

        var title: String

        if (workpaper.currentRuleAlbums.value?.rule?.changeByTiming == true) {
            title = context.getString(
                R.string.notify_will_change_at_about,
                format(calendar)
            )

            if (nextRuleCalendar.before(calendar)) {
                title = context.getString(
                    R.string.notify_will_change_at_about,
                    format(nextRuleCalendar)
                )
            }
        } else {
            title = context.getString(R.string.notify_next_wallpaper)
        }

        return Pair(title, text)
    }

    fun createPictures(contentUri: String): Pair<Bitmap, Bitmap>? {
        var bitmap = bitmapFromContentUri(contentUri = contentUri.toUri(), context) ?: return null

        val backgroundHeight = 288
        val backgroundSize = Size(backgroundHeight * 2, backgroundHeight)
        val background = Bitmap.createBitmap(
            backgroundSize.width, backgroundSize.height, Bitmap.Config.ARGB_8888
        ).copy(Bitmap.Config.ARGB_8888, true)
        val canvas = android.graphics.Canvas(background)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val maxWidth = 2048
        val cropWidth = if (bitmap.width > maxWidth) maxWidth else bitmap.width
        val cropSize = Size(
            cropWidth, cropWidth / 2
        )
        val backgroundBitmap = bitmap.centerCrop(cropSize.width, cropSize.height)
            .copy(Bitmap.Config.ARGB_8888, true)
            .scaleFixedRatio(backgroundSize.width, backgroundSize.height)
            .copy(Bitmap.Config.ARGB_8888, true)


        canvas.drawBitmap(backgroundBitmap, 0F, 0F, paint)
        canvas.drawARGB((255 * 0.4).toInt(), 0, 0, 0)

        bitmap = bitmap.scaleFixedRatio(backgroundSize.height, backgroundSize.height)
            .copy(Bitmap.Config.ARGB_8888, true)
        canvas.drawBitmap(
            bitmap, (backgroundSize.width / 2 - bitmap.width / 2).toFloat(), 0F, paint
        )

        return Pair(background, bitmap)
    }

    companion object {
        const val CHANNEL_ID = "workpaper_channel"

        val NOTIFICATION_ID =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            else 2
    }
}

fun NotificationCompat.Builder.setBigPicture(
    background: Bitmap,
    bitmap: Bitmap,
    context: Context,
    showChangeNow: Boolean = true
): NotificationCompat.Builder {
    val changeNextPendingIntent = PendingIntent.getBroadcast(
        context,
        NotificationActionType.CHANGE_NEXT.value,
        Intent(context, GenWallpaperReceiver::class.java).apply {
            action = GenWallpaperReceiver.ACTION_NEXT_WALLPAPER
        },
        PendingIntent.FLAG_IMMUTABLE
    )

    val wallpaperReceiverIntent = Intent(context, WallpaperReceiver::class.java)
    wallpaperReceiverIntent.putExtra(WallpaperReceiver.FLAG_CHANGE_BY_MANUAL, true)
    val changeNowPendingIntent = PendingIntent.getBroadcast(
        context,
        NotificationActionType.CHANGE_TO_NEXT_NOW.value,
        wallpaperReceiverIntent,
        PendingIntent.FLAG_MUTABLE
    )

    return setStyle(
        NotificationCompat.BigPictureStyle().bigPicture(background)
            .bigLargeIcon(null as Bitmap?)
    ).setLargeIcon(bitmap)
        .addAction(
            0,
            context.getString(R.string.notify_action_change_next),
            changeNextPendingIntent
        ).apply {
            if (showChangeNow) {
                addAction(
                    0,
                    context.getString(R.string.notify_action_change_to_next_now),
                    changeNowPendingIntent
                )
            }
        }
}