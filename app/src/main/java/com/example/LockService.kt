package com.example

import android.animation.ValueAnimator
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.Display
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.hypot
import kotlin.math.roundToInt
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.data.AllowedApp
import com.example.data.FocusScheduleRepository
import com.example.data.FocusSettingsRepository
import com.example.data.FocusStatsRepository
import com.example.data.FocusXpRepository
import com.example.data.LockAppLayout
import com.example.data.NotificationBlockerRepository
import com.example.data.ShareRepository
import com.example.service.ScheduleAlarmScheduler
import com.example.ui.components.FocusLockEmblem
import com.example.ui.components.LockAllowedAppsSection
import com.example.ui.components.LockPalette
import com.example.ui.components.ScheduledLockScreen
import com.example.ui.components.ThemeBackdropLayer
import com.example.ui.components.ThemedTimerRing
import com.example.ui.components.focusGlassPanel
import com.example.ui.components.performFocusGestureEnd
import com.example.ui.components.performFocusGestureStart
import com.example.ui.components.rememberFocusHaptics
import com.example.ui.components.rememberSpringPressScale
import com.example.ui.theme.AppVisualTheme
import com.example.ui.theme.FocusThemePalette
import com.example.ui.theme.LocalFocusTheme
import com.example.ui.theme.MyApplicationTheme
import com.example.widget.NothingFocusWidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

class LockService : Service(), LifecycleOwner, SavedStateRegistryOwner {

    companion object {
        private const val OVERLAY_PREFS = "focus_lock_overlay_prefs"
        private const val KEY_BUBBLE_X = "floating_bubble_x"
        private const val KEY_BUBBLE_Y = "floating_bubble_y"
        private const val KEY_ACTIVE_SESSION_START = "active_session_start_ms"
        private const val KEY_ACTIVE_SESSION_PIN = "active_session_pin"
        private const val KEY_ACTIVE_IS_SCHEDULED = "active_is_scheduled"
        private const val KEY_ACTIVE_SCHEDULE_ID = "active_schedule_id"
        private const val KEY_ACTIVE_SCHEDULE_NAME = "active_schedule_name"
        private const val KEY_ACTIVE_SCHEDULE_RANGE = "active_schedule_range"
        private const val KEY_ACTIVE_SCHEDULE_END_MS = "active_schedule_end_ms"

        const val EXTRA_IS_SCHEDULED = "extra_is_scheduled"
        const val EXTRA_SCHEDULE_ID = "extra_schedule_id"
        const val EXTRA_SCHEDULE_NAME = "extra_schedule_name"
        const val EXTRA_SCHEDULE_TIME_RANGE = "extra_schedule_time_range"
        const val EXTRA_SCHEDULE_END_TIME_MS = "extra_schedule_end_time_ms"

        @Volatile
        var currentSessionStartTimeMs: Long = 0L

        @Volatile
        var isRunning: Boolean = false

        @Volatile
        var isScheduledSession: Boolean = false

        fun clampBubblePosition(
            x: Int,
            y: Int,
            viewWidth: Int,
            viewHeight: Int,
            screenWidth: Int,
            screenHeight: Int,
            marginPx: Int
        ): Pair<Int, Int> {
            val safeW = viewWidth.coerceAtLeast(1)
            val safeH = viewHeight.coerceAtLeast(1)
            val maxX = (screenWidth - safeW - marginPx).coerceAtLeast(marginPx)
            val maxY = (screenHeight - safeH - marginPx).coerceAtLeast(marginPx)
            return x.coerceIn(marginPx, maxX) to y.coerceIn(marginPx, maxY)
        }
    }

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    private var windowManager: WindowManager? = null
    private var overlayContainer: DraggableOverlayContainer? = null
    private var composeView: ComposeView? = null
    private var currentMinimizedParams: WindowManager.LayoutParams? = null
    private var snapAnimator: ValueAnimator? = null
    private var bubbleX: Int = -1
    private var bubbleY: Int = -1
    private var targetPin: String = ""
    private var startTime: Long = 0
    private var activeScheduleId: String? = null
    private var activeScheduleName: String = "Scheduled Focus"
    private var activeScheduleTimeRange: String = ""
    private var activeScheduleEndTimeMs: Long = 0L

    @Volatile
    private var minimizeTimestamp: Long = 0L

    @Volatile
    private var isMinimizedVolatile: Boolean = false

    private val isStoppingLock = AtomicBoolean(false)
    private var isMinimizedState = mutableStateOf(false)
    private var isScheduledUiState = mutableStateOf(false)
    private var monitorJob: Job? = null
    private val serviceJob = SupervisorJob()
    private val scope = CoroutineScope(serviceJob + Dispatchers.IO)

    private fun setMinimized(minimized: Boolean) {
        isMinimizedVolatile = minimized
        isMinimizedState.value = minimized
    }

    private fun persistActiveSession(
        startMs: Long,
        pin: String,
        scheduled: Boolean = isScheduledSession,
        scheduleId: String? = activeScheduleId,
        scheduleName: String = activeScheduleName,
        scheduleRange: String = activeScheduleTimeRange,
        scheduleEndMs: Long = activeScheduleEndTimeMs
    ) {
        getSharedPreferences(OVERLAY_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_ACTIVE_SESSION_START, startMs)
            .putString(KEY_ACTIVE_SESSION_PIN, pin)
            .putBoolean(KEY_ACTIVE_IS_SCHEDULED, scheduled)
            .putString(KEY_ACTIVE_SCHEDULE_ID, scheduleId)
            .putString(KEY_ACTIVE_SCHEDULE_NAME, scheduleName)
            .putString(KEY_ACTIVE_SCHEDULE_RANGE, scheduleRange)
            .putLong(KEY_ACTIVE_SCHEDULE_END_MS, scheduleEndMs)
            .apply()
    }

    private fun clearActiveSession() {
        getSharedPreferences(OVERLAY_PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_ACTIVE_SESSION_START)
            .remove(KEY_ACTIVE_SESSION_PIN)
            .remove(KEY_ACTIVE_IS_SCHEDULED)
            .remove(KEY_ACTIVE_SCHEDULE_ID)
            .remove(KEY_ACTIVE_SCHEDULE_NAME)
            .remove(KEY_ACTIVE_SCHEDULE_RANGE)
            .remove(KEY_ACTIVE_SCHEDULE_END_MS)
            .apply()
    }

    private val transientSystemPackages = setOf(
        "android",
        "com.android.systemui",
        "com.android.intentresolver",
        "com.android.permissioncontroller",
        "com.google.android.permissioncontroller",
        "com.google.android.packageinstaller",
        "com.lbe.security.miui",
        "com.miui.securitycenter",
        "com.miui.securityadd"
    )

    private fun getHomeLauncherPackages(): Set<String> {
        val homePackages = mutableSetOf<String>()
        try {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
            }
            packageManager.queryIntentActivities(homeIntent, 0).forEach { info ->
                info.activityInfo?.packageName?.let { homePackages.add(it) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return homePackages
    }

    private fun getAllowedPackages(): Set<String> {
        val settings = FocusSettingsRepository.getCurrent(this)
        val packages = mutableSetOf<String>()

        for (app in settings.allowedApps) {
            when (app.packageName) {
                FocusSettingsRepository.PRESET_GOOGLE_PKG -> {
                    packages.addAll(FocusSettingsRepository.GOOGLE_PACKAGES)
                }
                FocusSettingsRepository.PRESET_XIAOMI_PKG -> {
                    packages.addAll(FocusSettingsRepository.XIAOMI_PACKAGES)
                }
                FocusSettingsRepository.PRESET_PHONE_PKG -> {
                    packages.addAll(FocusSettingsRepository.PHONE_PACKAGES)
                    try {
                        val telecomManager = getSystemService(Context.TELECOM_SERVICE) as? android.telecom.TelecomManager
                        telecomManager?.defaultDialerPackage?.let { packages.add(it) }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    try {
                        val dialIntent = Intent(Intent.ACTION_DIAL)
                        packageManager.queryIntentActivities(dialIntent, 0).forEach {
                            it.activityInfo?.packageName?.let { pkg -> packages.add(pkg) }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    try {
                        val callIntent = Intent(Intent.ACTION_CALL_BUTTON)
                        packageManager.queryIntentActivities(callIntent, 0).forEach {
                            it.activityInfo?.packageName?.let { pkg -> packages.add(pkg) }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                else -> {
                    packages.add(app.packageName)
                    try {
                        packageManager.getLaunchIntentForPackage(app.packageName)
                            ?.resolveActivity(packageManager)
                            ?.packageName
                            ?.let { packages.add(it) }
                    } catch (_: Exception) { }
                    try {
                        val mainIntent = Intent(Intent.ACTION_MAIN).apply {
                            addCategory(Intent.CATEGORY_LAUNCHER)
                            setPackage(app.packageName)
                        }
                        packageManager.queryIntentActivities(mainIntent, 0).forEach { info ->
                            info.activityInfo?.packageName?.let { packages.add(it) }
                            info.activityInfo?.applicationInfo?.packageName?.let { packages.add(it) }
                        }
                    } catch (_: Exception) { }
                }
            }
        }

        return packages
    }

    private fun startMonitoringForegroundApp() {
        monitorJob?.cancel()
        monitorJob = scope.launch {
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return@launch
            var trackedSessionMinimizeTs = 0L
            var lastQueryTime = 0L
            var sessionAllowedPkg: String? = null
            var lastObservedForegroundPkg: String? = null
            var unauthorizedStreak = 0
            var cachedHomePackages: Set<String> = emptySet()
            var cachedAllowedPackages: Set<String> = emptySet()

            while (isActive) {
                if (isMinimizedVolatile) {
                    val currentMinimizeTs = minimizeTimestamp
                    if (currentMinimizeTs != trackedSessionMinimizeTs) {
                        trackedSessionMinimizeTs = currentMinimizeTs
                        lastQueryTime = currentMinimizeTs
                        sessionAllowedPkg = null
                        lastObservedForegroundPkg = null
                        unauthorizedStreak = 0
                        cachedHomePackages = getHomeLauncherPackages()
                        cachedAllowedPackages = getAllowedPackages()
                    }

                    val settings = FocusSettingsRepository.getCurrent(this@LockService)
                    val graceMs = settings.appSwitchGraceSeconds * 1000L
                    val now = System.currentTimeMillis()
                    val homePackages = cachedHomePackages

                    val queryStart = minOf(lastQueryTime, now)
                    val events = usageStatsManager.queryEvents(queryStart, now)
                    var latestEventTs = queryStart
                    while (events.hasNextEvent()) {
                        val event = UsageEvents.Event()
                        events.getNextEvent(event)
                        if (event.timeStamp > latestEventTs) {
                            latestEventTs = event.timeStamp
                        }
                        if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                            val pkg = event.packageName
                            if (pkg != null && pkg != packageName && pkg !in transientSystemPackages) {
                                if (sessionAllowedPkg == null &&
                                    pkg !in homePackages &&
                                    event.timeStamp <= currentMinimizeTs + graceMs
                                ) {
                                    sessionAllowedPkg = pkg
                                }

                                if (event.timeStamp >= currentMinimizeTs + 800L) {
                                    lastObservedForegroundPkg = pkg
                                }
                            }
                        }
                    }
                    lastQueryTime = maxOf(lastQueryTime, latestEventTs + 1L)

                    if (now > currentMinimizeTs + graceMs) {
                        val fgPkg = lastObservedForegroundPkg
                        val isAllowed = fgPkg == null ||
                            fgPkg in cachedAllowedPackages ||
                            fgPkg == sessionAllowedPkg
                        if (!isAllowed) {
                            unauthorizedStreak++
                            if (unauthorizedStreak >= 2) {
                                unauthorizedStreak = 0
                                launch(Dispatchers.Main) {
                                    setMinimized(false)
                                    updateOverlayState(false)
                                }
                            }
                        } else {
                            unauthorizedStreak = 0
                        }
                    }
                } else {
                    sessionAllowedPkg = null
                    lastObservedForegroundPkg = null
                    unauthorizedStreak = 0
                    cachedAllowedPackages = emptySet()
                }
                delay(400)
            }
        }
    }

    private fun stopMonitoringForegroundApp() {
        monitorJob?.cancel()
        monitorJob = null
    }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        startMonitoringForegroundApp()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == "MAXIMIZE" || action == "MINIMIZE") {
            if (overlayContainer == null) {
                if (!isRunning) {
                    stopSelf()
                }
                return START_NOT_STICKY
            }
            if (action == "MAXIMIZE") {
                setMinimized(false)
                updateOverlayState(false)
            } else {
                val now = System.currentTimeMillis()
                if (!isMinimizedVolatile || now - minimizeTimestamp > 500L) {
                    minimizeTimestamp = now
                    setMinimized(true)
                    updateOverlayState(true)
                }
            }
            return START_STICKY
        }

        val prefs = getSharedPreferences(OVERLAY_PREFS, Context.MODE_PRIVATE)
        val incomingScheduled = intent?.getBooleanExtra(EXTRA_IS_SCHEDULED, false) == true
        val incomingScheduleId = intent?.getStringExtra(EXTRA_SCHEDULE_ID)

        // Conflict Rule: If a manual OTP session is already active, cancel this scheduled lock for today!
        if (incomingScheduled && isRunning && !isScheduledSession) {
            if (!incomingScheduleId.isNullOrBlank()) {
                FocusScheduleRepository.markSkippedForToday(applicationContext, incomingScheduleId)
            }
            return START_STICKY
        }

        if (incomingScheduled) {
            isScheduledSession = true
            isScheduledUiState.value = true
            activeScheduleId = incomingScheduleId
            activeScheduleName = intent?.getStringExtra(EXTRA_SCHEDULE_NAME) ?: "Scheduled Focus"
            activeScheduleTimeRange = intent?.getStringExtra(EXTRA_SCHEDULE_TIME_RANGE) ?: ""
            activeScheduleEndTimeMs = intent?.getLongExtra(EXTRA_SCHEDULE_END_TIME_MS, 0L) ?: 0L
        } else if (intent == null && prefs.getBoolean(KEY_ACTIVE_IS_SCHEDULED, false)) {
            isScheduledSession = true
            isScheduledUiState.value = true
            activeScheduleId = prefs.getString(KEY_ACTIVE_SCHEDULE_ID, null)
            activeScheduleName = prefs.getString(KEY_ACTIVE_SCHEDULE_NAME, null) ?: "Scheduled Focus"
            activeScheduleTimeRange = prefs.getString(KEY_ACTIVE_SCHEDULE_RANGE, null) ?: ""
            activeScheduleEndTimeMs = prefs.getLong(KEY_ACTIVE_SCHEDULE_END_MS, 0L)
        } else if (intent != null && !incomingScheduled) {
            isScheduledSession = false
            isScheduledUiState.value = false
            activeScheduleId = null
        }

        val incomingPin = intent?.getStringExtra("TARGET_PIN") ?: intent?.getStringExtra("PIN")
        if (incomingPin != null) {
            targetPin = incomingPin
        } else if (targetPin.isBlank()) {
            targetPin = prefs.getString(KEY_ACTIVE_SESSION_PIN, null) ?: "000000"
        }

        if (startTime == 0L) {
            val restoredStart = if (intent == null) prefs.getLong(KEY_ACTIVE_SESSION_START, 0L) else 0L
            startTime = if (restoredStart > 0L) restoredStart else System.currentTimeMillis()
            currentSessionStartTimeMs = startTime
            isRunning = true
            isStoppingLock.set(false)
            persistActiveSession(startTime, targetPin)
            if (restoredStart == 0L) {
                NotificationBlockerRepository.startFreshSession(applicationContext)
                NothingFocusWidgetUpdater.scheduleUpdateAll(applicationContext)
                scope.launch {
                    ShareRepository.syncLiveShareOnSessionStart(applicationContext, startTime)
                }
            }
        } else if (incomingPin != null || incomingScheduled) {
            persistActiveSession(startTime, targetPin)
        }

        createNotificationChannel()
        val notification = Notification.Builder(this, "lock_channel")
            .setContentTitle(if (isScheduledSession) "Scheduled Focus Active" else "Focus Lock Active")
            .setContentText(
                if (isScheduledSession) "$activeScheduleName · Unlocks automatically when timer finishes."
                else "Your phone is currently locked for studying."
            )
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                1,
                notification,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                } else {
                    0
                }
            )
        } else {
            startForeground(1, notification)
        }

        if (lifecycleRegistry.currentState == Lifecycle.State.CREATED) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }

        showOverlay()

        return START_STICKY
    }

    private fun applyHighRefreshRate(params: WindowManager.LayoutParams) {
        runCatching {
            val dm = getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
            val display = dm?.getDisplay(Display.DEFAULT_DISPLAY) ?: return
            val currentMode = display.mode
            val bestMode = display.supportedModes
                .filter {
                    it.physicalWidth == currentMode.physicalWidth &&
                        it.physicalHeight == currentMode.physicalHeight
                }
                .maxByOrNull { it.refreshRate }
                ?: display.supportedModes.maxByOrNull { it.refreshRate }
            if (bestMode != null) {
                params.preferredDisplayModeId = bestMode.modeId
                params.preferredRefreshRate = bestMode.refreshRate
            }
        }
    }

    private fun getScreenBounds(): Pair<Int, Int> {
        val wm = windowManager ?: return 1080 to 2400
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = wm.currentWindowMetrics.bounds
            bounds.width().coerceAtLeast(1) to bounds.height().coerceAtLeast(1)
        } else {
            val dm = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay?.getRealMetrics(dm)
            (dm.widthPixels.takeIf { it > 0 } ?: 1080) to (dm.heightPixels.takeIf { it > 0 } ?: 2400)
        }
    }

    private fun loadOrInitBubblePosition(): Pair<Int, Int> {
        val (screenW, screenH) = getScreenBounds()
        val density = resources.displayMetrics.density
        val marginPx = (12 * density).roundToInt()
        val estimatedW = (180 * density).roundToInt()
        val estimatedH = (56 * density).roundToInt()

        if (bubbleX < 0 || bubbleY < 0) {
            val prefs = getSharedPreferences(OVERLAY_PREFS, Context.MODE_PRIVATE)
            bubbleX = prefs.getInt(KEY_BUBBLE_X, -1)
            bubbleY = prefs.getInt(KEY_BUBBLE_Y, -1)
        }
        if (bubbleX < 0 || bubbleY < 0) {
            bubbleX = (screenW - estimatedW - marginPx).coerceAtLeast(marginPx)
            bubbleY = (56 * density).roundToInt()
        }
        val clamped = clampBubblePosition(
            x = bubbleX,
            y = bubbleY,
            viewWidth = (overlayContainer?.width?.takeIf { it > 0 } ?: estimatedW),
            viewHeight = (overlayContainer?.height?.takeIf { it > 0 } ?: estimatedH),
            screenWidth = screenW,
            screenHeight = screenH,
            marginPx = marginPx
        )
        bubbleX = clamped.first
        bubbleY = clamped.second
        return clamped
    }

    private fun saveBubblePosition(x: Int, y: Int) {
        getSharedPreferences(OVERLAY_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_BUBBLE_X, x)
            .putInt(KEY_BUBBLE_Y, y)
            .apply()
    }

    private fun updateOverlayState(minimized: Boolean) {
        val container = overlayContainer ?: return
        val cView = composeView ?: return
        val wm = windowManager ?: return

        val layoutParams = if (minimized) {
            val (initX, initY) = loadOrInitBubblePosition()
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = initX
                y = initY
            }.also { currentMinimizedParams = it }
        } else {
            currentMinimizedParams = null
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_FULLSCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
                applyHighRefreshRate(this)
            }
        }

        cView.layoutParams = FrameLayout.LayoutParams(
            if (minimized) FrameLayout.LayoutParams.WRAP_CONTENT else FrameLayout.LayoutParams.MATCH_PARENT,
            if (minimized) FrameLayout.LayoutParams.WRAP_CONTENT else FrameLayout.LayoutParams.MATCH_PARENT
        )

        @Suppress("DEPRECATION")
        if (minimized) {
            container.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        } else {
            container.systemUiVisibility = (
                android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                    or android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                )
        }

        wm.updateViewLayout(container, layoutParams)
    }

    private fun launchAllowedApp(app: AllowedApp) {
        minimizeTimestamp = System.currentTimeMillis()
        setMinimized(true)
        updateOverlayState(true)

        val intent = when (app.packageName) {
            FocusSettingsRepository.PRESET_GOOGLE_PKG -> Intent(this, LensLauncherActivity::class.java)
            FocusSettingsRepository.PRESET_PHONE_PKG -> Intent(this, PhoneLauncherActivity::class.java)
            FocusSettingsRepository.PRESET_XIAOMI_PKG -> Intent(this, XiaomiLauncherActivity::class.java)
            else -> Intent(this, AppLauncherActivity::class.java).apply {
                putExtra(AppLauncherActivity.EXTRA_PACKAGE_NAME, app.packageName)
                putExtra(AppLauncherActivity.EXTRA_APP_LABEL, app.label)
            }
        }.apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
        }
        startActivity(intent)
    }

    private fun showOverlay() {
        if (overlayContainer != null) return

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val cView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@LockService)
            setViewTreeSavedStateRegistryOwner(this@LockService)

            setContent {
                val settingsState by FocusSettingsRepository.observe(this@LockService).collectAsState()
                val scheduleState by FocusScheduleRepository.observe(this@LockService).collectAsState()
                val xpState by FocusXpRepository.observe(this@LockService).collectAsState()
                val iconCache by FocusSettingsRepository.appIconsState.collectAsState()
                MyApplicationTheme(appTheme = settingsState.appTheme) {
                    val minimized by isMinimizedState
                    val scheduledMode by isScheduledUiState

                    if (minimized) {
                        FloatingBubble(
                            onResumeLock = {
                                setMinimized(false)
                                updateOverlayState(false)
                            }
                        )
                    } else if (scheduledMode) {
                        val goalMs = settingsState.dailyGoalMinutes.coerceAtLeast(1L) * 60_000L
                        val dailyPct = ((xpState.todayFocusMs.toFloat() / goalMs.toFloat()) * 100f).toInt().coerceIn(0, 999)
                        LaunchedEffect(settingsState.allowedApps) {
                            FocusSettingsRepository.preloadAppIcons(this@LockService, settingsState.allowedApps)
                        }
                        ScheduledLockScreen(
                            scheduleName = activeScheduleName,
                            timeRangeLabel = activeScheduleTimeRange,
                            startTimeMs = startTime,
                            endTimeMs = activeScheduleEndTimeMs,
                            layoutConfig = scheduleState.layoutConfig,
                            dailyGoalPercent = dailyPct,
                            currentStreak = xpState.currentStreak,
                            allowedApps = settingsState.allowedApps,
                            appLayout = settingsState.lockAppLayout,
                            pillButtonSizeDp = settingsState.minimalPillButtonSizeDp,
                            iconCache = iconCache,
                            isPreview = false,
                            onLaunchApp = { app -> launchAllowedApp(app) },
                            onTimerFinished = {
                                activeScheduleId?.let { id ->
                                    FocusScheduleRepository.markOneTimeCompleted(applicationContext, id)
                                }
                                stopLock()
                            }
                        )
                    } else {
                        LockScreen(
                            targetPin = targetPin,
                            onLaunchGoogleLens = {
                                launchAllowedApp(
                                    AllowedApp("preset_google", "Google", FocusSettingsRepository.PRESET_GOOGLE_PKG, true)
                                )
                            },
                            onLaunchPhone = {
                                launchAllowedApp(
                                    AllowedApp("preset_phone", "Phone", FocusSettingsRepository.PRESET_PHONE_PKG, true)
                                )
                            },
                            onLaunchXiaomiHome = {
                                launchAllowedApp(
                                    AllowedApp("preset_xiaomi", "Xiaomi Home", FocusSettingsRepository.PRESET_XIAOMI_PKG, true)
                                )
                            },
                            onUnlock = { stopLock() },
                            allowedApps = settingsState.allowedApps,
                            layout = settingsState.lockAppLayout,
                            minimalPillButtonSizeDp = settingsState.minimalPillButtonSizeDp,
                            showSessionTimer = settingsState.showSessionTimer,
                            hapticKeypad = settingsState.hapticKeypad,
                            autoClearWrongPin = settingsState.autoClearWrongPin,
                            sessionStartTimeMs = startTime,
                            onLaunchApp = { app -> launchAllowedApp(app) }
                        )
                    }
                }
            }
        }
        composeView = cView

        val container = DraggableOverlayContainer(this).apply {
            setViewTreeLifecycleOwner(this@LockService)
            setViewTreeSavedStateRegistryOwner(this@LockService)
            addView(
                cView,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )
            isFocusableInTouchMode = true
            setOnKeyListener { _, keyCode, _ ->
                keyCode == KeyEvent.KEYCODE_BACK
            }
        }
        overlayContainer = container

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_FULLSCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            applyHighRefreshRate(this)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            cView.requestedFrameRate = 120f
        }
        cView.isFocusableInTouchMode = true
        cView.setOnKeyListener { _, keyCode, _ ->
            keyCode == KeyEvent.KEYCODE_BACK
        }

        @Suppress("DEPRECATION")
        container.systemUiVisibility = (
            android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            )

        windowManager?.addView(container, layoutParams)
    }

    private inner class DraggableOverlayContainer(context: Context) : FrameLayout(context) {
        private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
        private var downRawX = 0f
        private var downRawY = 0f
        private var startWindowX = 0
        private var startWindowY = 0
        private var isDragging = false

        override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
            if (!isMinimizedState.value) return false
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    snapAnimator?.cancel()
                    downRawX = ev.rawX
                    downRawY = ev.rawY
                    val lp = currentMinimizedParams
                    startWindowX = lp?.x ?: bubbleX
                    startWindowY = lp?.y ?: bubbleY
                    isDragging = false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = ev.rawX - downRawX
                    val dy = ev.rawY - downRawY
                    if (hypot(dx.toDouble(), dy.toDouble()) > touchSlop) {
                        isDragging = true
                        performFocusGestureStart()
                        return true
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isDragging = false
                }
            }
            return false
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            if (!isMinimizedVolatile) return super.onTouchEvent(event)
            when (event.actionMasked) {
                MotionEvent.ACTION_MOVE -> {
                    if (isDragging) {
                        val (screenW, screenH) = getScreenBounds()
                        val marginPx = (10 * resources.displayMetrics.density).roundToInt()
                        val rawNewX = startWindowX + (event.rawX - downRawX).roundToInt()
                        val rawNewY = startWindowY + (event.rawY - downRawY).roundToInt()
                        val (clampedX, clampedY) = clampBubblePosition(
                            x = rawNewX,
                            y = rawNewY,
                            viewWidth = width,
                            viewHeight = height,
                            screenWidth = screenW,
                            screenHeight = screenH,
                            marginPx = marginPx
                        )
                        bubbleX = clampedX
                        bubbleY = clampedY
                        currentMinimizedParams?.let { lp ->
                            if (lp.x != clampedX || lp.y != clampedY) {
                                lp.x = clampedX
                                lp.y = clampedY
                                runCatching { windowManager?.updateViewLayout(this, lp) }
                            }
                        }
                        return true
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (isDragging) {
                        isDragging = false
                        val (screenW, _) = getScreenBounds()
                        val marginPx = (10 * resources.displayMetrics.density).roundToInt()
                        val centerX = bubbleX + width / 2
                        val targetEdgeX = if (centerX < screenW / 2) {
                            marginPx
                        } else {
                            (screenW - width - marginPx).coerceAtLeast(marginPx)
                        }
                        val fromX = bubbleX
                        snapAnimator?.cancel()
                        snapAnimator = ValueAnimator.ofInt(fromX, targetEdgeX).apply {
                            duration = 210L
                            interpolator = DecelerateInterpolator()
                            addUpdateListener { anim ->
                                val nextX = anim.animatedValue as Int
                                bubbleX = nextX
                                currentMinimizedParams?.let { lp ->
                                    lp.x = nextX
                                    runCatching { windowManager?.updateViewLayout(this@DraggableOverlayContainer, lp) }
                                }
                            }
                            start()
                        }
                        bubbleX = targetEdgeX
                        saveBubblePosition(targetEdgeX, bubbleY)
                        performFocusGestureEnd()
                        performClick()
                        return true
                    }
                }
            }
            return super.onTouchEvent(event)
        }

        override fun performClick(): Boolean {
            super.performClick()
            return true
        }
    }

    private fun stopLock() {
        if (!isStoppingLock.compareAndSet(false, true)) return

        snapAnimator?.cancel()
        isRunning = false
        isScheduledSession = false
        isScheduledUiState.value = false
        activeScheduleId = null
        clearActiveSession()

        if (overlayContainer != null) {
            runCatching { windowManager?.removeView(overlayContainer) }
            overlayContainer = null
            composeView = null
        }

        val capturedStart = startTime
        val endTime = System.currentTimeMillis()
        val duration = if (capturedStart > 0L) endTime - capturedStart else 0L
        startTime = 0L
        currentSessionStartTimeMs = 0L
        val appCtx = applicationContext

        val hasBlockedNotifications = NotificationBlockerRepository.finalizeSessionOnUnlock(appCtx)

        if (duration > 0) {
            val settings = FocusSettingsRepository.getCurrent(appCtx)
            FocusXpRepository.recordCompletedSession(
                context = appCtx,
                sessionStartTimeMs = capturedStart,
                durationMs = duration,
                dailyGoalMinutes = settings.dailyGoalMinutes
            )
            FocusStatsRepository.recordCompletedSession(
                context = appCtx,
                startTimeMs = capturedStart,
                durationMs = duration
            )

            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                runCatching { ShareRepository.syncLiveShareOnSessionEnd(appCtx) }
                runCatching { NothingFocusWidgetUpdater.updateAllWidgets(appCtx) }
            }
        } else {
            NothingFocusWidgetUpdater.scheduleUpdateAll(appCtx)
        }

        // Re-arm next upcoming focus schedule cleanly after unlock
        ScheduleAlarmScheduler.syncAllAlarms(
            context = appCtx,
            nowMs = System.currentTimeMillis() + 2_000L,
            triggerIfActiveNow = false
        )

        if (hasBlockedNotifications) {
            runCatching {
                val launchIntent = Intent(appCtx, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    putExtra(MainActivity.EXTRA_SHOW_UNLOCK_SUMMARY, true)
                }
                startActivity(launchIntent)
            }
        }

        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        snapAnimator?.cancel()
        isRunning = false
        isScheduledSession = false
        currentSessionStartTimeMs = 0L
        NothingFocusWidgetUpdater.scheduleUpdateAll(applicationContext)
        stopMonitoringForegroundApp()
        scope.cancel()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        if (overlayContainer != null) {
            runCatching { windowManager?.removeView(overlayContainer) }
            overlayContainer = null
            composeView = null
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "lock_channel",
            "Lock Service Channel",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }
}

@Composable
fun LockScreen(
    targetPin: String,
    onLaunchGoogleLens: () -> Unit,
    onLaunchPhone: () -> Unit,
    onLaunchXiaomiHome: () -> Unit,
    onUnlock: () -> Unit,
    allowedApps: List<AllowedApp> = FocusSettingsRepository.DEFAULT_ALLOWED_APPS,
    layout: LockAppLayout = LockAppLayout.PILL_ROW,
    minimalPillButtonSizeDp: Int = FocusSettingsRepository.DEFAULT_PILL_BUTTON_SIZE_DP,
    showSessionTimer: Boolean = true,
    hapticKeypad: Boolean = true,
    autoClearWrongPin: Boolean = true,
    sessionStartTimeMs: Long = 0L,
    onLaunchApp: ((AllowedApp) -> Unit)? = null
) {
    val themePalette = LocalFocusTheme.current
    val palette = remember(themePalette) { LockPalette.fromFocusTheme(themePalette) }
    val context = LocalContext.current
    val haptics = rememberFocusHaptics()
    val iconCache by FocusSettingsRepository.appIconsState.collectAsState()
    var enteredPin by remember { mutableStateOf("") }
    var wrongPinError by remember { mutableStateOf(false) }
    val shakeOffset = remember { Animatable(0f) }
    var pinDragAccumulated by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(allowedApps) {
        FocusSettingsRepository.preloadAppIcons(context, allowedApps)
    }

    LaunchedEffect(wrongPinError, autoClearWrongPin) {
        if (wrongPinError) {
            if (hapticKeypad) {
                haptics.reject()
            }
            for (target in listOf(-16f, 16f, -12f, 12f, -6f, 6f, 0f)) {
                shakeOffset.animateTo(
                    target,
                    spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessHigh)
                )
            }
            if (autoClearWrongPin) {
                delay(180L)
                enteredPin = ""
                wrongPinError = false
            }
        }
    }

    val resolvedAppClick: (AllowedApp) -> Unit = remember(onLaunchApp, onLaunchGoogleLens, onLaunchPhone, onLaunchXiaomiHome, haptics) {
        { app ->
            haptics.tap()
            if (onLaunchApp != null) {
                onLaunchApp(app)
            } else {
                when (app.packageName) {
                    FocusSettingsRepository.PRESET_GOOGLE_PKG -> onLaunchGoogleLens()
                    FocusSettingsRepository.PRESET_PHONE_PKG -> onLaunchPhone()
                    FocusSettingsRepository.PRESET_XIAOMI_PKG -> onLaunchXiaomiHome()
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .padding(horizontal = 28.dp, vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        ThemeBackdropLayer(palette = themePalette)

        Column(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            // Serene Minimal Timer Hero
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (showSessionTimer) {
                    ThemedSessionClockHero(
                        sessionStartTimeMs = sessionStartTimeMs,
                        themePalette = themePalette,
                        palette = palette
                    )
                } else {
                    ThemedTimerRing(
                        progress = 1f,
                        size = 112.dp,
                        palette = themePalette
                    ) {
                        FocusLockEmblem(
                            size = 52.dp,
                            showAmbientGlow = true
                        )
                    }
                }

                Text(
                    text = if (wrongPinError) {
                        "Incorrect PIN"
                    } else if (enteredPin.isNotEmpty()) {
                        "Swipe dots left to clear"
                    } else {
                        "Enter 6-digit PIN"
                    },
                    color = if (wrongPinError) MaterialTheme.colorScheme.error else palette.secondaryText,
                    fontSize = 13.sp,
                    letterSpacing = 0.2.sp
                )
            }

            // Animated Minimal PIN Dots with Shake + Swipe-Left-to-Backspace
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { translationX = shakeOffset.value }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = { pinDragAccumulated = 0f },
                            onHorizontalDrag = { _, dragAmount ->
                                pinDragAccumulated += dragAmount
                            },
                            onDragEnd = {
                                if (pinDragAccumulated < -48f && enteredPin.isNotEmpty()) {
                                    if (hapticKeypad) haptics.tick()
                                    wrongPinError = false
                                    enteredPin = if (pinDragAccumulated < -160f) {
                                        ""
                                    } else {
                                        enteredPin.dropLast(1)
                                    }
                                }
                                pinDragAccumulated = 0f
                            },
                            onDragCancel = { pinDragAccumulated = 0f }
                        )
                    }
                    .padding(vertical = 8.dp)
            ) {
                for (i in 0 until 6) {
                    val filled = i < enteredPin.length
                    val dotSize by animateDpAsState(
                        targetValue = if (filled) 14.dp else 11.dp,
                        animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium),
                        label = "pin_dot_size_$i"
                    )
                    Box(
                        modifier = Modifier
                            .size(dotSize)
                            .clip(CircleShape)
                            .background(
                                when {
                                    wrongPinError -> MaterialTheme.colorScheme.error
                                    filled -> palette.accent
                                    else -> palette.surfaceElevated
                                }
                            )
                    )
                }
            }

            // Dynamic Allowed Apps in Chosen Sleek Layout
            if (allowedApps.isNotEmpty()) {
                LockAllowedAppsSection(
                    apps = allowedApps,
                    layout = layout,
                    palette = palette,
                    onAppClick = resolvedAppClick,
                    pillButtonSizeDp = minimalPillButtonSizeDp,
                    iconCache = iconCache,
                    readFromCacheOnly = true
                )
            }

            // Minimalist Tactile Keypad
            val keys = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("", "0", "DEL")
            )

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                for (row in keys) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(22.dp, Alignment.CenterHorizontally),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for (key in row) {
                            if (key.isEmpty()) {
                                Spacer(modifier = Modifier.size(66.dp))
                            } else {
                                KeypadButton(
                                    text = key,
                                    palette = palette
                                ) {
                                    if (hapticKeypad) {
                                        if (key == "DEL") haptics.tick() else haptics.tap()
                                    }
                                    if (key == "DEL") {
                                        wrongPinError = false
                                        if (enteredPin.isNotEmpty()) {
                                            enteredPin = enteredPin.dropLast(1)
                                        }
                                    } else {
                                        if (enteredPin.length < 6) {
                                            wrongPinError = false
                                            enteredPin += key
                                            if (enteredPin == targetPin) {
                                                if (hapticKeypad) haptics.confirm()
                                                onUnlock()
                                            } else if (enteredPin.length == 6) {
                                                wrongPinError = true
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemedSessionClockHero(
    sessionStartTimeMs: Long,
    themePalette: FocusThemePalette,
    palette: LockPalette
) {
    var elapsedSeconds by remember(sessionStartTimeMs) {
        mutableLongStateOf(
            if (sessionStartTimeMs > 0L) {
                ((System.currentTimeMillis() - sessionStartTimeMs).coerceAtLeast(0L)) / 1000L
            } else {
                0L
            }
        )
    }

    LaunchedEffect(sessionStartTimeMs) {
        while (isActive) {
            if (sessionStartTimeMs > 0L) {
                elapsedSeconds = ((System.currentTimeMillis() - sessionStartTimeMs).coerceAtLeast(0L)) / 1000L
            }
            delay(1000L)
        }
    }

    val mins = elapsedSeconds / 60L
    val secs = elapsedSeconds % 60L
    val minuteProgress = ((elapsedSeconds % 60L) / 60f).let { if (it <= 0f) 1f else it }

    ThemedTimerRing(
        progress = minuteProgress,
        size = 160.dp,
        palette = themePalette
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = String.format(Locale.US, "%02d:%02d", mins, secs),
                fontFamily = themePalette.numeralFont,
                color = palette.primaryText,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "FOCUSING",
                fontFamily = themePalette.displayFont,
                color = palette.accent,
                fontSize = 9.5.sp,
                letterSpacing = 1.8.sp
            )
        }
    }
}

@Composable
fun KeypadButton(
    text: String,
    palette: LockPalette = LockPalette.StealthDark,
    onClick: () -> Unit
) {
    val themePalette = LocalFocusTheme.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale = rememberSpringPressScale(interactionSource, pressedScale = 0.88f)
    val isDelete = text == "DEL"

    if (!isDelete && themePalette.isLiquidGlass) {
        Box(
            modifier = Modifier
                .size(66.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .focusGlassPanel(
                    shape = CircleShape,
                    palette = themePalette,
                    elevated = true,
                    isPressed = isPressed
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontFamily = themePalette.numeralFont,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                color = palette.primaryText,
                textAlign = TextAlign.Center
            )
        }
    } else {
        Button(
            onClick = onClick,
            interactionSource = interactionSource,
            modifier = Modifier
                .size(66.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isDelete) Color.Transparent else palette.surface,
                contentColor = palette.primaryText
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            if (isDelete) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Delete",
                    tint = palette.secondaryText,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text(
                    text = text,
                    fontFamily = themePalette.numeralFont,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun FloatingBubble(onResumeLock: () -> Unit) {
    val palette = LocalFocusTheme.current
    val haptics = rememberFocusHaptics()
    val bubbleShape = remember { RoundedCornerShape(50) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val contentRow: @Composable () -> Unit = {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.DragIndicator,
                contentDescription = "Drag to move",
                tint = palette.textSecondary,
                modifier = Modifier.size(15.dp)
            )
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = "Lock icon",
                tint = palette.primary,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = "Return to Lock",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = palette.textPrimary
            )
        }
    }

    if (palette.isLiquidGlass) {
        Box(
            modifier = Modifier
                .padding(4.dp)
                .heightIn(min = 44.dp)
                .wrapContentSize()
                .focusGlassPanel(
                    shape = bubbleShape,
                    palette = palette,
                    elevated = true,
                    isPressed = isPressed,
                    shimmer = true
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    haptics.confirm()
                    onResumeLock()
                }
                .testTag("floating_back_to_lock_bubble"),
            contentAlignment = Alignment.Center
        ) {
            contentRow()
        }
    } else {
        Surface(
            onClick = {
                haptics.confirm()
                onResumeLock()
            },
            color = palette.pillGlassFill,
            contentColor = palette.textPrimary,
            shape = bubbleShape,
            shadowElevation = 6.dp,
            modifier = Modifier
                .padding(4.dp)
                .heightIn(min = 44.dp)
                .wrapContentSize()
                .testTag("floating_back_to_lock_bubble")
        ) {
            contentRow()
        }
    }
}
