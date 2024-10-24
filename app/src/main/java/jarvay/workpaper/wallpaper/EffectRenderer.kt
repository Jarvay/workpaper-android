package jarvay.workpaper.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.media.effect.Effect
import android.media.effect.EffectContext
import android.media.effect.EffectFactory
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10


class EffectsRenderer() : GLSurfaceView.Renderer {
    private var bmp: Bitmap? = null
    private var width = 0
    private var height = 0

    private val textures = IntArray(2)
    private var square: Square? = null

    private var effectContext: EffectContext? = null
    private var effect: Effect? = null

    fun updateBitmap(bitmap: Bitmap) {
        bmp = bitmap
        width = bitmap.width
        height = bitmap.height
        generateSquare()
    }

    override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?) {

    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        generateSquare()
    }

    override fun onDrawFrame(gl: GL10) {
        if (null == effectContext) {
            effectContext = EffectContext.createWithCurrentGlContext()
        }

        if (null != effect) {
            effect?.release()
        }

        grayScaleEffect()

        square?.draw(textures[1])
    }

    private fun grayScaleEffect() {
        val factory = effectContext!!.factory
        effect = factory.createEffect(EffectFactory.EFFECT_GRAYSCALE)
        effect!!.apply(textures[0], width, height, textures[1])
    }

    private fun generateSquare() {
        if (bmp == null) return
        GLES20.glGenTextures(2, textures, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0])

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE
        )

        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0)
        square = Square()
    }
}