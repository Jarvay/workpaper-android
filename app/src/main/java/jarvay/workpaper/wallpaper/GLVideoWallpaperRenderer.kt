/*
 * Copyright 2019 Alynx Zhou
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jarvay.workpaper.wallpaper

import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.view.Surface
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class GLVideoWallpaperRenderer : GLWallpaperRenderer(), GLSurfaceView.Renderer {
    private val vertices: FloatBuffer
    private val texCoords: FloatBuffer
    private val indices: IntBuffer
    private val textures: IntArray
    private val buffers: IntArray
    private val mvp: FloatArray
    private var mvpLocation = 0
    private var positionLocation = 0
    private var texCoordLocation = 0
    private var surfaceTexture: SurfaceTexture? = null
    private var screenWidth = 0
    private var screenHeight = 0
    private var videoWidth = 0
    private var videoHeight = 0
    private var videoRotation = 0
    private var xOffset = 0f
    private var yOffset = 0f
    private var maxXOffset = 0f
    private var maxYOffset = 0f

    // Fix bug like https://stackoverflow.com/questions/14185661/surfacetexture-onframeavailablelistener-stops-being-called
    private var updatedFrame: Long = 0
    private var renderedFrame: Long = 0

    init {
        val vertexArray = floatArrayOf(
            -1.0f, -1.0f,
            -1.0f, 1.0f,
            1.0f, -1.0f,
            1.0f, 1.0f
        )
        vertices = ByteBuffer.allocateDirect(
            vertexArray.size * BYTES_PER_FLOAT
        ).order(ByteOrder.nativeOrder()).asFloatBuffer()
        vertices.put(vertexArray).position(0)
        val texCoordArray = floatArrayOf( // u, v
            0.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 1.0f,
            1.0f, 0.0f
        )
        texCoords = ByteBuffer.allocateDirect(
            texCoordArray.size * BYTES_PER_FLOAT
        ).order(ByteOrder.nativeOrder()).asFloatBuffer()
        texCoords.put(texCoordArray).position(0)
        val indexArray = intArrayOf(
            0, 1, 2,
            3, 2, 1
        )
        indices = ByteBuffer.allocateDirect(
            indexArray.size * BYTES_PER_INT
        ).order(ByteOrder.nativeOrder()).asIntBuffer()
        indices.put(indexArray).position(0)
        buffers = IntArray(3)
        textures = IntArray(1)
        mvp = floatArrayOf(
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
        )
    }

    override fun onSurfaceCreated(
        gl10: GL10,
        eglConfig: EGLConfig
    ) {
        // No depth test for 2D video.
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthMask(false)
        GLES20.glDisable(GLES20.GL_CULL_FACE)
        GLES20.glDisable(GLES20.GL_BLEND)
        GLES20.glGenTextures(textures.size, textures, 0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0])
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_LINEAR
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE
        )
        initProgram()
        mvpLocation = GLES20.glGetUniformLocation(program, "mvp")
        // Locations are NOT set in shader sources.
        positionLocation = GLES20.glGetAttribLocation(program, "in_position")
        texCoordLocation = GLES20.glGetAttribLocation(program, "in_tex_coord")

        GLES20.glGenBuffers(buffers.size, buffers, 0)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[0])
        GLES20.glBufferData(
            GLES20.GL_ARRAY_BUFFER, vertices.capacity() * BYTES_PER_FLOAT,
            vertices, GLES20.GL_STATIC_DRAW
        )
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[1])
        GLES20.glBufferData(
            GLES20.GL_ARRAY_BUFFER, texCoords.capacity() * BYTES_PER_FLOAT,
            texCoords, GLES20.GL_STATIC_DRAW
        )
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, buffers[2])
        GLES20.glBufferData(
            GLES20.GL_ELEMENT_ARRAY_BUFFER, indices.capacity() * BYTES_PER_INT,
            indices, GLES20.GL_STATIC_DRAW
        )
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
    }

    override fun onSurfaceChanged(
        gl10: GL10, width: Int, height: Int
    ) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(
        gl10: GL10
    ) {
        if (surfaceTexture == null) {
            return
        }
        if (renderedFrame < updatedFrame) {
            try {
                surfaceTexture!!.updateTexImage()
                ++renderedFrame
            } catch (e: Exception) {
                e.printStackTrace()
            }
            // Utils.debug(
            //     TAG, "renderedFrame: " + renderedFrame + " updatedFrame: " + updatedFrame
            // );
        }
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(program)
        GLES20.glUniformMatrix4fv(mvpLocation, 1, false, mvp, 0)
        // No vertex array in OpenGL ES 2.
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[0])
        GLES20.glEnableVertexAttribArray(positionLocation)
        GLES20.glVertexAttribPointer(
            positionLocation, 2, GLES20.GL_FLOAT, false, 2 * BYTES_PER_FLOAT, 0
        )
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[1])
        GLES20.glEnableVertexAttribArray(texCoordLocation)
        GLES20.glVertexAttribPointer(
            texCoordLocation, 2, GLES20.GL_FLOAT, false, 2 * BYTES_PER_FLOAT, 0
        )
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, buffers[2])
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_INT, 0)
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)
        GLES20.glDisableVertexAttribArray(texCoordLocation)
        GLES20.glDisableVertexAttribArray(positionLocation)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        GLES20.glUseProgram(0)
    }

    fun setSourcePlayer(player: MediaPlayer) {
        // Re-create SurfaceTexture when getting a new player.
        // Because maybe a new video is loaded.
        createSurfaceTexture()
        player.setSurface(Surface(surfaceTexture))
    }

    fun setScreenSize(width: Int, height: Int) {
        if (screenWidth != width || screenHeight != height) {
            screenWidth = width
            screenHeight = height
//            debug(TAG, String.format(
//                Locale.US, "Set screen size to %dx%d", screenWidth, screenHeight
//            ))
            maxXOffset =
                (1.0f - screenWidth.toFloat() / screenHeight / (videoWidth.toFloat() / videoHeight)) / 2
            maxYOffset =
                (1.0f - screenHeight.toFloat() / screenWidth / (videoHeight.toFloat() / videoWidth)) / 2
            updateMatrix()
        }
    }

    fun setVideoSizeAndRotation(width: Int, height: Int, rotation: Int) {
        // MediaMetadataRetriever always give us raw width and height and won't rotate them.
        // So we rotate them by ourselves.
        var newWidth = width
        var newHeight = height
        if (rotation % 180 != 0) {
            val swap = newWidth
            newWidth = newHeight
            newHeight = swap
        }
        if (videoWidth != newWidth || videoHeight != newHeight || videoRotation != rotation) {
            videoWidth = newWidth
            videoHeight = newHeight
            videoRotation = rotation
//            debug(TAG, String.format(
//                Locale.US, "Set video size to %dx%d", videoWidth, videoHeight
//            ))
//            debug(TAG, String.format(
//                Locale.US, "Set video rotation to %d", videoRotation
//            ))
            maxXOffset =
                (1.0f - screenWidth.toFloat() / screenHeight / (videoWidth.toFloat() / videoHeight)) / 2
            maxYOffset =
                (1.0f - screenHeight.toFloat() / screenWidth / (videoHeight.toFloat() / videoWidth)) / 2
            updateMatrix()
        }
    }

    fun setOffset(xOffset: Float, yOffset: Float) {
        var newXOffset = xOffset
        var newYOffset = yOffset
        if (newXOffset > maxXOffset) {
            newXOffset = maxXOffset
        }
        if (newXOffset < -maxXOffset) {
            newXOffset = -maxXOffset
        }
        if (newYOffset > maxYOffset) {
            newYOffset = maxYOffset
        }
        if (newYOffset < -maxXOffset) {
            newYOffset = -maxYOffset
        }
        if (this.xOffset != newXOffset || this.yOffset != newYOffset) {
            this.xOffset = newXOffset
            this.yOffset = newYOffset
//            debug(TAG, String.format(
//                Locale.US, "Set offset to %fx%f", this.xOffset, this.yOffset
//            ))
            updateMatrix()
        }
    }

    private fun createSurfaceTexture() {
        if (surfaceTexture != null) {
            surfaceTexture!!.release()
            surfaceTexture = null
        }
        updatedFrame = 0
        renderedFrame = 0
        surfaceTexture = SurfaceTexture(textures[0])
        surfaceTexture!!.setDefaultBufferSize(videoWidth, videoHeight)
        surfaceTexture!!.setOnFrameAvailableListener { ++updatedFrame }
    }

    private fun updateMatrix() {
        // Players are buggy and unclear, so we do crop by ourselves.
        // Start with an identify matrix.
        for (i in 0..15) {
            mvp[i] = 0.0f
        }
        mvp[15] = 1.0f
        mvp[10] = mvp[15]
        mvp[5] = mvp[10]
        mvp[0] = mvp[5]
        // OpenGL model matrix: scaling, rotating, translating.
        val videoRatio = videoWidth.toFloat() / videoHeight
        val screenRatio = screenWidth.toFloat() / screenHeight
        if (videoRatio >= screenRatio) {
//            debug(TAG, "X-cropping")
            // Treat video and screen width as 1, and compare width to scale.
            Matrix.scaleM(
                mvp, 0,
                videoWidth.toFloat() / videoHeight / (screenWidth.toFloat() / screenHeight), 1f, 1f
            )
            // Some video recorder save video frames in direction differs from recoring,
            // and add a rotation metadata. Need to detect and rotate them.
            if (videoRotation % 360 != 0) {
                Matrix.rotateM(mvp, 0, -videoRotation.toFloat(), 0f, 0f, 1f)
            }
            Matrix.translateM(mvp, 0, xOffset, 0f, 0f)
        } else {
//            debug(TAG, "Y-cropping")
            // Treat video and screen height as 1, and compare height to scale.
            Matrix.scaleM(
                mvp, 0, 1f,
                videoHeight.toFloat() / videoWidth / (screenHeight.toFloat() / screenWidth), 1f
            )
            // Some video recorder save video frames in direction differs from recoring,
            // and add a rotation metadata. Need to detect and rotate them.
            if (videoRotation % 360 != 0) {
                Matrix.rotateM(mvp, 0, -videoRotation.toFloat(), 0f, 0f, 1f)
            }
            Matrix.translateM(mvp, 0, 0f, yOffset, 0f)
        }
        // This is a 2D center crop, so we only need model matrix, no view and projection.
    }

    override fun getFragmentShaderCode(): String {
        return FRAGMENT_SHADER_CODE
    }

    override fun getVertexShaderCode(): String {
        return VERTEX_SHADER_CODE
    }

    companion object {
        private const val BYTES_PER_FLOAT = 4
        private const val BYTES_PER_INT = 4

        private const val VERTEX_SHADER_CODE: String =
            "#version 100\n" +
                    "attribute vec2 in_position;" +
                    "attribute vec2 in_tex_coord;" +
                    "uniform mat4 mvp;" +
                    "varying vec2 tex_coord;" +
                    "void main() {" +
                    "  gl_Position = mvp * vec4(in_position, 1.0, 1.0);" +
                    "  tex_coord = in_tex_coord;" +
                    "}"

        private const val FRAGMENT_SHADER_CODE: String =
            "#version 100\n" +
                    "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;" +
                    "uniform samplerExternalOES frame;" +
                    "varying vec2 tex_coord;" +
                    "void main() {" +
                    "  gl_FragColor = texture2D(frame, tex_coord);" +
                    "}"

    }
}
