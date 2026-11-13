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
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.KeyEvent
import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.outlined.Lock
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
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
import com.example.data.LockAppLayout
import com.example.ui.components.LockAllowedAppsSection
import com.example.ui.components.LockPalette
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.MyApplicationTheme
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
            var lastObservedForegroundPkg: String? = null
            var unauthorizedStreak = 0

            while (isActive) {
                if (isMinimizedState.value) {
                    val currentMinimizeTs = minimizeTimestamp
                    if (currentMinimizeTs != trackedSessionMinimizeTs) {
                        trackedSessionMinimizeTs = currentMinimizeTs
                        lastQueryTime = currentMinimizeTs
                        lastObservedForegroundPkg = null
                        unauthorizedStreak = 0
                    }

                    val settings = FocusSettingsRepository.getCurrent(this@LockService)
                    val graceMs = settings.appSwitchGraceSeconds * 1000L
                    val now = System.currentTimeMillis()

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
                                lastObservedForegroundPkg = pkg
                            }
                        }
                    }
                    lastQueryTime = maxOf(lastQueryTime, latestEventTs + 1L)

                    if (now > currentMinimizeTs + graceMs) {
                        val fgPkg = lastObservedForegroundPkg
                        val allowedPackages = getAllowedPackages()
                        if (fgPkg != null && fgPkg !in allowedPackages) {
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
                    lastObservedForegroundPkg = null
                    unauthorizedStreak = 0
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
                MyApplicationTheme(themeMode = settingsState.themeMode) {
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
                            followAppTheme = settingsState.lockScreenFollowsTheme,
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

        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        if (duration > 0) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val client = okhttp3.OkHttpClient()
                    val url = "https://focuslock-b6c0f-default-rtdb.asia-southeast1.firebasedatabase.app/sessions.json"
                    val bodyStr = """{"startTime": $startTime, "duration": $duration}"""
                    val body = bodyStr.toRequestBody("application/json; charset=utf-8".toMediaType())
                    val request = okhttp3.Request.Builder()
                        .url(url)
                        .post(body)
                        .build()
                    client.newCall(request).execute()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
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
    followAppTheme: Boolean = false,
    sessionStartTimeMs: Long = 0L,
    onLaunchApp: ((AllowedApp) -> Unit)? = null
) {
    val palette = if (followAppTheme) {
        LockPalette.fromMaterialTheme()
    } else {
        LockPalette.StealthDark
    }
    val haptic = LocalHapticFeedback.current
    var enteredPin by remember { mutableStateOf("") }
    var wrongPinError by remember { mutableStateOf(false) }

    var elapsedSeconds by remember(sessionStartTimeMs) {
        mutableLongStateOf(
            if (sessionStartTimeMs > 0L) ((System.currentTimeMillis() - sessionStartTimeMs) / 1000L).coerceAtLeast(0L) else 0L
        )
    }

    LaunchedEffect(showSessionTimer, sessionStartTimeMs) {
        if (showSessionTimer && sessionStartTimeMs > 0L) {
            while (true) {
                elapsedSeconds = ((System.currentTimeMillis() - sessionStartTimeMs) / 1000L).coerceAtLeast(0L)
                delay(1000L)
            }
        }
    }

    LaunchedEffect(wrongPinError, autoClearWrongPin) {
        if (wrongPinError && autoClearWrongPin) {
            delay(350L)
            enteredPin = ""
            wrongPinError = false
        }
    }

    val resolvedAppClick: (AllowedApp) -> Unit = { app ->
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .padding(horizontal = 24.dp, vertical = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Minimal Lock Header + Optional Live Timer
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(palette.surface)
                        .border(1.dp, palette.border, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = "Locked",
                        modifier = Modifier.size(24.dp),
                        tint = palette.accent
                    )
                }

                Text(
                    text = "Focus Mode Active",
                    color = palette.primaryText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                if (showSessionTimer) {
                    val hrs = elapsedSeconds / 3600
                    val mins = (elapsedSeconds % 3600) / 60
                    val secs = elapsedSeconds % 60
                    val timerFormatted = if (hrs > 0) {
                        String.format(Locale.US, "%02d:%02d:%02d", hrs, mins, secs)
                    } else {
                        String.format(Locale.US, "%02d:%02d", mins, secs)
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = palette.surface,
                        border = BorderStroke(1.dp, palette.border)
                    ) {
                        Text(
                            text = timerFormatted,
                            fontFamily = JetBrainsMono,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.accent,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                        )
                    }
                }

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

            // Dynamic Allowed Apps in Chosen Sleek Layout
            if (allowedApps.isNotEmpty()) {
                LockAllowedAppsSection(
                    apps = allowedApps,
                    layout = layout,
                    palette = palette,
                    onAppClick = resolvedAppClick
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
fun KeypadButton(
    text: String,
    palette: LockPalette = LockPalette.StealthDark,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(64.dp),
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
                fontFamily = JetBrainsMono,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun FloatingBubble(onResumeLock: () -> Unit) {
    Surface(
        onClick = onResumeLock,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
        shadowElevation = 6.dp,
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
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "Back to Lock",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
