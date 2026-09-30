package jarvay.workpaper.data.webWallpaperApi

import com.blankj.utilcode.util.LogUtils
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import jarvay.workpaper.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

private data class PexelsResponse(
    @SerializedName("photos") val photos: List<PexelsPhoto>
)

private data class PexelsPhoto(
    @SerializedName("src") val src: PexelsSrc
)

private data class PexelsSrc(
    @SerializedName("large2x") val large2x: String,
    @SerializedName("original") val original: String,
    @SerializedName("portrait") val portrait: String
)

class Pexels : BaseWebWallpaperApi() {
    private suspend fun fallback(width: Int, height: Int): String {
        return UnsplashIt().getImageUrl(width, height)
    }

    override suspend fun getImageUrl(width: Int, height: Int): String =
        withContext(Dispatchers.IO) {
            val apiKey = BuildConfig.PEXELS_API_KEY
            if (apiKey.isEmpty()) {
                LogUtils.e("Pexels API key not configured, set PEXELS_API_KEY in gradle.properties")
                return@withContext fallback(width, height)
            }

            val randomPage = (1..MAX_PAGE).random()
            val query = KEYWORDS.random()
            val url = "https://api.pexels.com/v1/search" +
                    "?query=${java.net.URLEncoder.encode(query, "UTF-8")}" +
                    "&orientation=portrait" +
                    "&per_page=$PER_PAGE" +
                    "&page=$randomPage" +
                    "&size=large"

            val request = Request.Builder()
                .url(url)
                .header("Authorization", apiKey)
                .build()

            try {
                REQUEST_CLIENT.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        LogUtils.e("Pexels request failed: ${response.code} ${response.message}")
                        return@use fallback(width, height)
                    }

                    val body = response.body?.string() ?: return@use fallback(width, height)
                    val parsed = Gson().fromJson(body, PexelsResponse::class.java)
                    val photo = parsed.photos.randomOrNull() ?: return@use fallback(width, height)
                    LogUtils.d("Pexels image url: ${photo.src.large2x}")
                    photo.src.large2x
                }
            } catch (e: Exception) {
                LogUtils.e("Pexels error: $e")
                fallback(width, height)
            }
        }

    companion object {
        private const val PER_PAGE = 80
        private const val MAX_PAGE = 100
        private val KEYWORDS = listOf(
            "nature", "landscape", "mountain", "ocean", "forest",
            "city", "architecture", "sky", "sunset", "sunrise",
            "abstract", "minimal", "texture", "pattern", "space",
            "galaxy", "stars", "flowers", "animals", "travel"
        )
    }
}