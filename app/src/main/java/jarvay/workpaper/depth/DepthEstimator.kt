package jarvay.workpaper.depth

import android.content.Context
import android.graphics.Bitmap

interface DepthEstimator {
    fun initialize()
    fun estimateDepth(bitmap: Bitmap): FloatArray
    fun isInitialized(): Boolean
    fun close()

    companion object {
        const val MODEL_MIDAS = "midas"
        const val MODEL_DAV2 = "dav2"

        fun create(context: Context, modelType: String = MODEL_DAV2): DepthEstimator {
            return when (modelType) {
                MODEL_DAV2 -> DAV2DepthEstimator(context)
                else -> MiDaSDepthEstimator(context)
            }
        }
    }
}
