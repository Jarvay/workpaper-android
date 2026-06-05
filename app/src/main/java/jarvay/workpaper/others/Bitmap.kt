package jarvay.workpaper.others

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PixelFormat
import android.media.ImageReader
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import androidx.annotation.IntRange
import androidx.annotation.RequiresApi
import com.blankj.utilcode.util.LogUtils
import jarvay.workpaper.JNIWrapper
import android.hardware.HardwareBuffer
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.RenderNode
import android.graphics.HardwareRenderer
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

    return Bitmap.createBitmap(this, dx, dy, targetWidth, targetHeight)
}

fun Bitmap.info(): String {
    return "width: $width, height: $height"
}

fun coverBitmapFromContentUri(contentUri: Uri, context: Context): Bitmap? {
    val retriever = MediaMetadataRetriever()
    retriever.setDataSource(context, contentUri)
    return retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
}

fun bitmapFromContentUri(
    contentUri: Uri,
    context: Context,
    options: BitmapFactory.Options = BitmapFactory.Options()
): Bitmap? {
    fun fromStream(): Bitmap? {
        return try {
            context.contentResolver.openInputStream(contentUri)
                ?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream, null, options.apply {
                        inMutable = true
                    })
                }
        } catch (e: Exception) {
            LogUtils.w("bitmapFromContentUri", "Load bitmap failed", e.toString())
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
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        try {
            blurHardwareBuffer(radius)
        } catch (e: Exception) {
            LogUtils.w("Bitmap.blur", "HardwareBuffer blur failed, falling back to JNI", e.toString())
            JNIWrapper.blur(this, radius)
        }
    } else {
        JNIWrapper.blur(this, radius)
    }
}

@RequiresApi(Build.VERSION_CODES.S)
private fun Bitmap.blurHardwareBuffer(radius: Int): Bitmap {
    val imageReader = ImageReader.newInstance(
        width, height,
        PixelFormat.RGBA_8888, 2,
        HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE or HardwareBuffer.USAGE_GPU_COLOR_OUTPUT
    )

    val renderNode = RenderNode("BlurEffect")
    val hardwareRenderer = HardwareRenderer()

    hardwareRenderer.setSurface(imageReader.surface)
    hardwareRenderer.setContentRoot(renderNode)
    renderNode.setPosition(0, 0, imageReader.width, imageReader.height)

    val blurRenderEffect = RenderEffect.createBlurEffect(
        radius.toFloat(), radius.toFloat(),
        Shader.TileMode.MIRROR
    )
    renderNode.setRenderEffect(blurRenderEffect)

    val renderCanvas = renderNode.beginRecording()
    renderCanvas.drawBitmap(this, 0f, 0f, null)
    renderNode.endRecording()

    hardwareRenderer.createRenderRequest()
        .setWaitForPresent(true)
        .syncAndDraw()

    val image = imageReader.acquireNextImage() ?: throw RuntimeException("Blur failed: ImageReader.acquireNextImage() returned null")
    val hardwareBuffer = image.hardwareBuffer ?: throw RuntimeException("Blur failed: Image.hardwareBuffer is null")

    try {
        val hardwareBitmap = Bitmap.wrapHardwareBuffer(hardwareBuffer, null)
            ?: throw RuntimeException("Blur failed: Bitmap.wrapHardwareBuffer() returned null")
        return hardwareBitmap.copy(Bitmap.Config.ARGB_8888, true)
    } finally {
        image.close()
        imageReader.close()
    }
}

fun Bitmap.noise(@IntRange(1, 100) percent: Int): Bitmap {
    return JNIWrapper.noise(this, percent)
}

fun Bitmap.effect(
    brightness: Int,
    contrast: Int,
    saturation: Int,
): Bitmap {
    val b = (brightness - 50) * 50 / 150
    val c = (contrast - 50) * 50 / 150
    val s = (saturation - 50) * 50 / 150

    if (b == 0 && c == 0 && s == 0) {
        return this
    }

    return JNIWrapper.effect(this, b, c, s)
}

fun Bitmap.setAlpha(alpha: Int): Bitmap {
    val bm = Bitmap.createBitmap(width, height, config)
    val canvas = Canvas(bm)
    val paint = Paint()
    paint.alpha = alpha
    canvas.drawBitmap(this, 0f, 0f, paint)
    return bm
}