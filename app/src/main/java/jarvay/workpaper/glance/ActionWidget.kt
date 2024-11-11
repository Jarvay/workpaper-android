package jarvay.workpaper.glance

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.glance.BitmapImageProvider
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.LocalContext
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
import dagger.hilt.EntryPoints
import jarvay.workpaper.EntryPoint
import jarvay.workpaper.R
import jarvay.workpaper.Workpaper
import jarvay.workpaper.data.wallpaper.WallpaperType
import jarvay.workpaper.others.bitmapFromContentUri
import jarvay.workpaper.others.coverBitmapFromContentUri
import jarvay.workpaper.others.scaleFixedRatio
import jarvay.workpaper.receiver.GenWallpaperReceiver
import jarvay.workpaper.receiver.WallpaperReceiver
import javax.inject.Inject

class ActionWidget @Inject constructor() : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val workpaper =
            EntryPoints.get(context.applicationContext, EntryPoint::class.java).workpaper()

        provideContent {
            GlanceTheme {
                Content(workpaper)
            }
        }
    }

    @SuppressLint("RestrictedApi")
    @Composable
    private fun Content(workpaper: Workpaper) {
        val context = LocalContext.current

        Column(
            modifier = GlanceModifier.fillMaxSize()
                .background(
                    Color(255, 255, 255, (255 * 0.25).toInt())
                ).cornerRadius(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val nextWallpaper by workpaper.nextWallpaper.collectAsState()
            if (nextWallpaper == null) {
                Placeholder()
                return@Column
            }

            val contentUri = nextWallpaper!!.wallpaper.contentUri
            val type = nextWallpaper!!.wallpaper.type
            grantUri(context, contentUri)
            var bitmap = when (type) {
                WallpaperType.IMAGE -> bitmapFromContentUri(contentUri.toUri(), context)
                WallpaperType.VIDEO -> coverBitmapFromContentUri(contentUri.toUri(), context)
            }

            if (bitmap == null) {
                Placeholder()
                return@Column
            }

            bitmap = bitmap.scaleFixedRatio(256, 256, false)

            releaseUriPermission(context, contentUri)

            val clickHelper = ClickHelper(
                interval = 250,
                onDouble = {
                    val i = Intent(context, WallpaperReceiver::class.java)
                    context.sendBroadcast(i)
                }
            ) {
                val i = Intent(context, GenWallpaperReceiver::class.java)
                i.setAction(GenWallpaperReceiver.ACTION_NEXT_WALLPAPER)
                context.sendBroadcast(i)
            }

            Image(
                modifier = GlanceModifier.fillMaxSize()
                    .clickable {
                        clickHelper.click()
                    },
                provider = BitmapImageProvider(bitmap),
                contentScale = ContentScale.Crop,
                contentDescription = null
            )
        }
    }

    @Composable
    private fun Placeholder() {
        val context = LocalContext.current
        Text(text = context.getString(R.string.app_name))
    }

    private fun releaseUriPermission(context: Context, uri: String) {
        val launcherName = launcherName(context) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.revokeUriPermission(
                launcherName,
                uri.toUri(),
                FLAG_GRANT_READ_URI_PERMISSION or FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
    }

    private fun launcherName(context: Context): String? {
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }
        val resolveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.resolveActivity(
                intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()),
            )
        } else {
            context.packageManager.resolveActivity(
                intent,
                PackageManager.MATCH_DEFAULT_ONLY,
            )
        }
        return resolveInfo?.activityInfo?.packageName
    }

    private fun grantUri(context: Context, uri: String) {
        val launcherName = launcherName(context)
        if (launcherName != null) {
            context.grantUriPermission(
                launcherName,
                uri.toUri(),
                PERMISSION_FLAG,
            )
        }
    }

    companion object {
        private const val PERMISSION_FLAG =
            FLAG_GRANT_READ_URI_PERMISSION or FLAG_GRANT_PERSISTABLE_URI_PERMISSION
    }
}