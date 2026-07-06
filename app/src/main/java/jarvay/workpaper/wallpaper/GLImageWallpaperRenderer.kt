package jarvay.workpaper.wallpaper

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import jarvay.workpaper.others.PARALLAX_NO_SCALE
import jarvay.workpaper.others.PARALLAX_QUAD_SCALE
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.abs

class GLImageWallpaperRenderer : GLSurfaceView.Renderer, GLWallpaperRenderer() {
    private val textureHandles = IntArray(2)
    private var currentTextureIndex = 0
    private var transitionAlpha = 0f
    private var shouldSwapAfterTransition = false

    private val pendingBitmaps = arrayOfNulls<Bitmap?>(2)
    private val textureUploaded = booleanArrayOf(false, false)

    private var screenWidth = 0
    private var screenHeight = 0
    private var bitmapWidth = 0
    private var bitmapHeight = 0

    private var parallaxOffsetX = 0f
    private var parallaxOffsetY = 0f
    private var parallaxScale = 1.0f

    private var uvLeft = 0f
    private var uvRight = 1f
    private var uvTop = 0f
    private var uvBottom = 1f

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

    private fun buildVertices() {
        // 顶点(x,y) + UV(u,v). 屏幕顶部(y=1) -> UV.v = 0 (bitmap顶部), 屏幕底部 -> UV.v = 1
        val vertices = floatArrayOf(
            -1f,  1f, uvLeft,  uvTop,      // 左上
            -1f, -1f, uvLeft,  uvBottom,   // 左下
             1f, -1f, uvRight, uvBottom,   // 右下
             1f,  1f, uvRight, uvTop,      // 右上
        )
        vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply { put(vertices); position(0) }
    }

    private fun recalcUV() {
        if (screenWidth == 0 || screenHeight == 0 || bitmapWidth == 0 || bitmapHeight == 0) {
            uvLeft = 0f; uvRight = 1f; uvTop = 0f; uvBottom = 1f
            return
        }
        val imgAspect = bitmapWidth.toFloat() / bitmapHeight.toFloat()
        val scrAspect = screenWidth.toFloat() / screenHeight.toFloat()
        val zoom = parallaxScale

        if (imgAspect > scrAspect) {
            // 图片比屏幕更宽 -> 水平裁剪 (取中间部分)
            val visW = scrAspect / imgAspect / zoom
            uvLeft = (1f - visW) / 2f
            uvRight = 1f - uvLeft
            uvTop = 0f
            uvBottom = 1f
        } else {
            // 图片比屏幕更高 -> 垂直裁剪
            val visH = imgAspect / scrAspect / zoom
            uvTop = (1f - visH) / 2f
            uvBottom = 1f - uvTop
            uvLeft = 0f
            uvRight = 1f
        }
    }

    override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        initProgram()
        parallaxOffsetLocation = GLES20.glGetUniformLocation(program, "uParallaxOffset")
        positionAttribLocation = GLES20.glGetAttribLocation(program, "vPosition")
        texCoordAttribLocation = GLES20.glGetAttribLocation(program, "vTexCoord")
        scaleUniformLocation = GLES20.glGetUniformLocation(program, "uScale")
        GLES20.glGenTextures(2, textureHandles, 0)
        for (i in 0 until 2) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandles[i])
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        }
        buildVertices()
    }

    override fun onSurfaceChanged(gl10: GL10?, width: Int, height: Int) {
        screenWidth = width
        screenHeight = height
        GLES20.glViewport(0, 0, width, height)
        recalcUV()
        buildVertices()
    }

    override fun onDrawFrame(p0: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        for (i in 0 until 2) {
            if (pendingBitmaps[i] != null && !textureUploaded[i]) {
                uploadTexture(i, pendingBitmaps[i]!!)
                pendingBitmaps[i] = null
                textureUploaded[i] = true
                recalcUV()
                buildVertices()
            }
        }

        updateWakeScale()
        GLES20.glUseProgram(program)

        val vb = vertexBuffer
        vb.position(0)
        GLES20.glVertexAttribPointer(positionAttribLocation, 2, GLES20.GL_FLOAT, false, 16, vb)
        GLES20.glEnableVertexAttribArray(positionAttribLocation)
        vb.position(2)
        GLES20.glVertexAttribPointer(texCoordAttribLocation, 2, GLES20.GL_FLOAT, false, 16, vb)
        GLES20.glEnableVertexAttribArray(texCoordAttribLocation)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandles[currentTextureIndex])
        GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "texture1"), 0)
        if (transitionAlpha > 0f) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandles[(currentTextureIndex + 1) % 2])
            GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "texture2"), 1)
        }
        GLES20.glUniform1f(GLES20.glGetUniformLocation(program, "alpha"), transitionAlpha)

        GLES20.glUniform2f(parallaxOffsetLocation, parallaxOffsetX, parallaxOffsetY)
        GLES20.glUniform1f(scaleUniformLocation, wakeScale)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4)

        GLES20.glDisableVertexAttribArray(positionAttribLocation)
        GLES20.glDisableVertexAttribArray(texCoordAttribLocation)

        if (transitionAlpha >= 1f && shouldSwapAfterTransition) {
            swapTextures()
            transitionAlpha = 0f
            shouldSwapAfterTransition = false
        }
    }

    private fun updateWakeScale() {
        if (abs(wakeScale - wakeScaleTarget) < 0.001f && abs(wakeScaleVelocity) < 0.001f) {
            wakeScale = wakeScaleTarget
            return
        }
        val dt = 1f / 60f
        val springForce = wakeStiffness * (wakeScaleTarget - wakeScale)
        val dampForce = wakeDamping * wakeScaleVelocity
        wakeScaleVelocity += (springForce - dampForce) * dt
        wakeScale = (wakeScale + wakeScaleVelocity * dt).coerceIn(0.9f, 1.15f)
    }

    fun triggerWakeAnimation() {
        wakeScale = 1.08f
        wakeScaleVelocity = 0f
        wakeScaleTarget = 1.0f
    }

    fun setBitmap(bitmap: Bitmap, isNext: Boolean = false, parallax: Boolean = false) {
        val textureIndex = if (isNext) (currentTextureIndex + 1) % 2 else currentTextureIndex
        pendingBitmaps[textureIndex]?.recycle()
        pendingBitmaps[textureIndex] = bitmap
        textureUploaded[textureIndex] = false

        bitmapWidth = bitmap.width
        bitmapHeight = bitmap.height

        parallaxScale = if (parallax) PARALLAX_QUAD_SCALE else PARALLAX_NO_SCALE
        recalcUV()
    }

    fun setTextureRatio(widthRatio: Float) {}

    fun updateOffset(offset: Float) {}

    fun updateParallaxOffset(tiltX: Float, tiltY: Float, sensitivity: Float) {
        val maxShift = 0.08f * sensitivity
        parallaxOffsetX = tiltX * maxShift
        parallaxOffsetY = -tiltY * maxShift
    }

    fun setParallaxEnabled(enabled: Boolean) {
        parallaxScale = if (enabled) PARALLAX_QUAD_SCALE else PARALLAX_NO_SCALE
        recalcUV()
        buildVertices()
    }

    private fun uploadTexture(index: Int, bitmap: Bitmap) {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandles[index])
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
    }

    fun startTransition() {
        transitionAlpha = 0f
        shouldSwapAfterTransition = true
    }

    fun updateTransitionAlpha(alpha: Float) {
        transitionAlpha = alpha.coerceIn(0f, 1f)
    }

    fun onDestroy() {
        for (i in 0 until 2) {
            pendingBitmaps[i]?.recycle()
            pendingBitmaps[i] = null
        }
        GLES20.glDeleteTextures(2, textureHandles, 0)
    }

    private fun swapTextures() {
        currentTextureIndex = (currentTextureIndex + 1) % 2
    }

    override fun getFragmentShaderCode(): String {
        return "precision mediump float;" +
                "uniform sampler2D texture1;" +
                "uniform sampler2D texture2;" +
                "uniform float alpha;" +
                "varying vec2 outTexCoord;" +
                "void main() {" +
                "  vec4 color1 = texture2D(texture1, outTexCoord);" +
                "  vec4 color2 = texture2D(texture2, outTexCoord);" +
                "  gl_FragColor = mix(color1, color2, alpha);" +
                "}"
    }

    override fun getVertexShaderCode(): String {
        return "attribute vec4 vPosition;" +
                "attribute vec2 vTexCoord;" +
                "varying vec2 outTexCoord;" +
                "uniform vec2 uParallaxOffset;" +
                "uniform float uScale;" +
                "void main() {" +
                "  outTexCoord = vTexCoord + uParallaxOffset;" +
                "  gl_Position = vec4(vPosition.xy * uScale, 0.0, 1.0);" +
                "}"
    }
}
