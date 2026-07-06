package jarvay.workpaper.service

import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory.Options
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
import jarvay.workpaper.data.preferences.DEFAULT_SETTINGS
import jarvay.workpaper.data.preferences.SettingsPreferences
import jarvay.workpaper.data.wallpaper.WallpaperType
import jarvay.workpaper.others.GestureEvent
import jarvay.workpaper.others.LOG_TAG
import jarvay.workpaper.others.PARALLAX_QUAD_SCALE
import jarvay.workpaper.others.bitmapFromContentUri
import jarvay.workpaper.others.centerCrop
import jarvay.workpaper.others.scaleFixedRatio
import jarvay.workpaper.others.wechatIntent
import jarvay.workpaper.depth.DepthEstimator

import jarvay.workpaper.receiver.WallpaperReceiver
import jarvay.workpaper.wallpaper.ParallaxSensorManager
import jarvay.workpaper.wallpaper.WallpaperRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
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

    private var cachedDepthUri: String? = null
    private var cachedDepthMap: FloatArray? = null
    private var cachedDepthImage: Bitmap? = null


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
        private var settings: SettingsPreferences = DEFAULT_SETTINGS

        private var currentBitmap: Bitmap? = null
        private var nextBitmap: Bitmap? = null
        private var parallaxSensorManager: ParallaxSensorManager? = null
        private var parallaxFrameRateJob: Job? = null
        private val depthEstimator: DepthEstimator = DepthEstimator.create(this@LiveWallpaperService)

        init {
            setTouchEventsEnabled(true)

            MainScope().launch {
                workpaper.settingsPreferencesRepository.settingsPreferencesFlow.collect {
                    settings = it
                    updateParallaxState()
                }
            }

            MainScope().launch {
                workpaper.currentRuleWithRelation.collect {
                    updateParallaxState()
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
                        LockAccessibilityService.lockScreen()
                    }

                    GestureEvent.OPEN_WECHAT -> {
                        startActivity(wechatIntent())
                    }

                    GestureEvent.OPEN_WECHAT_SCAN -> {
                        startActivity(wechatIntent(toScan = true))
                    }

                    GestureEvent.OPEN_ALIPAY_SCAN -> {
                        val uri = "alipayqr://platformapi/startapp?saId=10000007".toUri()
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
                workpaper.imageUri.distinctUntilChanged { old, new -> old == new }.collect {
                    if (it == null) return@collect
                    LogUtils.i(LOG_TAG, "On image uri", it.toString())

                    withContext(Dispatchers.IO) {
                        setImageBitmap(it.toUri())
                    }
                }
            }
            MainScope().launch {
                workpaper.videoUri.distinctUntilChanged { old, new -> old == new }.collect {
                    if (it == null) return@collect
                    LogUtils.i(LOG_TAG, "On video uri", it.toString())

                    changeVideoSource(it.toUri())
                }
            }

            engineLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        }

        override fun onDestroy() {
            super.onDestroy()
            stopParallaxFrameRateTimer()
            parallaxSensorManager?.stop()
            parallaxSensorManager = null
            renderer?.parallaxSensorManager = null
            depthEstimator.close()
            currentBitmap?.recycle()
            nextBitmap?.recycle()
            currentBitmap = null
            nextBitmap = null

            engineLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            renderer?.destroy()
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
            if (visible) {
                renderer?.triggerWakeAnimation()
            }
            updateParallaxState()
        }

        private fun isParallaxEnabled(): Boolean {
            val rule = workpaper.currentRuleWithRelation.value ?: return false
            return rule.rule.enableParallaxEffect
                    && renderer?.wallpaperType == WallpaperType.IMAGE
        }

        private fun updateParallaxState() {
            val enabled = isParallaxEnabled() && isVisible
            val frameRate = if (enabled) settings.parallaxFrameRate else 0

            if (enabled) {
                if (parallaxSensorManager == null) {
                    parallaxSensorManager = ParallaxSensorManager(this@LiveWallpaperService)
                    renderer?.parallaxSensorManager = parallaxSensorManager
                }
                parallaxSensorManager?.sensitivity = settings.parallaxSensitivity
                parallaxSensorManager?.invertDirection = settings.parallaxInvertDirection
                parallaxSensorManager?.start()
                renderer?.imageRenderer?.setParallaxEnabled(true)
                renderer?.depthLayerRenderer?.setParallaxEnabled(true)
                renderer?.setDepthStrength(settings.depthStrength)
                renderer?.updateWallpaperType(WallpaperType.IMAGE, frameRate)
                if (frameRate < 60) {
                    startParallaxFrameRateTimer()
                } else {
                    stopParallaxFrameRateTimer()
                }
            } else {
                parallaxSensorManager?.stop()
                stopParallaxFrameRateTimer()
                renderer?.imageRenderer?.setParallaxEnabled(false)
                if (renderer?.wallpaperType == WallpaperType.IMAGE) {
                    renderer?.updateWallpaperType(WallpaperType.IMAGE)
                }
            }
        }

        private fun startParallaxFrameRateTimer() {
            stopParallaxFrameRateTimer()
            val intervalMs = 1000L / settings.parallaxFrameRate.coerceAtLeast(1)
            parallaxFrameRateJob = lifecycleScope.launch {
                while (isActive) {
                    surfaceView?.requestRender()
                    delay(intervalMs)
                }
            }
        }

        private fun stopParallaxFrameRateTimer() {
            parallaxFrameRateJob?.cancel()
            parallaxFrameRateJob = null
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
            if (renderer?.wallpaperType != WallpaperType.IMAGE ||
                !settings.wallpaperScrollable
            ) return

            renderer?.imageRenderer?.updateOffset(xOffset)
            surfaceView?.requestRender()
        }

        private suspend fun setImageBitmap(uri: Uri) {
            if (!surfaceHolder.surface.isValid) return

            val parallax = isParallaxEnabled()
            if (uri.toString() != prevImageUri) {
                renderer?.useDepthLayers = false
            }
            renderer?.imageRenderer?.setParallaxEnabled(parallax)
            renderer?.depthLayerRenderer?.setParallaxEnabled(parallax)
            val frameRate = if (parallax) settings.parallaxFrameRate else 0
            renderer?.updateWallpaperType(WallpaperType.IMAGE, frameRate)
            stopVideo()

            var newBitmap = loadBitmap(uri) ?: return
            newBitmap = workpaper.handleBitmapStyle(newBitmap)

            currentBitmap?.recycle()
            currentBitmap = newBitmap
            renderer?.imageRenderer?.setBitmap(newBitmap, parallax = parallax)
            surfaceView?.requestRender()
            prevImageUri = uri.toString()

            if (parallax && settings.enableDepthLayers) {
                Log.d(LOG_TAG, "Depth layers enabled, parallax=$parallax, enableDepthLayers=${settings.enableDepthLayers}")
                try {
                    val cachedUri = this@LiveWallpaperService.cachedDepthUri
                    val cachedMap = this@LiveWallpaperService.cachedDepthMap
                    val cachedImg = this@LiveWallpaperService.cachedDepthImage
                    if (cachedUri == uri.toString() && cachedMap != null && cachedImg != null) {
                        Log.d(LOG_TAG, "Using cached depth data")
                        renderer?.depthLayerRenderer?.setDepthData(cachedImg.copy(cachedImg.config, false), cachedMap)
                        renderer?.useDepthLayers = true
                        surfaceView?.requestRender()
                    } else {
                        Log.d(LOG_TAG, "Depth processing starting...")
                        if (depthEstimator.isInitialized().not()) {
                            withContext(Dispatchers.IO) {
                                depthEstimator.initialize()
                            }
                        }
                        if (depthEstimator.isInitialized()) {
                            val bitmapForDepth = newBitmap.copy(newBitmap.config, false)
                            val depthMap = withContext(Dispatchers.IO) {
                                depthEstimator.estimateDepth(bitmapForDepth)
                            }
                            Log.d(LOG_TAG, "Depth map size: ${depthMap.size}")
                            if (depthMap.isNotEmpty()) {
                                this@LiveWallpaperService.cachedDepthUri = uri.toString()
                                this@LiveWallpaperService.cachedDepthMap = depthMap
                                this@LiveWallpaperService.cachedDepthImage = bitmapForDepth
                                renderer?.depthLayerRenderer?.setDepthData(bitmapForDepth, depthMap)
                                renderer?.useDepthLayers = true
                                surfaceView?.requestRender()
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(LOG_TAG, "Depth processing failed", e)
                }
            } else {
                Log.d(LOG_TAG, "Depth layers NOT enabled: parallax=$parallax, enableDepthLayers=${settings.enableDepthLayers}")
            }
        }

        private fun loadBitmap(uri: Uri?, options: Options = Options()): Bitmap? {
            if (uri == null) return null

            val originBitmap = bitmapFromContentUri(uri, this@LiveWallpaperService, options)
                ?: return null

            val parallax = isParallaxEnabled()

            return if (surfaceSize.width > 0 && surfaceSize.height > 0) {
                val scale = if (parallax) PARALLAX_QUAD_SCALE else 1.0f
                val targetW = (surfaceSize.width * scale).toInt()
                val targetH = (surfaceSize.height * scale).toInt()

                originBitmap.scaleFixedRatio(
                    targetWidth = targetW,
                    targetHeight = targetH,
                    useMin = false
                ).let {
                    if (!settings.wallpaperScrollable && !parallax) {
                        it.centerCrop(
                            targetWidth = surfaceSize.width,
                            targetHeight = surfaceSize.height
                        )
                    } else {
                        it
                    }
                }.also {
                    if (it != originBitmap) {
                        originBitmap.recycle()
                    }
                }
            } else {
                originBitmap
            }
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

        private fun changeVideoSource(uri: Uri) {
            LogUtils.i(
                LOG_TAG,
                "fun changeVideoSource",
                "isVisible=$isVisible, isScreenOn=$isScreenOn"
            )

            if (renderer == null) return

            renderer!!.videoRenderer.setSourcePlayer(player)
            renderer!!.updateWallpaperType(WallpaperType.VIDEO)
            parallaxSensorManager?.stop()

            updateVideoInfo(uri)

            player.apply {
                reset()
                setVolume(0f, 0f)
                isLooping = true
                setDataSource(this@LiveWallpaperService, uri)
                setOnPreparedListener {
                    if (isVisible && isScreenOn) {
                        it.start()
                        LogUtils.i(
                            LOG_TAG,
                            "isVisible=$isVisible, isScreenOn=$isScreenOn",
                            "player started"
                        )
                    } else {
                        LogUtils.i(
                            LOG_TAG,
                            "isVisible=$isVisible, isScreenOn=$isScreenOn",
                            "player not start"
                        )
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
                    LogUtils.i(LOG_TAG, "player stopped")
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
