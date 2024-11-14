package jarvay.workpaper.wallpaper

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Size
import androidx.annotation.FloatRange
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import jarvay.workpaper.data.wallpaper.WallpaperType
import jarvay.workpaper.service.LiveWallpaperService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class WallpaperRenderer @OptIn(UnstableApi::class) constructor
    (
    private val surfaceView: LiveWallpaperService.LiveWallpaperEngine.GLWallpaperSurfaceView,
    private val scope: CoroutineScope
) : GLSurfaceView.Renderer {
    val imageRenderer = GLImageWallpaperRenderer()
    var videoRenderer = GLVideoWallpaperRenderer()
    var wallpaperType = WallpaperType.IMAGE

    val surfaceSize = MutableStateFlow(Size(0, 0))
    private var scale = 1.0f

    override fun onSurfaceCreated(gl10: GL10, p1: EGLConfig) {
        imageRenderer.onSurfaceCreated(gl10, p1)
        videoRenderer.onSurfaceCreated(gl10, p1)
    }

    override fun onSurfaceChanged(gl10: GL10, width: Int, height: Int) {
        surfaceSize.value = Size(width, height)
        imageRenderer.onSurfaceChanged(gl10, width, height)
        videoRenderer.onSurfaceChanged(gl10, width, height)
    }

    override fun onDrawFrame(gl10: GL10) {
        if (scale != 1.0f) {
            val scaleWidth = (surfaceSize.value.width * scale).toInt()
            val scaleHeight = (surfaceSize.value.height * scale).toInt()
            val offsetX = (scaleWidth - surfaceSize.value.width) / 2
            val offsetY = (scaleHeight - surfaceSize.value.height) / 2

            GLES20.glViewport(
                -offsetX,
                -offsetY,
                scaleWidth,
                scaleHeight
            )
        }
        when (wallpaperType) {
            WallpaperType.IMAGE -> imageRenderer.onDrawFrame(gl10)
            WallpaperType.VIDEO -> videoRenderer.onDrawFrame(gl10)
        }
    }

    fun updateWallpaperType(type: WallpaperType) {
        wallpaperType = type
        surfaceView.renderMode = when (type) {
            WallpaperType.IMAGE -> GLSurfaceView.RENDERMODE_WHEN_DIRTY
            WallpaperType.VIDEO -> GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
    }

    fun scaleTransition(@FloatRange(from = 1.0) scale: Float) {
        scope.launch(Dispatchers.IO) {
            var current = scale
            while (current > 1.0f) {
                this@WallpaperRenderer.scale = current
                surfaceView.requestRender()
                current -= 0.01f
                delay(10)
            }
            this@WallpaperRenderer.scale = 1.0f
        }
    }
}