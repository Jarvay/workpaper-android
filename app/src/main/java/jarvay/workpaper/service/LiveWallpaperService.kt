package jarvay.workpaper.service

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.Surface
import android.view.SurfaceHolder
import dagger.hilt.android.AndroidEntryPoint
import jarvay.workpaper.Workpaper
import jarvay.workpaper.data.preferences.SettingsPreferencesRepository
import jarvay.workpaper.others.centerCrop
import jarvay.workpaper.others.info
import jarvay.workpaper.others.scaleFixedRatio
import jarvay.workpaper.receiver.WallpaperReceiver
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LiveWallpaperService : WallpaperService() {
    @Inject
    lateinit var workpaper: Workpaper

    @Inject
    lateinit var settingsPreferencesRepository: SettingsPreferencesRepository

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        workpaper.liveWallpaperEngineCreated = true
    }

    override fun onCreateEngine(): Engine {
        return LiveWallpaperEngine()
    }

    inner class LiveWallpaperEngine : Engine() {
        private val paint = Paint()

        private var bitmap: Bitmap? = null

        private var wallpaperScrollable: Boolean = false

        init {
            setTouchEventsEnabled(true)

            MainScope().launch {
                settingsPreferencesRepository.settingsPreferencesFlow.collect {
                    wallpaperScrollable = it.wallpaperScrollable
                }
            }
        }

        private fun canScroll(canvas: Canvas): Boolean {
            Log.d("bitmap", bitmap.toString())
            Log.d("bitmap", bitmap!!.info())
            val bitmapRatio = bitmap!!.width.toFloat() / bitmap!!.height.toFloat()
            return bitmapRatio > 1
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

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            Log.d(javaClass.simpleName, "onCreate")
            MainScope().launch {
                workpaper.currentBitmap.collect {
                    Log.d(javaClass.simpleName, "wallpaper bitmap update")
                    if (surfaceHolder == null || it == null) return@collect
                    bitmap = it.copy(Bitmap.Config.ARGB_8888, true)
                    updateWallpaper(bitmap!!)
                }
            }
        }

        override fun onTouchEvent(event: MotionEvent?) {
            Log.d("onTouchEvent", event.toString())
            super.onTouchEvent(event)
            if (event == null) return
            gestureDetector.onTouchEvent(event)
        }

        override fun onOffsetsChanged(
            xOffset: Float,
            yOffset: Float,
            xOffsetStep: Float,
            yOffsetStep: Float,
            xPixelOffset: Int,
            yPixelOffset: Int
        ) {
            super.onOffsetsChanged(
                xOffset,
                yOffset,
                xOffsetStep,
                yOffsetStep,
                xPixelOffset,
                yPixelOffset
            )
            Log.d("onOffsetsChanged", "onOffsetsChanged")
            Log.d("xOffset", xOffset.toString())
            Log.d("xOffsetStep", xOffsetStep.toString())
            if (bitmap != null && wallpaperScrollable) {
                updateBitmapOffset(xOffset)
            }
        }

        private fun updateBitmapOffset(xOffset: Float) {
            if (!surfaceHolder.surface.isValid) return
            var canvas: Canvas? = null
            try {
                canvas = surfaceHolder.lockCanvas() ?: return
                Log.d("canScroll(canvas)", canScroll(canvas).toString())
                if (!canScroll(canvas)) return

                Log.d("updateBitmapOffset", "updateBitmapOffset")

                val x = ((bitmap!!.width - canvas.width).toFloat() * xOffset).toInt()

                if (x + canvas.width > bitmap!!.width) return
                val currentBitmap = Bitmap.createBitmap(
                    bitmap!!,
                    x,
                    0,
                    canvas.width,
                    canvas.height
                )
                canvas.drawBitmap(currentBitmap, 0f, 0f, null)
                Log.d("updateBitmapOffset-2", "updateBitmapOffset-2")
            } finally {
                surfaceHolder.unlockCanvasAndPost(canvas)
            }
        }

        private fun updateWallpaper(originBitmap: Bitmap) {
            if (!surfaceHolder.surface.isValid) return
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

            canvas = surfaceHolder.lockCanvas()
            bitmap = originBitmap.scaleFixedRatio(
                targetWidth = canvas.width,
                targetHeight = canvas.height,
                useMin = false
            )

            var newBitmap = bitmap

            if (!wallpaperScrollable || !canScroll(canvas)) {
                newBitmap = bitmap!!.centerCrop(
                    targetWidth = canvas.width,
                    targetHeight = canvas.height
                )
            }

            canvas?.let { surfaceHolder.unlockCanvasAndPost(canvas) }

            while (alpha <= 255) {
                canvas = surfaceHolder.lockCanvas()

                paint.alpha = alpha
                canvas.drawBitmap(newBitmap!!, 0F, 0F, paint)

                alpha += 5
                Thread.sleep(10)

                canvas?.let {
                    surfaceHolder.unlockCanvasAndPost(it)
                }
            }
        }
    }
}