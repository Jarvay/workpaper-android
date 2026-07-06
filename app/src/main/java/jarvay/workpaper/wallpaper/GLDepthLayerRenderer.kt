package jarvay.workpaper.wallpaper

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.abs

class GLDepthLayerRenderer : GLSurfaceView.Renderer, GLWallpaperRenderer() {

    private val layerTextureHandles = IntArray(3)
    private val layerShiftFactors = floatArrayOf(0.15f, 0.55f, 1.0f)
    private var hasLayers = false

    @Volatile private var pendingBitmap: Bitmap? = null
    @Volatile private var pendingDepthMap: FloatArray? = null

    private var imgWidth = 0
    private var imgHeight = 0
    private var surfaceWidth = 0
    private var surfaceHeight = 0

    private var uvLeft = 0f
    private var uvRight = 1f
    private var uvTop = 0f
    private var uvBottom = 1f
    private var parallaxScale = 1.05f

    @Volatile private var tiltX = 0f
    @Volatile private var tiltY = 0f
    @Volatile private var sensitivity = 1.0f
    private var depthStrength = 0.5f

    private var wakeScale = 1.0f
    private var wakeScaleTarget = 1.0f
    private var wakeScaleVelocity = 0f
    private val wakeStiffness = 120f
    private val wakeDamping = 18f

    private lateinit var vertexBuffer: java.nio.FloatBuffer
    private var positionAttribLocation = -1
    private var texCoordAttribLocation = -1
    private var parallaxOffsetLocation = -1
    private var scaleUniformLocation = -1
    private var alphaLocation = -1

    private fun buildVertices() {
        val vertices = floatArrayOf(
            -1f, 1f, uvLeft, uvTop,
            -1f, -1f, uvLeft, uvBottom,
            1f, -1f, uvRight, uvBottom,
            1f, 1f, uvRight, uvTop
        )
        vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply { put(vertices); position(0) }
    }

    private fun recalcUV() {
        if (surfaceWidth == 0 || surfaceHeight == 0 || imgWidth == 0 || imgHeight == 0) {
            uvLeft = 0f; uvRight = 1f; uvTop = 0f; uvBottom = 1f
            return
        }
        val imgAspect = imgWidth.toFloat() / imgHeight.toFloat()
        val scrAspect = surfaceWidth.toFloat() / surfaceHeight.toFloat()
        if (imgAspect > scrAspect) {
            val visW = scrAspect / imgAspect
            uvLeft = (1f - visW) / 2f; uvRight = 1f - uvLeft
            uvTop = 0f; uvBottom = 1f
        } else {
            val visH = imgAspect / scrAspect
            uvTop = (1f - visH) / 2f; uvBottom = 1f - uvTop
            uvLeft = 0f; uvRight = 1f
        }
    }

    override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        initProgram()
        positionAttribLocation = GLES20.glGetAttribLocation(program, "vPosition")
        texCoordAttribLocation = GLES20.glGetAttribLocation(program, "vTexCoord")
        parallaxOffsetLocation = GLES20.glGetUniformLocation(program, "uParallaxOffset")
        scaleUniformLocation = GLES20.glGetUniformLocation(program, "uScale")
        alphaLocation = GLES20.glGetUniformLocation(program, "uAlpha")

        GLES20.glGenTextures(3, layerTextureHandles, 0)
        for (i in 0 until 3) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, layerTextureHandles[i])
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        }
        buildVertices()
        Log.d(TAG, "onSurfaceCreated")
    }

    override fun onSurfaceChanged(gl10: GL10?, w: Int, h: Int) {
        surfaceWidth = w; surfaceHeight = h
        GLES20.glViewport(0, 0, w, h)
        recalcUV(); buildVertices()
    }

    override fun onDrawFrame(p0: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        processPending()
        applyPendingState()
        updateWakeScale()

        if (!hasLayers) return

        GLES20.glUseProgram(program)

        val vb = vertexBuffer; vb.position(0)
        GLES20.glVertexAttribPointer(positionAttribLocation, 2, GLES20.GL_FLOAT, false, 16, vb)
        GLES20.glEnableVertexAttribArray(positionAttribLocation)
        vb.position(2)
        GLES20.glVertexAttribPointer(texCoordAttribLocation, 2, GLES20.GL_FLOAT, false, 16, vb)
        GLES20.glEnableVertexAttribArray(texCoordAttribLocation)

        val aspect = surfaceWidth.toFloat() / surfaceHeight.toFloat()
        val maxShift = 0.12f * sensitivity * depthStrength

        for (i in 0 until 3) {
            val shift = layerShiftFactors[i] * maxShift
            val offsetX = tiltX * shift
            val offsetY = -tiltY * shift * aspect

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, layerTextureHandles[i])
            GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "texture1"), 0)

            GLES20.glUniform2f(parallaxOffsetLocation, offsetX, offsetY)
            GLES20.glUniform1f(scaleUniformLocation, parallaxScale * wakeScale)

            if (i == 0) {
                GLES20.glDisable(GLES20.GL_BLEND)
            } else {
                GLES20.glEnable(GLES20.GL_BLEND)
                GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
            }
            GLES20.glUniform1f(alphaLocation, 1.0f)

            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4)
        }

        GLES20.glDisableVertexAttribArray(positionAttribLocation)
        GLES20.glDisableVertexAttribArray(texCoordAttribLocation)
    }

    private fun applyPendingState() {
        pendingParallaxEnabled?.let {
            parallaxScale = if (it) 1.05f else 1f
            pendingParallaxEnabled = null
            recalcUV(); buildVertices()
        }
        pendingDepthStrength?.let { depthStrength = it; pendingDepthStrength = null }
    }

    @Volatile private var pendingParallaxEnabled: Boolean? = null
    @Volatile private var pendingDepthStrength: Float? = null

    private fun processPending() {
        val bmp = pendingBitmap ?: return
        val dm = pendingDepthMap ?: return
        pendingBitmap = null; pendingDepthMap = null

        val maxDim = 1080
        var processingBmp = bmp
        if (bmp.width > maxDim) {
            val s = maxDim.toFloat() / bmp.width
            processingBmp = Bitmap.createScaledBitmap(bmp, maxDim, (bmp.height * s).toInt(), true)
        }
        imgWidth = processingBmp.width; imgHeight = processingBmp.height
        recalcUV(); buildVertices()

        val minD = dm.min(); val maxD = dm.max(); val range = maxD - minD
        if (range <= 0f || range.isNaN()) { uploadSimpleLayer(processingBmp); return }

        val depthW = kotlin.math.sqrt(dm.size.toDouble()).toInt().coerceAtLeast(1)
        val depthH = (dm.size / depthW).coerceAtLeast(1)

        val scaledDepth = FloatArray(imgWidth * imgHeight)
        for (y in 0 until imgHeight) {
            val sy = (y.toFloat() / imgHeight * depthH).toInt().coerceIn(0, depthH - 1)
            for (x in 0 until imgWidth) {
                val sx = (x.toFloat() / imgWidth * depthW).toInt().coerceIn(0, depthW - 1)
                scaledDepth[y * imgWidth + x] = (dm[sy * depthW + sx] - minD) / range
            }
        }

        val bmpPixels = IntArray(imgWidth * imgHeight)
        processingBmp.getPixels(bmpPixels, 0, imgWidth, 0, 0, imgWidth, imgHeight)

        val sortedD = scaledDepth.copyOf().also { it.sort() }
        val t1 = sortedD[(0.33f * sortedD.size).toInt().coerceIn(0, sortedD.size - 1)]
        val t2 = sortedD[(0.67f * sortedD.size).toInt().coerceIn(0, sortedD.size - 1)]

        val gradMap = computeDepthGradient(scaledDepth, imgWidth, imgHeight)

        for (layerIdx in 0 until 3) {
            val layerPixels = IntArray(imgWidth * imgHeight)

            for (p in 0 until imgWidth * imgHeight) {
                val d = scaledDepth[p]
                val edgeStrength = gradMap[p]
                val feather = 0.15f + edgeStrength * 0.1f

                val alpha = when (layerIdx) {
                    0 -> 1.0f
                    1 -> {
                        when {
                            d >= t2 -> 1.0f
                            d >= t2 - feather -> (d - (t2 - feather)) / feather
                            d <= t1 -> 1.0f
                            d <= t1 + feather -> 1f - (d - t1) / feather
                            else -> 0f
                        }
                    }
                    else -> {
                        when {
                            d <= t1 -> 1.0f
                            d <= t1 + feather -> 1f - (d - t1) / feather
                            else -> 0f
                        }
                    }
                }

                if (alpha > 0.02f) {
                    val src = bmpPixels[p]
                    val srcA = (src shr 24) and 0xFF
                    val srcR = (src shr 16) and 0xFF
                    val srcG = (src shr 8) and 0xFF
                    val srcB = src and 0xFF
                    val newA = (srcA * alpha).toInt().coerceIn(0, 255)
                    layerPixels[p] = (newA shl 24) or (srcR shl 16) or (srcG shl 8) or srcB
                } else {
                    layerPixels[p] = 0
                }
            }

            val config = if (layerIdx == 0) Bitmap.Config.RGB_565 else Bitmap.Config.ARGB_8888
            val layerBmp = Bitmap.createBitmap(imgWidth, imgHeight, config)
            layerBmp.setPixels(layerPixels, 0, imgWidth, 0, 0, imgWidth, imgHeight)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, layerTextureHandles[layerIdx])
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, layerBmp, 0)
            layerBmp.recycle()
        }

        if (processingBmp != bmp) processingBmp.recycle()

        hasLayers = true
        Log.d(TAG, "3 layers created, img=${imgWidth}x${imgHeight}")
    }

    private fun computeDepthGradient(depth: FloatArray, w: Int, h: Int): FloatArray {
        val grad = FloatArray(w * h)
        var maxGrad = 0f
        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                val gx = depth[y * w + x + 1] - depth[y * w + x - 1]
                val gy = depth[(y + 1) * w + x] - depth[(y - 1) * w + x]
                val g = kotlin.math.sqrt(gx * gx + gy * gy)
                grad[y * w + x] = g
                if (g > maxGrad) maxGrad = g
            }
        }
        if (maxGrad > 0f) {
            for (i in grad.indices) grad[i] /= maxGrad
        }
        return grad
    }

    private fun uploadSimpleLayer(bmp: Bitmap) {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, layerTextureHandles[0])
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0)
        val emptyBuf = ByteBuffer.allocate(4)
        for (i in 1 until 3) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, layerTextureHandles[i])
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, 1, 1, 0,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, emptyBuf)
            emptyBuf.position(0)
        }
        hasLayers = true
    }

    private fun updateWakeScale() {
        if (abs(wakeScale - wakeScaleTarget) < 0.001f && abs(wakeScaleVelocity) < 0.001f) {
            wakeScale = wakeScaleTarget; return
        }
        val dt = 1f / 60f
        wakeScaleVelocity += (wakeStiffness * (wakeScaleTarget - wakeScale) - wakeDamping * wakeScaleVelocity) * dt
        wakeScale = (wakeScale + wakeScaleVelocity * dt).coerceIn(0.9f, 1.15f)
    }

    fun triggerWakeAnimation() { wakeScale = 1.08f; wakeScaleVelocity = 0f; wakeScaleTarget = 1.0f }

    fun setDepthData(img: Bitmap, dm: FloatArray) {
        pendingBitmap?.recycle(); pendingBitmap = img; pendingDepthMap = dm
        Log.d(TAG, "setDepthData: ${img.width}x${img.height} depth=${dm.size}")
    }

    fun updateParallaxOffset(tx: Float, ty: Float, s: Float) { tiltX = tx; tiltY = ty; sensitivity = s }
    fun setParallaxEnabled(e: Boolean) { pendingParallaxEnabled = e }
    fun setDepthStrength(s: Float) { pendingDepthStrength = s }

    fun onDestroy() {
        if (hasLayers) { GLES20.glDeleteTextures(3, layerTextureHandles, 0); hasLayers = false }
        pendingBitmap?.recycle(); pendingBitmap = null; pendingDepthMap = null
    }

    override fun getFragmentShaderCode(): String =
        "precision mediump float;" +
        "uniform sampler2D texture1;" +
        "uniform float uAlpha;" +
        "varying vec2 outTexCoord;" +
        "void main() {" +
        "  vec4 color = texture2D(texture1, outTexCoord);" +
        "  gl_FragColor = color * uAlpha;" +
        "}"

    override fun getVertexShaderCode(): String =
        "attribute vec4 vPosition;" +
        "attribute vec2 vTexCoord;" +
        "varying vec2 outTexCoord;" +
        "uniform vec2 uParallaxOffset;" +
        "uniform float uScale;" +
        "void main() {" +
        "  outTexCoord = vTexCoord;" +
        "  gl_Position = vec4(vPosition.xy * uScale + uParallaxOffset, 0.0, 1.0);" +
        "}"

    companion object { private const val TAG = "GLDepthRenderer" }
}
