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
    private val textureHandles = IntArray(1)
    private var textureHandle = 0
    var bitmap: Bitmap? = null

    private val vertices = FloatArray(20)
    private var vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
        .order(ByteOrder.nativeOrder()).asFloatBuffer()

    override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        initProgram()
        GLES20.glGenTextures(1, textureHandles, 0)
    }

    override fun onSurfaceChanged(gl10: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(p0: GL10?) {
        if (bitmap != null) {
            textureHandle = loadBitmap(bitmap!!)
            bitmap!!.recycle()
            bitmap = null
        }
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(program)

        vertexBuffer.position(0)
        GLES20.glVertexAttribPointer(0, 3, GLES20.GL_FLOAT, false, 20, vertexBuffer)
        GLES20.glEnableVertexAttribArray(0)

        vertexBuffer.position(3)
        GLES20.glVertexAttribPointer(1, 2, GLES20.GL_FLOAT, false, 20, vertexBuffer)
        GLES20.glEnableVertexAttribArray(1)

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4)

        GLES20.glDisableVertexAttribArray(0)
        GLES20.glDisableVertexAttribArray(1)
    }

    override fun getFragmentShaderCode(): String {
        return FRAGMENT_SHADER_CODE
    }

    override fun getVertexShaderCode(): String {
        return VERTEX_SHADER_CODE
    }

    private fun loadBitmap(bitmap: Bitmap): Int {
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

        if (textureHandles[0] != 0) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandles[0])
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

            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        }

        if (textureHandles[0] == 0) {
            throw RuntimeException("Error loading texture.")
        }

        return textureHandles[0]
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
                    "uniform sampler2D texture;" +
                    "varying vec2 outTexCoord;" +
                    "void main() {" +
                    "  gl_FragColor = texture2D(texture, outTexCoord);" +
                    "}"
    }
}