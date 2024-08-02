package jarvay.workpaper.worker

import android.app.Service.MODE_PRIVATE
import android.app.WallpaperManager
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import androidx.core.net.toUri
import androidx.hilt.work.HiltWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import jarvay.workpaper.data.album.AlbumDao
import jarvay.workpaper.others.SharePreferenceKey
import jarvay.workpaper.others.defaultSharedPreferences

@HiltWorker
class WallpaperWorker @AssistedInject constructor(
    @Assisted val appContext: Context,
    @Assisted params: WorkerParameters,
    private val albumDao: AlbumDao
) : Worker(appContext, params) {

    private fun nextIndex(list: List<String>, currentIndex: Int): Int {
        return if (currentIndex + 1 >= list.size) 0 else currentIndex + 1
    }

    override fun doWork(): Result {
        val sp = defaultSharedPreferences(appContext)
        val index = sp.getInt(SharePreferenceKey.LAST_INDEX_KEY, 0)
        val albumId = sp.getLong(SharePreferenceKey.CURRENT_ALBUM_ID_KEY, -1)

        if (albumId < 0) return Result.failure()

        val album = albumDao.findById(albumId)

        album?.let {
            val nextIndex = nextIndex(album.wallpaperUris, index)
            val wallpaper = album.wallpaperUris[nextIndex]
            val lastWallpaper = sp.getString(SharePreferenceKey.LAST_WALLPAPER, null)
            if (lastWallpaper.equals(wallpaper)) {
                return@let
            }

            val wallpaperUri = wallpaper.toUri()
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                try {
                    val source =
                        ImageDecoder.createSource(applicationContext.contentResolver, wallpaperUri)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                    }
                } catch (e: Exception) {
                    applicationContext.contentResolver.openInputStream(wallpaperUri)
                        ?.use { inputStream ->
                            val options = BitmapFactory.Options().apply {
                                inMutable = true
                            }
                            BitmapFactory.decodeStream(inputStream, null, options)
                        }
                }
            } else {
                applicationContext.contentResolver.openInputStream(wallpaperUri)
                    ?.use { inputStream ->
                        val options = BitmapFactory.Options().apply {
                            inMutable = true
                        }
                        BitmapFactory.decodeStream(inputStream, null, options)
                    }
            }
            val wallpaperManager = WallpaperManager.getInstance(applicationContext)
            wallpaperManager.setBitmap(bitmap)

            sp.edit().apply {
                putInt(SharePreferenceKey.LAST_INDEX_KEY, nextIndex)
                putString(SharePreferenceKey.LAST_WALLPAPER, wallpaper)
            }.apply()
        }

        return Result.success()
    }
}