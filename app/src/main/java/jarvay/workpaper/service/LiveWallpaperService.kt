package jarvay.workpaper.service

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.hardware.display.DisplayManager
import android.media.MediaPlayer
import android.net.Uri
import android.opengl.GLSurfaceView
import android.os.Build
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.Surface
import android.view.SurfaceHolder
import androidx.core.net.toUri
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.AndroidEntryPoint
import jarvay.workpaper.Workpaper
import jarvay.workpaper.others.GLBitmapRenderer
import jarvay.workpaper.others.GLES
import jarvay.workpaper.others.bitmapFromContentUri
import jarvay.workpaper.others.centerCrop
import jarvay.workpaper.others.getScreenSize
import jarvay.workpaper.others.scaleFixedRatio
import jarvay.workpaper.receiver.WallpaperReceiver
import jarvay.workpaper.wallpaper.EffectsRenderer
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject


@AndroidEntryPoint
class LiveWallpaperService : WallpaperService() {
    @Inject
    lateinit var workpaper: Workpaper

    override fun onCreateEngine(): Engine {
        return LiveWallpaperEngine()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(javaClass.simpleName, "onDestroy")
    }

    inner class LiveWallpaperEngine : Engine() {
        private val paint = Paint()

        private var mediaPlayer = ExoPlayer.Builder(this@LiveWallpaperService).build()

        private var glSurfaceView: WallpaperGLSurfaceView? = null
        private var renderer: EffectsRenderer? = null

        private val imageReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                Log.d("imageReceiver", "imageReceiver")
                if (context == null || intent == null) return
                if (intent.action != ACTION_UPDATE_IMAGE) return

                val contentUri = intent.getStringExtra(EXTRA_IMAGE_CONTENT_URI) ?: return
                var bitmap =
                    bitmapFromContentUri(contentUri.toUri(), this@LiveWallpaperService) ?: return
                bitmap = runBlocking {
                    workpaper.handleBitmapStyle(bitmap)
                }
                setImageBitmap(bitmap)
            }
        }

        private val videoReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (context == null || intent == null) return
                if (intent.action != ACTION_UPDATE_VIDEO) return

                val contentUri = intent.getStringExtra(EXTRA_VIDEO_CONTENT_URI) ?: ""
                Log.d("contentUri", contentUri)
                if (contentUri.isEmpty()) return

            }
        }

        init {
            setTouchEventsEnabled(true)
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

        @SuppressLint("UnspecifiedRegisterReceiverFlag")
        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)

            val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
            val configurationInfo = am.deviceConfigurationInfo
            val supportEs2 = configurationInfo.reqGlEsVersion >= 0x20000
            Log.d("supportEs2", supportEs2.toString())
            initGLSurfaceView()
//            GLES.initGl()

            val imageIntentFilter = IntentFilter(ACTION_UPDATE_IMAGE)
            val videoIntentFilter = IntentFilter(ACTION_UPDATE_VIDEO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(imageReceiver, imageIntentFilter, RECEIVER_EXPORTED)
                registerReceiver(videoReceiver, videoIntentFilter, RECEIVER_EXPORTED)
            } else {
                registerReceiver(imageReceiver, imageIntentFilter)
                registerReceiver(videoReceiver, videoIntentFilter)
            }

            Log.d(javaClass.simpleName, "onCreate")
        }

        private fun initGLSurfaceView() {
            glSurfaceView = WallpaperGLSurfaceView(this@LiveWallpaperService)
            glSurfaceView!!.setEGLContextClientVersion(2)
            renderer = EffectsRenderer()
            glSurfaceView!!.setRenderer(renderer)
            glSurfaceView!!.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
            Log.d(javaClass.simpleName, Thread.currentThread().name)
        }

        override fun onTouchEvent(event: MotionEvent?) {
            Log.d("onTouchEvent", event.toString())
            super.onTouchEvent(event)
            if (event == null) return
            gestureDetector.onTouchEvent(event)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            Log.d("onSurfaceCreated", "onSurfaceCreated")

            mediaPlayer.setVideoSurface(holder.surface)
            if (currentVideoUri().isNotEmpty()) {

            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder?) {
            super.onSurfaceDestroyed(holder)
            destroyMediaPlayer()
        }

        private fun initMediaPlayer(uri: Uri) {

        }

        private fun setImageBitmap(originBitmap: Bitmap) {
            if (!surfaceHolder.surface.isValid) return

            destroyMediaPlayer()

            var canvas: Canvas?
            var alpha = 0

            val displayManager =
                this@LiveWallpaperService.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            val displayMode = displayManager.displays[0].mode

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                surfaceHolder.surface.setFrameRate(
                    displayMode.refreshRate,
                    Surface.FRAME_RATE_COMPATIBILITY_FIXED_SOURCE
                )
            }

//            canvas = surfaceHolder.lockCanvas()
            val screenSize = getScreenSize(this@LiveWallpaperService)
            val bitmap = originBitmap.scaleFixedRatio(
                targetWidth = screenSize.width,
                targetHeight = screenSize.height,
                useMin = false
            ).centerCrop(
                targetWidth = screenSize.width,
                targetHeight = screenSize.height
            )
            renderer!!.updateBitmap(bitmap)
            Log.d(javaClass.simpleName, Thread.currentThread().name)
            glSurfaceView?.requestRender()

//            canvas?.let { surfaceHolder.unlockCanvasAndPost(canvas) }

//            while (alpha <= 255) {
//                canvas = surfaceHolder.lockCanvas()
//
//                paint.alpha = alpha
//                canvas.drawBitmap(bitmap, 0F, 0F, paint)
//
//                alpha += 5
//                Thread.sleep(10)
//
//                canvas?.let {
//                    surfaceHolder.unlockCanvasAndPost(it)
//                }
//            }
        }

        private fun destroyMediaPlayer() {
        }

        private fun currentVideoUri(): String {
            return runBlocking {
                workpaper.runningPreferencesRepository.runningPreferencesFlow.first().currentVideoContentUri
            }
        }

        override fun onDestroy() {
            super.onDestroy()
            unregisterReceiver(imageReceiver)
            unregisterReceiver(videoReceiver)
            destroyMediaPlayer()
        }

        inner class WallpaperGLSurfaceView internal constructor(context: Context) :
            GLSurfaceView(context) {
            override fun getHolder(): SurfaceHolder {
                return surfaceHolder
            }

            fun onWallpaperDestroy() {
                super.onDetachedFromWindow()
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