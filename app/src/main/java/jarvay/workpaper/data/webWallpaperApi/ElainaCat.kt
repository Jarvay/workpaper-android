package jarvay.workpaper.data.webWallpaperApi

class ElainaCat : BaseWebWallpaperApi() {
    override suspend fun getImageUrl(width: Int, height: Int): String {
        return "https://api.elaina.cat/random/mobile/"
    }
}