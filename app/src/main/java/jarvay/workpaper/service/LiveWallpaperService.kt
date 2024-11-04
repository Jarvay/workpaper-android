package jarvay.workpaper.service

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.opengl.GLSurfaceView
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.SurfaceHolder
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import dagger.hilt.android.AndroidEntryPoint
import jarvay.workpaper.Workpaper
import jarvay.workpaper.data.wallpaper.WallpaperType
import jarvay.workpaper.others.bitmapFromContentUri
import jarvay.workpaper.others.centerCrop
import jarvay.workpaper.others.getScreenSize
import jarvay.workpaper.others.scaleFixedRatio
import jarvay.workpaper.receiver.WallpaperReceiver
import jarvay.workpaper.wallpaper.WallpaperRenderer
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class LiveWallpaperService : WallpaperService() {
    @Inject
    lateinit var workpaper: Workpaper

    private var prevImageUri: String? = null

    @OptIn(UnstableApi::class)
    override fun onCreateEngine(): Engine {
        return LiveWallpaperEngine()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(javaClass.simpleName, "onDestroy")
    }

    @UnstableApi
    inner class LiveWallpaperEngine : Engine() {
        private var surfaceView: GLWallpaperSurfaceView? = null
        private var renderer: WallpaperRenderer? = null
        private val player: MediaPlayer = MediaPlayer()

        init {
            player.apply {
                setVolume(0f, 0f)
                isLooping = true
            }
        }

        private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                val result = super.onDoubleTap(e)
                Log.d(javaClass.simpleName, "onDoubleTap")
                val intent = Intent(this@LiveWallpaperService, WallpaperReceiver::class.java)
                sendBroadcast(intent)

                return result
            }
        }
        private val gestureDetector = GestureDetector(this@LiveWallpaperService, gestureListener)

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)

            MainScope().launch {
                workpaper.imageUri.collect {
                    Log.d("imageUri collected", it.toString())
                    if (it == null) return@collect
                    setImageBitmap(it.toUri())
                }
            }
            MainScope().launch {
                workpaper.videoUri.collect {
                    Log.d("videoUri collected", it.toString())
                    if (it == null) return@collect
                    startVideo(it.toUri())
                }
            }

            val i = Intent(this@LiveWallpaperService, WorkpaperService::class.java)
            startService(i)

            Log.d(javaClass.simpleName, "onCreate")
        }

        override fun onTouchEvent(event: MotionEvent?) {
            Log.d("onTouchEvent", event.toString())
            super.onTouchEvent(event)
            if (event == null) return
            gestureDetector.onTouchEvent(event)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            if (renderer?.wallpaperType != WallpaperType.VIDEO) return
            if (visible) {
                Log.d("onVisibilityChanged", "true")
                player.start()
            } else {
                Log.d("onVisibilityChanged", "false")
                player.pause()
            }
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            initSurfaceView()
            Log.d("onSurfaceCreated", "onSurfaceCreated")
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder?) {
            super.onSurfaceDestroyed(holder)
            stopVideo()
            destroySurfaceView()
        }

        private fun setImageBitmap(uri: Uri) {
            if (!surfaceHolder.surface.isValid) return

            renderer?.updateWallpaperType(WallpaperType.IMAGE)
            stopVideo()


            val originBitmap = loadBitmap(uri) ?: return
            val prevBitmap = loadBitmap(prevImageUri?.toUri())

            var alpha = 50
            while (alpha <= 255) {
                val alphaBitmap = bitmapWithTransition(prevBitmap, originBitmap, alpha)

                renderer?.imageRenderer?.bitmap = alphaBitmap
                surfaceView?.requestRender()

                alpha += 5
                Thread.sleep(10)
            }

            prevImageUri = uri.toString()
        }

        private fun bitmapWithTransition(
            backgroundBitmap: Bitmap?,
            frontBitmap: Bitmap,
            alpha: Int
        ): Bitmap {
            val result = Bitmap.createBitmap(
                frontBitmap.width,
                frontBitmap.height,
                frontBitmap.config
            )

            val canvas = Canvas(result)
            val paint = Paint()
            if (backgroundBitmap != null) {
                canvas.drawBitmap(backgroundBitmap, 0f, 0f, paint)
            }
            paint.alpha = alpha
            canvas.drawBitmap(frontBitmap, 0f, 0f, paint)

            return result
        }

        private fun loadBitmap(uri: Uri?): Bitmap? {
            if (uri == null) return null

            val screenSize = getScreenSize(this@LiveWallpaperService)

            val originBitmap =
                bitmapFromContentUri(uri, this@LiveWallpaperService)
                    ?: return null
            var bitmap = originBitmap.scaleFixedRatio(
                targetWidth = screenSize.width,
                targetHeight = screenSize.height,
                useMin = false
            ).centerCrop(
                targetWidth = screenSize.width,
                targetHeight = screenSize.height
            )
            bitmap = runBlocking {
                workpaper.handleBitmapStyle(bitmap)
            }

            return bitmap
        }

        private fun initSurfaceView() {
            if (surfaceView != null) return

            surfaceView = GLWallpaperSurfaceView(this@LiveWallpaperService)

            renderer = renderer ?: WallpaperRenderer(surfaceView!!)
            val width = surfaceHolder.surfaceFrame.width()
            val height = surfaceHolder.surfaceFrame.height()
            renderer!!.videoRenderer.setScreenSize(width, height)

            surfaceView!!.setEGLContextClientVersion(2)
            surfaceView!!.apply {
                setRenderer(renderer)
                preserveEGLContextOnPause = true
                renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
            }
        }

        private fun destroySurfaceView() {
            surfaceView?.detach()
            surfaceView = null
            renderer = null
        }

        private fun startVideo(uri: Uri) {
            if (renderer == null) return

            renderer!!.videoRenderer.setSourcePlayer(player)
            renderer!!.updateWallpaperType(WallpaperType.VIDEO)

            updateVideoInfo(uri)

            player.apply {
                reset()
                setVolume(0f, 0f)
                isLooping = true
                setDataSource(this@LiveWallpaperService, uri)
                setOnPreparedListener {
                    it.start()
                }
                prepareAsync()
            }

            prevImageUri = null
        }

        private fun stopVideo() {
            player.apply {
                if (isPlaying) {
                    stop()
                }
            }
        }

        private fun updateVideoInfo(uri: Uri) {
            if (renderer == null) return

            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(this@LiveWallpaperService, uri)
            val rotation = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION
            )!!.toInt()
            val width = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
            )!!.toInt()
            val height = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
            )!!.toInt()
            retriever.release()
            renderer!!.videoRenderer.setVideoSizeAndRotation(
                width = width,
                height = height,
                rotation = rotation
            )
        }

        inner class GLWallpaperSurfaceView(
            context: Context
        ) : GLSurfaceView(context) {
            override fun getHolder(): SurfaceHolder {
                return surfaceHolder
            }

            fun detach() {
                onDetachedFromWindow()
            }
        }
    }

    companion object {
        const val ACTION_UPDATE_IMAGE = "action_update_image"
        const val ACTION_UPDATE_VIDEO = "action_update_video"

        const val EXTRA_IMAGE_CONTENT_URI = "extraImageContentUri"
        const val EXTRA_VIDEO_CONTENT_URI = "extraVideoContentUri"
    }
}