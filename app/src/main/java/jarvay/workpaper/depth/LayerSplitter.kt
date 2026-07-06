package jarvay.workpaper.depth

import android.graphics.Bitmap
import android.util.Log

class LayerSplitter {

    data class DepthLayer(
        val bitmap: Bitmap,
        val depthFactor: Float
    )

    fun split(
        original: Bitmap,
        depthMap: FloatArray,
        numLayers: Int = 3
    ): List<DepthLayer> {
        val width = original.width
        val height = original.height
        val pixelCount = width * height

        val depthWidth = kotlin.math.sqrt(depthMap.size.toDouble()).toInt()
        val depthHeight = depthMap.size / depthWidth
        val scaledDepth = scaleDepthMap(depthMap, depthWidth, depthHeight, width, height)

        val minDepth = scaledDepth.min()
        val maxDepth = scaledDepth.max()
        val range = maxDepth - minDepth

        Log.d(TAG, "split: image=" + width + "x" + height + " depthMap=" + depthWidth + "x" + depthHeight +
            " min=" + "%.4f".format(minDepth) + " max=" + "%.4f".format(maxDepth) + " range=" + "%.4f".format(range))

        if (range <= 0f) {
            Log.w(TAG, "Depth range is zero, returning single layer")
            return listOf(DepthLayer(original.copy(original.config, true), 0.5f))
        }

        val originalPixels = IntArray(pixelCount)
        original.getPixels(originalPixels, 0, width, 0, 0, width, height)

        val layers = mutableListOf<DepthLayer>()

        for (layerIndex in 0 until numLayers) {
            val layerBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val layerPixelsFinal = IntArray(pixelCount)

            val lo = layerIndex.toFloat() / numLayers
            val hi = (layerIndex + 1).toFloat() / numLayers

            var visibleCount = 0

            for (i in 0 until pixelCount) {
                val normalizedDepth = (scaledDepth[i] - minDepth) / range
                val depth = normalizedDepth
                val rgb = originalPixels[i]
                if (depth >= lo && depth < hi) {
                    layerPixelsFinal[i] = rgb or (0xFF shl 24)
                    visibleCount++
                } else if (layerIndex == numLayers - 1 && depth >= lo) {
                    layerPixelsFinal[i] = rgb or (0xFF shl 24)
                    visibleCount++
                } else {
                    layerPixelsFinal[i] = 0
                }
            }

            layerBitmap.setPixels(layerPixelsFinal, 0, width, 0, 0, width, height)
            val depthFactor = (layerIndex + 0.5f) / numLayers
            layers.add(DepthLayer(layerBitmap, depthFactor))

            Log.d(TAG, "Layer " + layerIndex + ": depthRange=[" + "%.2f".format(lo) + "," + "%.2f".format(hi) +
                "] depthFactor=" + "%.3f".format(depthFactor) + " visible=" + visibleCount + "/" + pixelCount)
        }

        return layers
    }

    private fun scaleDepthMap(
        depthMap: FloatArray,
        srcWidth: Int,
        srcHeight: Int,
        dstWidth: Int,
        dstHeight: Int
    ): FloatArray {
        val result = FloatArray(dstWidth * dstHeight)
        for (y in 0 until dstHeight) {
            for (x in 0 until dstWidth) {
                val srcX = (x.toFloat() / dstWidth * srcWidth).toInt().coerceIn(0, srcWidth - 1)
                val srcY = (y.toFloat() / dstHeight * srcHeight).toInt().coerceIn(0, srcHeight - 1)
                result[y * dstWidth + x] = depthMap[srcY * srcWidth + srcX]
            }
        }
        return result
    }

    companion object {
        private const val TAG = "LayerSplitter"
    }
}
