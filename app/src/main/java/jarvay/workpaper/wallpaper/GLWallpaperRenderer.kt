package jarvay.workpaper.wallpaper

import android.opengl.GLES20

abstract class GLWallpaperRenderer {
    protected var program = 0

    protected abstract fun getFragmentShaderCode(): String
    protected abstract fun getVertexShaderCode(): String

    protected fun initProgram() {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, getVertexShaderCode())
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, getFragmentShaderCode())

        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        return shader
    }
}