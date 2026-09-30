package jarvay.workpaper.data.webWallpaperApi

class Bing : BaseWebWallpaperApi() {
    override suspend fun getImageUrl(width: Int, height: Int): String {
        return "https://bz.w3h5.com/img/m"
    }
}