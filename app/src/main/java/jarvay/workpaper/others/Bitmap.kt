package jarvay.workpaper.others

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.util.Log
import kotlin.math.min

fun Bitmap.scaleFixedRatio(targetWidth: Int, targetHeight: Int): Bitmap {
    val scaleWidth = (targetWidth.toFloat()) / width
    val scaleHeight = (targetHeight.toFloat()) / height

    val scaleRatio = min(scaleHeight.toDouble(), scaleWidth.toDouble()).toFloat()
    val matrix = Matrix()
    matrix.postScale(scaleRatio, scaleRatio)
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

fun Bitmap.centerCrop(targetWidth: Int, targetHeight: Int): Bitmap {
    val srcRate = width.toFloat() / height.toFloat()
    val desRate: Float = targetWidth.toFloat() / targetHeight.toFloat()
    var dx = 0
    var dy = 0
    if (srcRate == desRate) {
        return this
    } else if (srcRate > desRate) {
        dx = (width - targetWidth) / 2
    } else {
        dy = (height - targetHeight) / 2
    }

    val desBitmap = Bitmap.createBitmap(this, dx, dy, targetWidth, targetHeight)
    return desBitmap
}

fun Bitmap.info(): String {
    return "width: $width, height: $height"
}

fun bitmapFromContentUri(contentUri: Uri, context: Context): Bitmap? {
    fun fromStream(): Bitmap? {
        return try {
            context.contentResolver.openInputStream(contentUri)
                ?.use { inputStream ->
                    val options = BitmapFactory.Options().apply {
                        inMutable = true
                    }
                    BitmapFactory.decodeStream(inputStream, null, options)
                }
        } catch (e: Exception) {
            Log.w("bitmapFromContentUri", e.toString())
            e.printStackTrace()
            null
        }
    }

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        try {
            val source =
                ImageDecoder.createSource(context.contentResolver, contentUri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.isMutableRequired = true
            }
        } catch (e: Exception) {
            fromStream()
        }
    } else {
        fromStream()
    }
}