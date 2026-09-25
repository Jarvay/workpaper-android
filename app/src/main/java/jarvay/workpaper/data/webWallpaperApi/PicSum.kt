package jarvay.workpaper.data.webWallpaperApi

class PicSum : BaseWebWallpaperApi() {
    override suspend fun getImageUrl(width: Int, height: Int): String {
        return "https://picsum.photos/$width/$height"
    }
}