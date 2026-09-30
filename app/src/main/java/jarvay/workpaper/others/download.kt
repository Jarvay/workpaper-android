package jarvay.workpaper.others

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.blankj.utilcode.util.LogUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds

private const val DOWNLOAD_TIMEOUT_SECONDS = 15L
private const val DOWNLOAD_RETRY_COUNT = 3
private const val DOWNLOAD_RETRY_DELAY_SECONDS = 5L
private const val HTTP_CACHE_SIZE_BYTES = 50L * 1024 * 1024

private val downloadClient = OkHttpClient.Builder()
    .callTimeout(DOWNLOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    .build()

private var cachedDownloadClient: OkHttpClient? = null

private fun getCachedDownloadClient(context: Context): OkHttpClient {
    return cachedDownloadClient ?: run {
        val cacheDir = File(context.cacheDir, "wallpaper_http_cache")
        val client = OkHttpClient.Builder()
            .cache(Cache(cacheDir, HTTP_CACHE_SIZE_BYTES))
            .callTimeout(DOWNLOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
        cachedDownloadClient = client
        client
    }
}

private val WALLPAPER_RELATIVE_PATH =
    Environment.DIRECTORY_DOWNLOADS + "/Workpaper/Wallpaper"

private val IMAGE_EXTENSIONS = listOf(".jpg", ".jpeg", ".png", ".webp", ".gif")

private fun md5Hex(data: ByteArray): String {
    val digest = MessageDigest.getInstance("MD5")
    val bytes = digest.digest(data)
    return bytes.joinToString("") { "%02x".format(it) }
}

private fun urlHash(url: String): String = md5Hex(url.toByteArray())

private fun getUrlImageExt(url: String): String? {
    val path = url.substringBefore("?")
    return IMAGE_EXTENSIONS.firstOrNull { path.endsWith(it, ignoreCase = true) }
}

private fun findExistingUri(context: Context, displayName: String): Uri? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val contentResolver = context.contentResolver
        val projection = arrayOf(MediaStore.MediaColumns._ID)
        val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ? " +
                "AND ${MediaStore.MediaColumns.RELATIVE_PATH} = ? " +
                "AND ${MediaStore.MediaColumns.IS_PENDING} = 0"
        val selectionArgs = arrayOf(displayName, "$WALLPAPER_RELATIVE_PATH/")
        contentResolver.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id)
            } else null
        }
    } else {
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "Workpaper/Wallpaper"
        )
        val file = File(dir, displayName)
        if (file.exists()) Uri.fromFile(file) else null
    }
}

suspend fun downloadImage(context: Context, url: String): Uri? {
    repeat(DOWNLOAD_RETRY_COUNT) { attempt ->
        try {
            val uri = downloadImageInternal(context, url)
            if (uri != null) return uri
            LogUtils.w("downloadImage attempt ${attempt + 1} returned null, will retry")
        } catch (e: Exception) {
            LogUtils.e("downloadImage attempt ${attempt + 1} error: $e")
        }
        if (attempt < DOWNLOAD_RETRY_COUNT - 1) {
            delay((DOWNLOAD_RETRY_DELAY_SECONDS * 1000).milliseconds)
        }
    }
    LogUtils.e("downloadImage failed after $DOWNLOAD_RETRY_COUNT attempts, url: $url")
    return null
}

private suspend fun downloadImageInternal(context: Context, url: String): Uri? =
    withContext(Dispatchers.IO) {
        val urlExt = getUrlImageExt(url)
        val fileExt = urlExt ?: ".jpg"
        val isContentAddressed = urlExt != null

        if (isContentAddressed) {
            val displayName = "${urlHash(url)}$fileExt"
            findExistingUri(context, displayName)?.let {
                LogUtils.d("downloadImage url cache hit: $it")
                return@withContext it
            }
        }

        val client = if (isContentAddressed) downloadClient else getCachedDownloadClient(context)
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@use null

            val body = response.body ?: return@use null
            val bodyBytes = body.bytes()
            if (bodyBytes.isEmpty()) return@use null

            val displayName = if (isContentAddressed) {
                "${urlHash(url)}$fileExt"
            } else {
                "${md5Hex(bodyBytes)}$fileExt"
            }

            findExistingUri(context, displayName)?.let {
                LogUtils.d("downloadImage content cache hit: $it")
                return@use it
            }

            val mimeType = when (fileExt) {
                ".jpg", ".jpeg" -> "image/jpeg"
                ".png" -> "image/png"
                ".webp" -> "image/webp"
                ".gif" -> "image/gif"
                else -> "image/jpeg"
            }
            val contentResolver = context.contentResolver

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, WALLPAPER_RELATIVE_PATH)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val uri = contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues
                ) ?: return@use null

                contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(bodyBytes)
                } ?: return@use null

                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                contentResolver.update(uri, contentValues, null, null)

                LogUtils.d("downloadImage uri: $uri")
                uri
            } else {
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "Workpaper/Wallpaper"
                ).apply { mkdirs() }
                val file = File(dir, displayName)
                FileOutputStream(file).use { output ->
                    output.write(bodyBytes)
                }
                LogUtils.d("downloadImage file: $file")
                Uri.fromFile(file)
            }
        }
    }