package com.example

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
import android.view.Display
import android.view.Gravity
import android.view.KeyEvent
import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.data.AllowedApp
import com.example.data.FocusSettingsRepository
import com.example.data.FocusStatsRepository
import com.example.data.FocusXpRepository
import com.example.data.LockAppLayout
import com.example.data.NotificationBlockerRepository
import com.example.data.ShareRepository
import com.example.ui.components.FocusLockEmblem
import com.example.ui.components.LockAllowedAppsSection
import com.example.ui.components.LockPalette
import com.example.ui.components.ThemeBackdropLayer
import com.example.ui.components.ThemedDivider
import com.example.ui.components.ThemedTimerRing
import com.example.ui.components.rememberSpringPressScale
import com.example.ui.theme.AppVisualTheme
import com.example.ui.theme.FocusThemePalette
import com.example.ui.theme.LocalFocusTheme
import com.example.ui.theme.MyApplicationTheme
import com.example.widget.NothingFocusWidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Locale

class LockService : Service(), LifecycleOwner, SavedStateRegistryOwner {

    companion object {
        @Volatile
        var currentSessionStartTimeMs: Long = 0L
    }

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    private var windowManager: WindowManager? = null
    private var composeView: ComposeView? = null
    private var targetPin: String = ""
    private var startTime: Long = 0

    @Volatile
    private var minimizeTimestamp: Long = 0L

    private var isMinimizedState = mutableStateOf(false)
    private var monitorJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

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
                if (isMinimizedState.value) {
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
                                    isMinimizedState.value = false
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
        if (startTime == 0L) {
            startTime = System.currentTimeMillis()
            currentSessionStartTimeMs = startTime
            NotificationBlockerRepository.startFreshSession(applicationContext)
            NothingFocusWidgetUpdater.scheduleUpdateAll(applicationContext)
            scope.launch {
                ShareRepository.syncLiveShareOnSessionStart(applicationContext, startTime)
            }
        }

        when (intent?.action) {
            "MAXIMIZE" -> {
                isMinimizedState.value = false
                updateOverlayState(false)
                return START_STICKY
            }
            "MINIMIZE" -> {
                val now = System.currentTimeMillis()
                if (!isMinimizedState.value || now - minimizeTimestamp > 500L) {
                    minimizeTimestamp = now
                    isMinimizedState.value = true
                    updateOverlayState(true)
                }
                return START_STICKY
            }
            else -> {
                targetPin = intent?.getStringExtra("TARGET_PIN") ?: intent?.getStringExtra("PIN") ?: "000000"
            }
        }

        createNotificationChannel()
        val notification = Notification.Builder(this, "lock_channel")
            .setContentTitle("Focus Lock Active")
            .setContentText("Your phone is currently locked for studying.")
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

        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

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

    private fun updateOverlayState(minimized: Boolean) {
        val view = composeView ?: return
        val wm = windowManager ?: return

        val layoutParams = if (minimized) {
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.END
                x = 32
                y = 150
            }
        } else {
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

        @Suppress("DEPRECATION")
        if (minimized) {
            view.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        } else {
            view.systemUiVisibility = (
                android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                    or android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                )
        }

        wm.updateViewLayout(view, layoutParams)
    }

    private fun launchAllowedApp(app: AllowedApp) {
        minimizeTimestamp = System.currentTimeMillis()
        isMinimizedState.value = true
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
        if (composeView != null) return

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@LockService)
            setViewTreeSavedStateRegistryOwner(this@LockService)

            setContent {
                val settingsState by FocusSettingsRepository.observe(this@LockService).collectAsState()
                MyApplicationTheme(appTheme = settingsState.appTheme) {
                    val minimized by isMinimizedState

                    if (minimized) {
                        FloatingBubble(
                            onResumeLock = {
                                isMinimizedState.value = false
                                updateOverlayState(false)
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
            composeView?.requestedFrameRate = 120f
        }
        composeView?.isFocusableInTouchMode = true

        @Suppress("DEPRECATION")
        composeView?.systemUiVisibility = (
            android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            )

        composeView?.setOnKeyListener { _, keyCode, _ ->
            keyCode == KeyEvent.KEYCODE_BACK
        }

        windowManager?.addView(composeView, layoutParams)
    }

    private fun stopLock() {
        if (composeView != null) {
            windowManager?.removeView(composeView)
            composeView = null
        }

        val capturedStart = startTime
        val endTime = System.currentTimeMillis()
        val duration = endTime - capturedStart
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

            CoroutineScope(Dispatchers.IO).launch {
                ShareRepository.syncLiveShareOnSessionEnd(appCtx)
                NothingFocusWidgetUpdater.updateAllWidgets(appCtx)
            }
        } else {
            NothingFocusWidgetUpdater.scheduleUpdateAll(appCtx)
        }

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
        currentSessionStartTimeMs = 0L
        NothingFocusWidgetUpdater.scheduleUpdateAll(applicationContext)
        stopMonitoringForegroundApp()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        if (composeView != null) {
            windowManager?.removeView(composeView)
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
    showSessionTimer: Boolean = true,
    hapticKeypad: Boolean = true,
    autoClearWrongPin: Boolean = true,
    sessionStartTimeMs: Long = 0L,
    onLaunchApp: ((AllowedApp) -> Unit)? = null
) {
    val themePalette = LocalFocusTheme.current
    val palette = remember(themePalette) { LockPalette.fromFocusTheme(themePalette) }
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val iconCache by FocusSettingsRepository.appIconsState.collectAsState()
    var enteredPin by remember { mutableStateOf("") }
    var wrongPinError by remember { mutableStateOf(false) }

    LaunchedEffect(allowedApps, themePalette.theme) {
        FocusSettingsRepository.preloadAppIcons(context, allowedApps, themePalette.theme)
    }

    LaunchedEffect(wrongPinError, autoClearWrongPin) {
        if (wrongPinError && autoClearWrongPin) {
            delay(350L)
            enteredPin = ""
            wrongPinError = false
        }
    }

    val resolvedAppClick: (AllowedApp) -> Unit = remember(onLaunchApp, onLaunchGoogleLens, onLaunchPhone, onLaunchXiaomiHome) {
        { app ->
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
            .padding(horizontal = 24.dp, vertical = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        ThemeBackdropLayer(palette = themePalette)

        Column(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Themed Lock Header + Ring + Live Session Clock
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
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(palette.surface)
                                .border(1.dp, themePalette.primaryBorderTint, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            FocusLockEmblem(
                                size = 48.dp,
                                showAmbientGlow = true
                            )
                        }
                    }
                }

                val lockHeaderLabel = when (themePalette.theme) {
                    AppVisualTheme.NOTHING_CRIMSON -> "FOCUS MODE ACTIVE"
                    AppVisualTheme.CYBER_ICE -> "HUD LOCK // ENGAGED"
                    AppVisualTheme.SOLAR_AMBER -> "CHRONO LOCK ACTIVE"
                    AppVisualTheme.ULTRAVIOLET_PULSE -> "Focus Mode Active"
                }

                Text(
                    text = lockHeaderLabel,
                    fontFamily = themePalette.displayFont,
                    color = palette.primaryText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = if (themePalette.theme == AppVisualTheme.NOTHING_CRIMSON) 1.2.sp else 0.sp
                )

                Text(
                    text = if (wrongPinError) "Incorrect PIN" else "Enter 6-digit PIN to unlock",
                    color = if (wrongPinError) MaterialTheme.colorScheme.error else palette.secondaryText,
                    fontSize = 13.sp
                )
            }

            // Sleek PIN Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                modifier = Modifier.fillMaxWidth()
            ) {
                for (i in 0 until 6) {
                    val filled = i < enteredPin.length
                    Box(
                        modifier = Modifier
                            .size(13.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    wrongPinError -> MaterialTheme.colorScheme.error
                                    filled -> palette.accent
                                    else -> Color.Transparent
                                }
                            )
                            .border(
                                width = 1.5.dp,
                                color = when {
                                    wrongPinError -> MaterialTheme.colorScheme.error
                                    filled -> palette.accent
                                    else -> palette.border
                                },
                                shape = CircleShape
                            )
                    )
                }
            }

            if (themePalette.useDottedDividers) {
                ThemedDivider(
                    palette = themePalette,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            // Dynamic Allowed Apps in Chosen Sleek Layout
            if (allowedApps.isNotEmpty()) {
                LockAllowedAppsSection(
                    apps = allowedApps,
                    layout = layout,
                    palette = palette,
                    onAppClick = resolvedAppClick,
                    iconCache = iconCache,
                    readFromCacheOnly = true
                )
            }

            // Minimalist Keypad
            val keys = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("", "0", "DEL")
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                for (row in keys) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterHorizontally),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for (key in row) {
                            if (key.isEmpty()) {
                                Spacer(modifier = Modifier.size(64.dp))
                            } else {
                                KeypadButton(
                                    text = key,
                                    palette = palette
                                ) {
                                    if (hapticKeypad) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
        size = 148.dp,
        palette = themePalette
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            FocusLockEmblem(
                size = 30.dp,
                showAmbientGlow = false
            )
            Text(
                text = String.format(Locale.US, "%02d:%02d", mins, secs),
                fontFamily = themePalette.numeralFont,
                color = palette.primaryText,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "FOCUS",
                fontFamily = themePalette.displayFont,
                color = palette.accent,
                fontSize = 10.sp,
                letterSpacing = 1.4.sp
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
    val scale = rememberSpringPressScale(interactionSource, pressedScale = 0.90f)

    Button(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = Modifier
            .size(64.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = palette.surface,
            contentColor = palette.primaryText
        ),
        border = BorderStroke(1.dp, palette.border),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        if (text == "DEL") {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Backspace,
                contentDescription = "Delete",
                tint = palette.secondaryText,
                modifier = Modifier.size(18.dp)
            )
        } else {
            Text(
                text = text,
                fontFamily = themePalette.numeralFont,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun FloatingBubble(onResumeLock: () -> Unit) {
    val palette = LocalFocusTheme.current
    Surface(
        onClick = onResumeLock,
        color = palette.surfaceElevated,
        contentColor = palette.textPrimary,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, palette.primaryBorderTint),
        shadowElevation = 0.dp,
        modifier = Modifier
            .padding(8.dp)
            .wrapContentSize()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = "Lock icon",
                tint = palette.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "Back to Lock",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.textPrimary
            )
        }
    }
}
