package jarvay.workpaper.wallpaper

import android.opengl.GLSurfaceView
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import jarvay.workpaper.data.wallpaper.WallpaperType
import jarvay.workpaper.service.LiveWallpaperService
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class WallpaperRenderer @OptIn(UnstableApi::class) constructor
    (
    private val surfaceView: LiveWallpaperService.LiveWallpaperEngine.GLWallpaperSurfaceView
) : GLSurfaceView.Renderer {
    val imageRenderer = GLImageWallpaperRenderer()
    var videoRenderer = GLVideoWallpaperRenderer()
    var wallpaperType = WallpaperType.IMAGE

    override fun onSurfaceCreated(p0: GL10, p1: EGLConfig) {
        imageRenderer.onSurfaceCreated(p0, p1)
        videoRenderer.onSurfaceCreated(p0, p1)
    }

    override fun onSurfaceChanged(p0: GL10, width: Int, height: Int) {
        imageRenderer.onSurfaceChanged(p0, width, height)
        videoRenderer.onSurfaceChanged(p0, width, height)
    }

    override fun onDrawFrame(p0: GL10) {
        when (wallpaperType) {
            WallpaperType.IMAGE -> imageRenderer.onDrawFrame(p0)
            WallpaperType.VIDEO -> videoRenderer.onDrawFrame(p0)
        }
    }

    fun updateWallpaperType(type: WallpaperType) {
        wallpaperType = type
        surfaceView.renderMode = when (type) {
            WallpaperType.IMAGE -> GLSurfaceView.RENDERMODE_WHEN_DIRTY
            WallpaperType.VIDEO -> GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
    }
}