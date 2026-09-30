package jarvay.workpaper.data.webWallpaperApi

import okhttp3.OkHttpClient

abstract class BaseWebWallpaperApi {
    abstract suspend fun getImageUrl(width: Int, height: Int): String

    companion object {
        @JvmStatic
        protected val REQUEST_CLIENT = OkHttpClient.Builder().build()
    }
}