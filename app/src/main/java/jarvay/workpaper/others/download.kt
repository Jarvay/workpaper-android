package jarvay.workpaper.others

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
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.TimeUnit

private const val DOWNLOAD_TIMEOUT_SECONDS = 15L
private const val DOWNLOAD_RETRY_COUNT = 3
private const val DOWNLOAD_RETRY_DELAY_SECONDS = 5L

private val downloadClient = OkHttpClient.Builder()
    .callTimeout(DOWNLOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    .build()

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
            delay(DOWNLOAD_RETRY_DELAY_SECONDS * 1000)
        }
    }
    LogUtils.e("downloadImage failed after $DOWNLOAD_RETRY_COUNT attempts, url: $url")
    return null
}

private suspend fun downloadImageInternal(context: Context, url: String): Uri? =
    withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        downloadClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@use null

            val body = response.body ?: return@use null

            val fileExt = listOf(".jpg", ".jpeg", ".png", ".webp", ".gif").firstOrNull {
                url.substringBefore("?").endsWith(it, ignoreCase = true)
            } ?: ".jpg"
            val mimeType = when (fileExt) {
                ".jpg", ".jpeg" -> "image/jpeg"
                ".png" -> "image/png"
                ".webp" -> "image/webp"
                ".gif" -> "image/gif"
                else -> "image/jpeg"
            }
            val displayName = "${UUID.randomUUID()}$fileExt"
            val relativePath = Environment.DIRECTORY_DOWNLOADS + "/Workpaper/Wallpaper"
            val contentResolver = context.contentResolver

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val uri = contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues
                ) ?: return@use null

                contentResolver.openOutputStream(uri)?.use { output ->
                    body.byteStream().use { input ->
                        input.copyTo(output)
                    }
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
                    body.byteStream().use { input ->
                        input.copyTo(output)
                    }
                }
                LogUtils.d("downloadImage file: $file")
                Uri.fromFile(file)
            }
        }
    }