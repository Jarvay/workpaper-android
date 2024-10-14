package jarvay.workpaper.service

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.hardware.display.DisplayManager
import android.os.Build
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.Surface
import android.view.SurfaceHolder
import dagger.hilt.android.AndroidEntryPoint
import jarvay.workpaper.Workpaper
import jarvay.workpaper.others.centerCrop
import jarvay.workpaper.others.scaleFixedRatio
import jarvay.workpaper.receiver.WallpaperReceiver
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LiveWallpaperService : WallpaperService() {
    @Inject
    lateinit var workpaper: Workpaper

    override fun onCreateEngine(): Engine {
        return LiveWallpaperEngine()
    }

    inner class LiveWallpaperEngine : Engine() {
        private val paint = Paint()

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

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            Log.d(javaClass.simpleName, "onCreate")
            MainScope().launch {
                workpaper.currentBitmap.collect {
                    Log.d(javaClass.simpleName, "wallpaper bitmap update")
                    if (surfaceHolder == null || it == null) return@collect
                    updateBitmap()
                }
            }
        }

        override fun onTouchEvent(event: MotionEvent?) {
            Log.d("onTouchEvent", event.toString())
            super.onTouchEvent(event)
            if (event == null) return
            gestureDetector.onTouchEvent(event)
        }

        private fun updateBitmap() {
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
            val bitmap = workpaper.currentBitmap.value!!.scaleFixedRatio(
                targetWidth = canvas.width,
                targetHeight = canvas.height,
                useMin = false
            ).centerCrop(
                targetWidth = canvas.width,
                targetHeight = canvas.height
            )
            workpaper.currentBitmap.value!!.recycle()
            workpaper.currentBitmap.value = null
            canvas?.let { surfaceHolder.unlockCanvasAndPost(canvas) }

            while (alpha <= 255) {
                canvas = surfaceHolder.lockCanvas()

                paint.alpha = alpha
                canvas.drawBitmap(bitmap, 0F, 0F, paint)

                alpha += 5
                Thread.sleep(10)

                canvas?.let {
                    surfaceHolder.unlockCanvasAndPost(it)
                }
            }
        }
    }
}