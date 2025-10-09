package jarvay.workpaper.wallpaper

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class GLImageWallpaperRenderer : GLSurfaceView.Renderer, GLWallpaperRenderer() {
    private val textureHandles = IntArray(2)
    private var currentTextureIndex = 0
    private var transitionAlpha = 0f
    private var shouldSwapAfterTransition = false

    private val vertices = floatArrayOf(
        -1.0f, 1.0f, 0.0f, 0.0f, 0.0f,
        -1.0f, -1.0f, 0.0f, 0.0f, 1.0f,
        1.0f, -1.0f, 0.0f, 1.0f, 1.0f,
        1.0f, 1.0f, 0.0f, 1.0f, 0.0f,
    )

    private val vertexBuffer = ByteBuffer
        .allocateDirect(vertices.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply {
            put(vertices)
            position(0)
        }

    private val pendingBitmaps = arrayOfNulls<Bitmap?>(2)
    private val textureUploaded = booleanArrayOf(false, false)

    private var textureWidthRatio = 1.0f
    private var currentOffset = 0f
    private var screenWidth = 0
    private var screenHeight = 0

    override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        initProgram()
        GLES20.glGenTextures(2, textureHandles, 0)

        for (i in 0 until 2) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandles[i])
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_LINEAR
            )
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR
            )
        }
    }

    override fun onSurfaceChanged(gl10: GL10?, width: Int, height: Int) {
        screenWidth = width
        screenHeight = height
        GLES20.glViewport(0, 0, width, height)
        updateTextureCoordinates()
    }

    override fun onDrawFrame(p0: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        for (i in 0 until 2) {
            if (pendingBitmaps[i] != null && !textureUploaded[i]) {
                uploadTexture(i, pendingBitmaps[i]!!)
                pendingBitmaps[i] = null
                textureUploaded[i] = true
            }
        }

        GLES20.glUseProgram(program)

        vertexBuffer.position(0)
        GLES20.glVertexAttribPointer(0, 3, GLES20.GL_FLOAT, false, 20, vertexBuffer)
        GLES20.glEnableVertexAttribArray(0)

        vertexBuffer.position(3)
        GLES20.glVertexAttribPointer(1, 2, GLES20.GL_FLOAT, false, 20, vertexBuffer)
        GLES20.glEnableVertexAttribArray(1)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandles[currentTextureIndex])
        GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "texture1"), 0)

        if (transitionAlpha > 0f) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
            GLES20.glBindTexture(
                GLES20.GL_TEXTURE_2D,
                textureHandles[(currentTextureIndex + 1) % 2]
            )
            GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "texture2"), 1)
        }

        GLES20.glUniform1f(GLES20.glGetUniformLocation(program, "alpha"), transitionAlpha)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4)

        GLES20.glDisableVertexAttribArray(0)
        GLES20.glDisableVertexAttribArray(1)

        if (transitionAlpha >= 1f && shouldSwapAfterTransition) {
            swapTextures()
            transitionAlpha = 0f
            shouldSwapAfterTransition = false
        }
    }

    fun setBitmap(bitmap: Bitmap, isNext: Boolean = false) {
        val textureIndex = if (isNext) (currentTextureIndex + 1) % 2 else currentTextureIndex

        pendingBitmaps[textureIndex]?.recycle()
        pendingBitmaps[textureIndex] = bitmap
        textureUploaded[textureIndex] = false

        val scaleX = 1.0f
        val scaleY = 1.0f
        val verticesData = floatArrayOf(
            -scaleX, scaleY, 0.0f, 0.0f, 0.0f,
            -scaleX, -scaleY, 0.0f, 0.0f, 1.0f,
            scaleX, -scaleY, 0.0f, 1.0f, 1.0f,
            scaleX, scaleY, 0.0f, 1.0f, 0.0f,
        )
        vertexBuffer.clear()
        vertexBuffer.put(verticesData).position(0)

        val ratio = bitmap.width.toFloat() / screenWidth
        setTextureRatio(ratio)
    }

    fun setTextureRatio(widthRatio: Float) {
        textureWidthRatio = widthRatio
        updateTextureCoordinates()
    }

    fun updateOffset(offset: Float) {
        currentOffset = offset
        updateTextureCoordinates()
    }

    private fun updateTextureCoordinates() {
        val left: Float
        val right: Float

        if (textureWidthRatio > 1.0f) {
            val scrollableWidth = textureWidthRatio - 1.0f
            left = currentOffset * scrollableWidth / textureWidthRatio
            right = left + 1.0f / textureWidthRatio
        } else {
            left = 0f
            right = 1f
        }

        vertices[3] = left
        vertices[4] = 0f

        vertices[8] = left
        vertices[9] = 1f

        vertices[13] = right
        vertices[14] = 1f

        vertices[18] = right
        vertices[19] = 0f

        vertexBuffer.clear()
        vertexBuffer.put(vertices).position(0)
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
        return FRAGMENT_SHADER_CODE
    }

    override fun getVertexShaderCode(): String {
        return VERTEX_SHADER_CODE
    }

    companion object {
        private const val VERTEX_SHADER_CODE: String =
            "attribute vec4 vPosition;" +
                    "attribute vec2 vTexCoord;" +
                    "varying vec2 outTexCoord;" +
                    "void main() {" +
                    "  gl_Position = vPosition;" +
                    "  outTexCoord = vTexCoord;" +
                    "}"

        private const val FRAGMENT_SHADER_CODE: String =
            "precision mediump float;" +
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
}