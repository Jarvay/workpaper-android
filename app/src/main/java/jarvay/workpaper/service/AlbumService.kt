package jarvay.workpaper.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import dagger.hilt.android.AndroidEntryPoint
import jarvay.workpaper.data.album.AlbumDao
import jarvay.workpaper.others.SharePreferenceKey
import jarvay.workpaper.others.defaultSharedPreferences
import javax.inject.Inject

@AndroidEntryPoint
class AlbumService @Inject constructor() : Service() {
    @Inject
    lateinit var albumDao: AlbumDao

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val albumId = intent?.getLongExtra(ALBUM_ID_KEY, 0)
        if (albumId != null) {
            albumDao.findById(albumId) ?: return super.onStartCommand(intent, flags, startId)
            val sp = defaultSharedPreferences(this)
            sp.edit().putLong(SharePreferenceKey.CURRENT_ALBUM_ID_KEY, albumId)
                .putInt(SharePreferenceKey.LAST_INDEX_KEY, 0).apply()
        }

        return super.onStartCommand(intent, flags, startId)
    }

    companion object {
        const val ALBUM_ID_KEY = "id"
    }
}