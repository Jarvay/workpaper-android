package jarvay.workpaper.service

import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory.Options
import android.graphics.Canvas
import android.graphics.Paint
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.opengl.GLSurfaceView
import android.service.wallpaper.WallpaperService
import android.util.Size
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.SurfaceHolder
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import com.blankj.utilcode.util.LogUtils
import dagger.hilt.android.AndroidEntryPoint
import jarvay.workpaper.Workpaper
import jarvay.workpaper.data.preferences.SettingsPreferences
import jarvay.workpaper.data.wallpaper.WallpaperType
import jarvay.workpaper.others.GestureEvent
import jarvay.workpaper.others.bitmapFromContentUri
import jarvay.workpaper.others.centerCrop
import jarvay.workpaper.others.scaleFixedRatio
import jarvay.workpaper.others.wechatIntent
import jarvay.workpaper.receiver.WallpaperReceiver
import jarvay.workpaper.wallpaper.WallpaperRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject


@AndroidEntryPoint
class LiveWallpaperService : WallpaperService(), LifecycleOwner {
    @Inject
    lateinit var workpaper: Workpaper
    private var prevImageUri: String? = null
    private var surfaceSize = Size(0, 0)
    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle
        get() = lifecycleRegistry


    override fun onCreate() {
        super.onCreate()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    @OptIn(UnstableApi::class)
    override fun onCreateEngine(): Engine {
        return LiveWallpaperEngine()
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }

    @UnstableApi
    inner class LiveWallpaperEngine : Engine(), LifecycleOwner {
        override val lifecycle: Lifecycle
            get() = engineLifecycleRegistry
        private val engineLifecycleRegistry = LifecycleRegistry(this)

        private var surfaceView: GLWallpaperSurfaceView? = null
        private var renderer: WallpaperRenderer? = null
        private val player: MediaPlayer = MediaPlayer()
        private var resetOnScreenOff = false
        private var doubleTapEvent: GestureEvent = GestureEvent.NONE
        private var isScreenOn = true
        private var bitmap: Bitmap? = null
        private var settings: SettingsPreferences? = null

        init {
            setTouchEventsEnabled(true)

            MainScope().launch {
                workpaper.settingsPreferencesRepository.settingsPreferencesFlow.collect {
                    settings = it
                }
            }
        }

        private val screenStateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent == null || context == null) return

                if (intent.action == Intent.ACTION_SCREEN_ON) {
                    isScreenOn = true
                }
                if (intent.action == Intent.ACTION_SCREEN_OFF) {
                    isScreenOn = false
                    if (player.isPlaying) {
                        player.pause()
                    }
                    if (resetOnScreenOff) {
                        player.seekTo(0);
                    }
                }
            }
        }

        private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                val result = super.onDoubleTap(e)

                when (doubleTapEvent) {
                    GestureEvent.NONE -> {

                    }

                    GestureEvent.CHANGE_WALLPAPER -> {
                        val intent =
                            Intent(this@LiveWallpaperService, WallpaperReceiver::class.java)
                        sendBroadcast(intent)
                    }

                    GestureEvent.LOCK_SCREEN -> {
                        val devicePolicyManager =
                            getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
                        devicePolicyManager.lockNow()
                    }

                    GestureEvent.OPEN_WECHAT -> {
                        startActivity(wechatIntent())
                    }

                    GestureEvent.OPEN_WECHAT_SCAN -> {
                        startActivity(wechatIntent(toScan = true))
                    }

                    GestureEvent.OPEN_ALIPAY_SCAN -> {
                        val uri = Uri.parse("alipayqr://platformapi/startapp?saId=10000007")
                        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(intent)
                    }
                }

                return result
            }
        }
        private val gestureDetector = GestureDetector(this@LiveWallpaperService, gestureListener)

        init {
            player.apply {
                setVolume(0f, 0f)
                isLooping = true
            }
            lifecycleScope.launch {
                workpaper.settingsPreferencesRepository.settingsPreferencesFlow.collect {
                    resetOnScreenOff = it.videoResetProgressOnScreenOff
                    doubleTapEvent = try {
                        GestureEvent.valueOf(it.doubleTapEvent)
                    } catch (e: Exception) {
                        GestureEvent.NONE
                    }
                }
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)

            val intentFilter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            }
            ContextCompat.registerReceiver(
                this@LiveWallpaperService,
                screenStateReceiver,
                intentFilter,
                ContextCompat.RECEIVER_EXPORTED
            )

            MainScope().launch {
                workpaper.imageUri.collect {
                    if (it == null) return@collect
                    LogUtils.i(
                        this@LiveWallpaperEngine.javaClass.simpleName, "On image uri", it.toString()
                    )

                    withContext(Dispatchers.IO) {
                        setImageBitmap(it.toUri())
                    }
                }
            }
            MainScope().launch {
                workpaper.videoUri.collect {
                    if (it == null) return@collect
                    LogUtils.i(
                        this@LiveWallpaperEngine.javaClass.simpleName, "On video uri", it.toString()
                    )

                    startVideo(it.toUri())
                }
            }

            engineLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        }

        override fun onDestroy() {
            super.onDestroy()
            engineLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

            unregisterReceiver(screenStateReceiver)
        }

        override fun onTouchEvent(event: MotionEvent?) {
            super.onTouchEvent(event)
            if (event == null) return
            gestureDetector.onTouchEvent(event)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            if (renderer == null) return

            when (renderer!!.wallpaperType) {
                WallpaperType.IMAGE -> {
                    onImageVisibleChanged(visible)
                }

                WallpaperType.VIDEO -> {
                    onVideoVisibleChanged(visible)
                }
            }
        }

        private fun onImageVisibleChanged(visible: Boolean) {
            if (renderer == null) return
        }

        private fun onVideoVisibleChanged(visible: Boolean) {
            if (visible) {
                player.start()
            } else {
                player.pause()
            }
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            initSurfaceView()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder?) {
            super.onSurfaceDestroyed(holder)
            stopVideo()
            destroySurfaceView()
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
                xOffset, yOffset, xOffsetStep, yOffsetStep, xPixelOffset, yPixelOffset
            )
            if (renderer?.wallpaperType !== WallpaperType.IMAGE) return

            if (bitmap != null && settings?.wallpaperScrollable == true) {
                updateBitmapOffset(xOffset)
            }
        }

        private fun canScroll(): Boolean {
            if (bitmap == null) return false

            val bitmapRatio = bitmap!!.width.toFloat() / bitmap!!.height.toFloat()
            return bitmapRatio > 1
        }

        private fun updateBitmapOffset(xOffset: Float) {
            if (!canScroll()) {
                return
            }

            MainScope().launch(Dispatchers.IO) {
                val x = ((bitmap!!.width - surfaceSize.width).toFloat() * xOffset).toInt()

                if (x + surfaceSize.width > bitmap!!.width) {
                    return@launch
                }

                val newBitmap = Bitmap.createBitmap(
                    bitmap!!, x, 0, surfaceSize.width, surfaceSize.height
                )

                renderer?.imageRenderer?.bitmap = newBitmap
                surfaceView?.requestRender()
            }
        }

        private suspend fun setImageBitmap(uri: Uri) {
            if (!surfaceHolder.surface.isValid) return

            renderer?.updateWallpaperType(WallpaperType.IMAGE)
            stopVideo()

            val bitmapMap = mutableMapOf<Int, Bitmap>()

            val originBitmap = loadBitmap(uri) ?: return

            if (settings?.imageTransition == false) {
                bitmapMap[0] = originBitmap
                changeBitmap(bitmapMap)
                return
            }

            val lowSampleOptions = Options().apply { inSampleSize = 1024 }
            val lowSampleOriginBitmap = loadBitmap(uri, lowSampleOptions) ?: return
            
            val prevBitmap = loadBitmap(prevImageUri?.toUri(), lowSampleOptions)
            prevImageUri = uri.toString()

            if (prevBitmap == null) {
                bitmapMap[0] = originBitmap
                changeBitmap(bitmapMap)
                return
            }

            var alpha = 55
            val alphaList = mutableListOf<Int>()

            while (alpha <= 255) {
                alphaList.add(alpha)
                alpha += 10
            }
            bitmapMap[alphaList.size] = originBitmap

            for ((index, item) in alphaList.withIndex()) {
                MainScope().launch(Dispatchers.IO) {
                    bitmapMap[index] = getBitmapWithAlpha(prevBitmap, lowSampleOriginBitmap, item)
                    if (bitmapMap.size - 1 == alphaList.size) {
                        changeBitmap(bitmapMap)
                        prevBitmap.recycle()
                    }
                }
            }
        }

        private suspend fun changeBitmap(bitmaps: Map<Int, Bitmap>) {
            for (index in 0 until bitmaps.size) {
                try {
                    renderer?.imageRenderer?.bitmap = bitmaps[index]
                    surfaceView?.requestRender()
                    delay(15)
                    if (index < bitmaps.size - 1) {
                        bitmaps[index - 1]?.recycle()
                    }
                } catch (e: Exception) {
                    LogUtils.e(e.toString())
                }
            }
        }

        private fun getBitmapWithAlpha(
            backgroundBitmap: Bitmap?, frontBitmap: Bitmap, alpha: Int
        ): Bitmap {
            val result = Bitmap.createBitmap(
                frontBitmap.width,
                frontBitmap.height,
                Bitmap.Config.RGB_565
            )

            val canvas = Canvas(result)
            val paint = Paint()
            if (backgroundBitmap != null) {
                paint.alpha = 255 - alpha
                canvas.drawBitmap(backgroundBitmap, 0f, 0f, paint)
            }
            paint.alpha = alpha
            canvas.drawBitmap(frontBitmap, 0f, 0f, paint)

            return result
        }

        private suspend fun loadBitmap(uri: Uri?, options: Options = Options()): Bitmap? {
            if (uri == null) return null

            val originBitmap =
                bitmapFromContentUri(uri, this@LiveWallpaperService, options) ?: return null
            bitmap = originBitmap

            var bitmap = originBitmap.scaleFixedRatio(
                targetWidth = surfaceSize.width, targetHeight = surfaceSize.height, useMin = false
            ).centerCrop(
                targetWidth = surfaceSize.width, targetHeight = surfaceSize.height
            )
            bitmap = workpaper.handleBitmapStyle(bitmap)

            return bitmap
        }

        private fun initSurfaceView() {
            if (surfaceView != null) return

            surfaceView = GLWallpaperSurfaceView(this@LiveWallpaperService)

            renderer = renderer ?: WallpaperRenderer(surfaceView!!, lifecycleScope)

            if (surfaceSize.width == 0) {
                lifecycleScope.launch {
                    renderer!!.surfaceSize.collect {
                        if (it.width > 0) {
                            surfaceSize = Size(it.width, it.height)
                        }
                    }
                }
            }

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

            LogUtils.i(
                this@LiveWallpaperEngine.javaClass.simpleName,
                "fun startVideo",
                "isVisible=$isVisible, isScreenOn=$isScreenOn"
            )

            renderer!!.videoRenderer.setSourcePlayer(player)
            renderer!!.updateWallpaperType(WallpaperType.VIDEO)

            updateVideoInfo(uri)

            player.apply {
                reset()
                setVolume(0f, 0f)
                isLooping = true
                setDataSource(this@LiveWallpaperService, uri)
                if (isVisible && isScreenOn) {
                    setOnPreparedListener {
                        it.start()
                        LogUtils.i(javaClass.simpleName, "player started")
                    }
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
                width = width, height = height, rotation = rotation
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
}