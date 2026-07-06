package jarvay.workpaper.wallpaper

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import android.util.Size
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import jarvay.workpaper.data.wallpaper.WallpaperType
import jarvay.workpaper.service.LiveWallpaperService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class WallpaperRenderer @OptIn(UnstableApi::class) constructor
    (
    private val surfaceView: LiveWallpaperService.LiveWallpaperEngine.GLWallpaperSurfaceView,
    private val scope: CoroutineScope
) : GLSurfaceView.Renderer {
    val imageRenderer = GLImageWallpaperRenderer()
    val depthLayerRenderer = GLDepthLayerRenderer()
    var videoRenderer = GLVideoWallpaperRenderer()
    var wallpaperType = WallpaperType.IMAGE
    var parallaxSensorManager: ParallaxSensorManager? = null
    var useDepthLayers = false

    val surfaceSize = MutableStateFlow(Size(0, 0))
    private var lastUseDepthLayers = false
    private var depthStrength = 0.15f

    override fun onSurfaceCreated(gl10: GL10, p1: EGLConfig) {
        imageRenderer.onSurfaceCreated(gl10, p1)
        depthLayerRenderer.onSurfaceCreated(gl10, p1)
        videoRenderer.onSurfaceCreated(gl10, p1)
    }

    override fun onSurfaceChanged(gl10: GL10, width: Int, height: Int) {
        surfaceSize.value = Size(width, height)
        imageRenderer.onSurfaceChanged(gl10, width, height)
        depthLayerRenderer.onSurfaceChanged(gl10, width, height)
        videoRenderer.onSurfaceChanged(gl10, width, height)
    }

    override fun onDrawFrame(gl10: GL10) {
        if (lastUseDepthLayers != useDepthLayers) {
            Log.d(TAG, "Switching renderer: useDepthLayers=$useDepthLayers, wallpaperType=$wallpaperType")
            lastUseDepthLayers = useDepthLayers
        }

        parallaxSensorManager?.let { sensor ->
            sensor.update()
            if (useDepthLayers) {
                depthLayerRenderer.updateParallaxOffset(sensor.tiltX, sensor.tiltY, sensor.sensitivity)
            } else {
                imageRenderer.updateParallaxOffset(sensor.tiltX, sensor.tiltY, sensor.sensitivity)
            }
        }

        when (wallpaperType) {
            WallpaperType.IMAGE -> {
                if (useDepthLayers) {
                    depthLayerRenderer.onDrawFrame(gl10)
                } else {
                    imageRenderer.onDrawFrame(gl10)
                }
            }
            WallpaperType.VIDEO -> videoRenderer.onDrawFrame(gl10)
        }
    }

    fun triggerWakeAnimation() {
        imageRenderer.triggerWakeAnimation()
        depthLayerRenderer.triggerWakeAnimation()
    }

    fun setDepthStrength(strength: Float) {
        depthStrength = strength
        depthLayerRenderer.setDepthStrength(strength)
    }

    fun updateWallpaperType(
        type: WallpaperType,
        parallaxFrameRate: Int = 0
    ) {
        wallpaperType = type
        surfaceView.renderMode = when {
            type == WallpaperType.VIDEO -> GLSurfaceView.RENDERMODE_CONTINUOUSLY
            parallaxFrameRate >= 60 -> GLSurfaceView.RENDERMODE_CONTINUOUSLY
            parallaxFrameRate > 0 -> GLSurfaceView.RENDERMODE_WHEN_DIRTY
            else -> GLSurfaceView.RENDERMODE_WHEN_DIRTY
        }
    }

    fun destroy() {
        imageRenderer.onDestroy()
        depthLayerRenderer.onDestroy()
    }

    companion object {
        private const val TAG = "WallpaperRenderer"
    }
}
