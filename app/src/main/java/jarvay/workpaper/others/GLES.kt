package jarvay.workpaper.others

import android.opengl.GLES20
import androidx.annotation.OptIn
import androidx.media3.common.util.GlUtil.checkGlError
import androidx.media3.common.util.UnstableApi

object GLES {
    private const val VERTEX_SHADER_CODE = "" +
            // This matrix member variable provides a hook to manipulate
            // the coordinates of the objects that use this vertex shader
            "uniform mat4 uMVPMatrix;" +
            "attribute vec4 aPosition;" +
            "void main(){" +
            "  gl_Position = uMVPMatrix * aPosition;" +
            "}"

    private const val FRAGMENT_SHADER_CODE = "" +
            "precision mediump float;" +
            "uniform sampler2D uTexture;" +
            "uniform vec4 uColor;" +
            "void main(){" +
            "  gl_FragColor = uColor;" +
            "}"

    private var PROGRAM_HANDLE: Int = 0
    private var ATTRIB_POSITION_HANDLE: Int = 0
    private var UNIFORM_COLOR_HANDLE: Int = 0
    private var UNIFORM_MVP_MATRIX_HANDLE: Int = 0

    fun initGl() {
        // Initialize shaders and create/link program
        val vertexShaderHandle = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER_CODE)
        val fragShaderHandle = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_CODE)

        PROGRAM_HANDLE = createAndLinkProgram(vertexShaderHandle, fragShaderHandle, null)
        ATTRIB_POSITION_HANDLE = GLES20.glGetAttribLocation(PROGRAM_HANDLE, "aPosition")
        UNIFORM_MVP_MATRIX_HANDLE = GLES20.glGetUniformLocation(PROGRAM_HANDLE, "uMVPMatrix")
        UNIFORM_COLOR_HANDLE = GLES20.glGetUniformLocation(PROGRAM_HANDLE, "uColor")
    }

    fun loadShader(type: Int, shaderCode: String): Int {
        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        val shaderHandle = GLES20.glCreateShader(type)

        // add the source code to the shader and compile it
        GLES20.glShaderSource(shaderHandle, shaderCode)
        GLES20.glCompileShader(shaderHandle)
        return shaderHandle
    }

    fun createAndLinkProgram(
        vertexShaderHandle: Int,
        fragShaderHandle: Int,
        attributes: Array<String>?
    ): Int {
        val programHandle = GLES20.glCreateProgram()
//        checkGlError("glCreateProgram")
        GLES20.glAttachShader(programHandle, vertexShaderHandle)
        GLES20.glAttachShader(programHandle, fragShaderHandle)
        if (attributes != null) {
            val size = attributes.size
            for (i in 0 until size) {
                GLES20.glBindAttribLocation(programHandle, i, attributes[i])
            }
        }
        GLES20.glLinkProgram(programHandle)
//        checkGlError("glLinkProgram")
        GLES20.glDeleteShader(vertexShaderHandle)
        GLES20.glDeleteShader(fragShaderHandle)
        return programHandle
    }
}