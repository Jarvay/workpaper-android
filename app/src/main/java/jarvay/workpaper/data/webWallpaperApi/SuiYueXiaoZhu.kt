package jarvay.workpaper.data.webWallpaperApi

class SuiYueXiaoZhu : BaseWebWallpaperApi() {
    override suspend fun getImageUrl(width: Int, height: Int): String {
        return "https://img.xjh.me/random_img.php?type=bg&ctype=nature&return=302&device=mobile"
    }
}