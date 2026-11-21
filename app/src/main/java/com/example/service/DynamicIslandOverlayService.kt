package com.example.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.data.DynamicIslandRepository
import com.example.data.FocusSettingsRepository
import com.example.ui.components.DynamicIslandOverlayHost
import com.example.ui.theme.MyApplicationTheme

/**
 * Lightweight system overlay service that floats the Nothing dot-matrix Dynamic Island
 * at the top of the screen over any app (for post-unlock Notification Summary, Upcoming
 * Schedule alerts with Skip Today, and Goal/Streak Milestones).
 */
class DynamicIslandOverlayService : Service(), LifecycleOwner, SavedStateRegistryOwner {

    companion object {
        @Volatile
        var isOverlayVisible: Boolean = false

        fun show(context: Context) {
            val appCtx = context.applicationContext
            if (!runCatching { Settings.canDrawOverlays(appCtx) }.getOrDefault(false)) return
            runCatching {
                appCtx.startService(Intent(appCtx, DynamicIslandOverlayService::class.java))
            }
        }

        fun stop(context: Context) {
            val appCtx = context.applicationContext
            runCatching {
                appCtx.stopService(Intent(appCtx, DynamicIslandOverlayService::class.java))
            }
        }
    }

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private var windowManager: WindowManager? = null
    private var composeView: ComposeView? = null

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        windowManager = getSystemService(WINDOW_SERVICE) as? WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!runCatching { Settings.canDrawOverlays(this) }.getOrDefault(false)) {
            stopSelf()
            return START_NOT_STICKY
        }
        val activeEvent = DynamicIslandRepository.observe(this).value.activeEvent
        if (activeEvent == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (lifecycleRegistry.currentState == Lifecycle.State.CREATED) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }
        ensureOverlayAttached()
        return START_NOT_STICKY
    }

    private fun ensureOverlayAttached() {
        if (composeView != null) return
        val wm = windowManager ?: return

        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@DynamicIslandOverlayService)
            setViewTreeSavedStateRegistryOwner(this@DynamicIslandOverlayService)
            setContent {
                val settings by FocusSettingsRepository.observe(this@DynamicIslandOverlayService).collectAsState()
                val islandState by DynamicIslandRepository.observe(this@DynamicIslandOverlayService).collectAsState()

                LaunchedEffect(islandState.activeEvent) {
                    if (islandState.activeEvent == null) {
                        stopSelf()
                    }
                }

                MyApplicationTheme(appTheme = settings.appTheme) {
                    DynamicIslandOverlayHost(
                        state = islandState,
                        isSystemOverlay = true
                    )
                }
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        runCatching {
            wm.addView(view, params)
            composeView = view
            isOverlayVisible = true
        }.onFailure {
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isOverlayVisible = false
        composeView?.let { v ->
            runCatching { windowManager?.removeView(v) }
        }
        composeView = null
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
