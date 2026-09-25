package jarvay.workpaper.data.webWallpaperApi

enum class WebWallpaperApi(
    val apiName: String
) {
    BING(apiName = "Bing"),
    PIC_SUM(apiName = "PicSum"),
    PEXELS(apiName = "Pexels"),
    ELAINA_CAT(apiName = "ElainaCat(anime)"),
    SUI_YUE_XIAO_ZHU(apiName = "岁月小筑"),
    UNSPLASH_IT(apiName = "Unsplash It"),
    ;

    fun getApi(): BaseWebWallpaperApi {
        return when (this) {
            BING -> Bing()
            PIC_SUM -> PicSum()
            PEXELS -> Pexels()
            ELAINA_CAT -> ElainaCat()
            SUI_YUE_XIAO_ZHU -> SuiYueXiaoZhu()
            UNSPLASH_IT -> UnsplashIt()
        }
    }
}