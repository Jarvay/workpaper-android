package jarvay.workpaper.glance

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.BitmapImageProvider
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.text.Text
import jarvay.workpaper.R
import jarvay.workpaper.others.info
import jarvay.workpaper.receiver.ActionWidgetReceiver
import jarvay.workpaper.receiver.GenWallpaperReceiver
import jarvay.workpaper.receiver.WallpaperReceiver
import javax.inject.Inject

class ActionWidget @Inject constructor(
    private val receiver: ActionWidgetReceiver?
) : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                Content()
            }
        }
    }

    @SuppressLint("RestrictedApi")
    @Composable
    private fun Content() {
        val workpaper = receiver?.workpaper ?: return
        val context = LocalContext.current

        val size = LocalSize.current

        Log.d(javaClass.simpleName, size.toString())

        Log.d(javaClass.simpleName, workpaper.toString())

        Column(
            modifier = GlanceModifier.fillMaxSize()
                .cornerRadius(16.dp)
                .background(
                    Color(255, 255, 255, (255 * 0.25).toInt())
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val bitmap by workpaper.nextWallpaperBitmap.collectAsState()
            val settingWallpaper by workpaper.settingWallpaper.collectAsState(initial = false)

            Log.d(javaClass.simpleName, listOf("bitmap", bitmap?.info()).toString())
            if (bitmap == null) {
                Placeholder()
                return@Column
            }

            Log.d(javaClass.simpleName, bitmap!!.info())

            val clickHelper = ClickHelper(
                interval = 200,
                onDouble = {
                    Log.d(javaClass.simpleName, "onDouble")
                    if (settingWallpaper) return@ClickHelper
                    val i = Intent(context, WallpaperReceiver::class.java)
                    context.sendBroadcast(i)
                }
            ) {
                Log.d(javaClass.simpleName, "onSingle")
                val i = Intent(context, GenWallpaperReceiver::class.java)
                i.setAction(GenWallpaperReceiver.ACTION_NEXT_WALLPAPER)
                context.sendBroadcast(i)
            }

            Image(
                modifier = GlanceModifier.cornerRadius(16.dp)
                    .fillMaxSize()
                    .clickable {
                        clickHelper.click()
                    },
                provider = BitmapImageProvider(bitmap!!),
                contentScale = ContentScale.Fit,
                contentDescription = null
            )
        }
    }

    @Composable
    private fun Placeholder() {
        val context = LocalContext.current
        Text(text = context.getString(R.string.app_name))
    }
}