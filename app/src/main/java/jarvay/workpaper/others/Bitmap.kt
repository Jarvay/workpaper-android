package jarvay.workpaper.others

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.IntRange
import com.google.android.renderscript.Toolkit
import com.google.gson.Gson
import java.util.Random
import kotlin.math.max
import kotlin.math.min


fun Bitmap.scaleFixedRatio(targetWidth: Int, targetHeight: Int, useMin: Boolean = true): Bitmap {
    val scaleWidth = (targetWidth.toFloat()) / width
    val scaleHeight = (targetHeight.toFloat()) / height

    val scaleRatio = if (useMin) {
        min(scaleHeight.toDouble(), scaleWidth.toDouble()).toFloat()
    } else {
        max(scaleHeight.toDouble(), scaleWidth.toDouble()).toFloat()
    }
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

fun Bitmap.blur(@IntRange(1, 25) radius: Int): Bitmap {
    return Toolkit.blur(this, radius)
}

fun Bitmap.noise(@IntRange(1, 100) percent: Int): Bitmap {
    val pixels = IntArray(width * height)

    getPixels(pixels, 0, width, 0, 0, width, height)

    val random = Random()

    var index: Int

    var rgb: Int
    var randColor: Int

    for (y in 0 until height) {
        for (x in 0 until width) {
            if (random.nextInt(101) > percent / 2) {
                continue
            }
            index = y * width + x
            rgb = random.nextInt(100)
            randColor = Color.rgb(rgb, rgb, rgb)
            pixels[index] = pixels[index] or randColor
        }
    }
    val bmOut = Bitmap.createBitmap(width, height, config)
    bmOut.setPixels(pixels, 0, width, 0, 0, width, height)
    return bmOut
}

fun Bitmap.effect(
    brightness: Int,
    contrast: Int,
    saturation: Int,
): Bitmap {
    val bitmap = Bitmap.createBitmap(this, 0, 0, width, height)

    val colorMatrix = ColorMatrix()

    val lum = (brightness - 50) * 2 * 0.3f * 255 * 0.01f
    val brightnessArray = floatArrayOf(
        1f, 0f, 0f, 0f, lum,
        0f, 1f, 0f, 0f, lum,
        0f, 0f, 1f, 0f, lum,
        0f, 0f, 0f, 1f, 0f
    )
    colorMatrix.set(brightnessArray)

    val scale = (contrast - 50 + 100) / 100f
    val offset = 0.5f * (1 - scale) + 0.5f
    val contrastArray =
        floatArrayOf(
            scale, 0f, 0f, 0f, offset,
            0f, scale, 0f, 0f, offset,
            0f, 0f, scale, 0f, offset,
            0f, 0f, 0f, 1f, 0f
        )
    colorMatrix.postConcat(ColorMatrix(contrastArray))
    val saturationMatrix = ColorMatrix()
    saturationMatrix.setSaturation(((saturation - 50) / 50) * 0.3f + 1)
    colorMatrix.postConcat(saturationMatrix)

    val colorFilter = ColorMatrixColorFilter(colorMatrix)

    val paint = Paint().apply {
        this.colorFilter = colorFilter
    }

    val canvas = Canvas(bitmap)
    canvas.drawBitmap(this, 0f, 0f, paint)

    return bitmap
}