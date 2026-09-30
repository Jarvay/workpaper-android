package jarvay.workpaper.data.webWallpaperApi

class UnsplashIt : BaseWebWallpaperApi() {
    override suspend fun getImageUrl(width: Int, height: Int): String {
        return "https://unsplash.it/$width/$height?random"
    }
}