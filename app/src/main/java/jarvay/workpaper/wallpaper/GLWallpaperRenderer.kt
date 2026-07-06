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

        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            val log = GLES20.glGetProgramInfoLog(program)
            GLES20.glDeleteProgram(program)
            throw RuntimeException("Program link failed: $log")
        }
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)

        val compileStatus = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] == 0) {
            val log = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            throw RuntimeException("Shader compile failed ($type): $log")
        }

        return shader
    }
}