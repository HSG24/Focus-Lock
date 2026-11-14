package com.example

import android.app.AppOpsManager
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.view.Display
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.FocusSettingsRepository
import com.example.data.GitHubReleaseInfo
import com.example.data.GitHubUpdater
import com.example.data.UpdateCheckResult
import com.example.data.UpdateUiState
import com.example.ui.components.FloatingPillBottomBar
import com.example.ui.components.PillNavTab
import com.example.ui.components.UpdateBottomSheet
import com.example.ui.components.performClockTickHaptic
import com.example.ui.screens.FocusHomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ShareScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.OledBlack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : ComponentActivity() {
    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun configureHighRefreshRate() {
        runCatching {
            val activeDisplay: Display? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                display
            } else {
                val dm = getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
                dm?.getDisplay(Display.DEFAULT_DISPLAY)
            }
            if (activeDisplay != null) {
                val currentMode = activeDisplay.mode
                val bestMode = activeDisplay.supportedModes
                    .filter {
                        it.physicalWidth == currentMode.physicalWidth &&
                            it.physicalHeight == currentMode.physicalHeight
                    }
                    .maxByOrNull { it.refreshRate }
                if (bestMode != null && bestMode.modeId != currentMode.modeId) {
                    window.attributes = window.attributes.apply {
                        preferredDisplayModeId = bestMode.modeId
                    }
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                window.frameRateBoostOnTouchEnabled = true
                window.isFrameRatePowerSavingsBalanced = false
                window.decorView.requestedFrameRate = 120f
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.BLACK),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        configureHighRefreshRate()

        setContent {
            val context = LocalContext.current
            val view = LocalView.current
            val lifecycleOwner = LocalLifecycleOwner.current
            val coroutineScope = rememberCoroutineScope()

            val settingsState by FocusSettingsRepository.observe(this).collectAsStateWithLifecycle()
            var showShare by remember { mutableStateOf(false) }
            var shareInitialDateMs by remember { mutableStateOf<Long?>(null) }
            val isLiveShareActive = !settingsState.activeLiveShareId.isNullOrBlank()

            // GitHub In-App Updater state
            var updateState by remember { mutableStateOf<UpdateUiState>(UpdateUiState.Idle) }
            var activeUpdateRelease by remember { mutableStateOf<GitHubReleaseInfo?>(null) }
            var showUpdateSheet by remember { mutableStateOf(false) }
            var awaitingInstallPermission by remember { mutableStateOf(false) }

            fun startApkDownload(release: GitHubReleaseInfo) {
                if (!GitHubUpdater.canRequestPackageInstalls(context)) {
                    awaitingInstallPermission = true
                    updateState = UpdateUiState.NeedsInstallPermission(release)
                    return
                }
                coroutineScope.launch {
                    updateState = UpdateUiState.Downloading(
                        release = release,
                        progress = 0f,
                        downloadedBytes = 0L,
                        totalBytes = release.apkSizeBytes
                    )
                    GitHubUpdater.downloadApk(context, release) { progress, downloaded, total ->
                        updateState = UpdateUiState.Downloading(
                            release = release,
                            progress = progress,
                            downloadedBytes = downloaded,
                            totalBytes = total
                        )
                    }.onSuccess { apkFile ->
                        updateState = UpdateUiState.ReadyToInstall(release, apkFile)
                        GitHubUpdater.launchPackageInstaller(context, apkFile)
                            .onFailure { err ->
                                updateState = UpdateUiState.Error(
                                    message = err.message ?: "Could not open package installer.",
                                    release = release
                                )
                            }
                    }.onFailure { err ->
                        updateState = UpdateUiState.Error(
                            message = err.message ?: "APK download failed. Check your connection.",
                            release = release
                        )
                    }
                }
            }

            fun triggerUpdateCheck(silent: Boolean) {
                if (updateState is UpdateUiState.Checking || updateState is UpdateUiState.Downloading) return
                coroutineScope.launch {
                    if (!silent) {
                        updateState = UpdateUiState.Checking
                    }
                    when (val result = GitHubUpdater.checkForUpdates(BuildConfig.VERSION_NAME)) {
                        is UpdateCheckResult.Available -> {
                            activeUpdateRelease = result.release
                            updateState = UpdateUiState.UpdateAvailable(result.release)
                            showUpdateSheet = true
                        }
                        is UpdateCheckResult.AlreadyLatest -> {
                            if (!silent) {
                                updateState = UpdateUiState.UpToDate(result.currentVersion)
                            }
                        }
                        is UpdateCheckResult.Failed -> {
                            if (!silent) {
                                updateState = UpdateUiState.Error(result.message)
                            }
                        }
                    }
                }
            }

            // Silent background update check on launch
            LaunchedEffect(Unit) {
                triggerUpdateCheck(silent = true)
            }

            // Resume APK download automatically after granting unknown-sources permission
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        configureHighRefreshRate()
                        if (awaitingInstallPermission) {
                            awaitingInstallPermission = false
                            val release = activeUpdateRelease
                            if (release != null && GitHubUpdater.canRequestPackageInstalls(context)) {
                                startApkDownload(release)
                            }
                        }
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            val navTabs = remember {
                listOf(
                    PillNavTab(
                        label = "Focus",
                        selectedIcon = Icons.Filled.Lock,
                        unselectedIcon = Icons.Outlined.Lock,
                        testTag = "nav_home"
                    ),
                    PillNavTab(
                        label = "Stats",
                        selectedIcon = Icons.Filled.Assessment,
                        unselectedIcon = Icons.Outlined.Assessment,
                        testTag = "nav_stats"
                    ),
                    PillNavTab(
                        label = "Settings",
                        selectedIcon = Icons.Filled.Settings,
                        unselectedIcon = Icons.Outlined.Settings,
                        testTag = "nav_settings"
                    )
                )
            }

            val pagerState = rememberPagerState(initialPage = 0) { navTabs.size }

            // Subtle haptic tick when swiping between main tabs
            LaunchedEffect(pagerState) {
                snapshotFlow { pagerState.settledPage }
                    .distinctUntilChanged()
                    .drop(1)
                    .collect {
                        view.performClockTickHaptic()
                    }
            }

            MyApplicationTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(OledBlack)
                ) {
                    AnimatedContent(
                        targetState = showShare,
                        transitionSpec = {
                            if (targetState) {
                                (slideInVertically(
                                    animationSpec = spring(
                                        dampingRatio = 0.85f,
                                        stiffness = Spring.StiffnessMediumLow
                                    ),
                                    initialOffsetY = { it / 6 }
                                ) + fadeIn()) togetherWith fadeOut()
                            } else {
                                fadeIn() togetherWith (slideOutVertically(
                                    animationSpec = spring(
                                        dampingRatio = 0.9f,
                                        stiffness = Spring.StiffnessMedium
                                    ),
                                    targetOffsetY = { it / 6 }
                                ) + fadeOut())
                            }
                        },
                        label = "share_overlay_transition"
                    ) { isShareOpen ->
                        if (isShareOpen) {
                            ShareScreen(
                                settingsState = settingsState,
                                initialDateMs = shareInitialDateMs,
                                onBack = { showShare = false }
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize()) {
                                HorizontalPager(
                                    state = pagerState,
                                    beyondViewportPageCount = 1,
                                    flingBehavior = PagerDefaults.flingBehavior(
                                        state = pagerState,
                                        snapAnimationSpec = spring(
                                            dampingRatio = 0.84f,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    ),
                                    modifier = Modifier.fillMaxSize()
                                ) { page ->
                                    when (page) {
                                        0 -> FocusHomeScreen(
                                            onOpenShare = {
                                                shareInitialDateMs = null
                                                showShare = true
                                            },
                                            isLiveShareActive = isLiveShareActive
                                        )
                                        1 -> StatsScreen(
                                            settingsState = settingsState,
                                            onOpenShare = { dateMs ->
                                                shareInitialDateMs = dateMs
                                                showShare = true
                                            },
                                            isLiveShareActive = isLiveShareActive
                                        )
                                        else -> SettingsScreen(
                                            settingsState = settingsState,
                                            updateUiState = updateState,
                                            onCheckForUpdates = { triggerUpdateCheck(silent = false) },
                                            onOpenUpdateSheet = {
                                                if (activeUpdateRelease != null) {
                                                    showUpdateSheet = true
                                                }
                                            }
                                        )
                                    }
                                }

                                FloatingPillBottomBar(
                                    tabs = navTabs,
                                    selectedIndex = pagerState.currentPage,
                                    pagerPosition = pagerState.currentPage + pagerState.currentPageOffsetFraction,
                                    onSelectTab = { targetIndex ->
                                        if (targetIndex != pagerState.currentPage) {
                                            view.performClockTickHaptic()
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(
                                                    page = targetIndex,
                                                    animationSpec = spring(
                                                        dampingRatio = 0.82f,
                                                        stiffness = Spring.StiffnessMediumLow
                                                    )
                                                )
                                            }
                                        }
                                    },
                                    modifier = Modifier.align(Alignment.BottomCenter)
                                )
                            }
                        }
                    }

                    val sheetRelease = activeUpdateRelease
                    if (showUpdateSheet && sheetRelease != null) {
                        UpdateBottomSheet(
                            updateState = updateState,
                            release = sheetRelease,
                            onUpdateNow = {
                                if (GitHubUpdater.canRequestPackageInstalls(context)) {
                                    startApkDownload(sheetRelease)
                                } else {
                                    awaitingInstallPermission = true
                                    updateState = UpdateUiState.NeedsInstallPermission(sheetRelease)
                                    GitHubUpdater.openUnknownAppSourcesSettings(context)
                                }
                            },
                            onOpenInstallPermission = {
                                awaitingInstallPermission = true
                                GitHubUpdater.openUnknownAppSourcesSettings(context)
                            },
                            onInstallDownloaded = {
                                val ready = updateState as? UpdateUiState.ReadyToInstall
                                if (ready != null) {
                                    GitHubUpdater.launchPackageInstaller(context, ready.apkFile)
                                        .onFailure { err ->
                                            updateState = UpdateUiState.Error(
                                                message = err.message ?: "Could not launch installer.",
                                                release = sheetRelease
                                            )
                                        }
                                } else {
                                    startApkDownload(sheetRelease)
                                }
                            },
                            onDismiss = {
                                showUpdateSheet = false
                            }
                        )
                    }
                }
            }
        }
    }
}

data class Session(val startTime: Long, val duration: Long)

suspend fun fetchAllSessions(): List<Session> {
    return withContext(Dispatchers.IO) {
        val list = mutableListOf<Session>()
        try {
            val client = OkHttpClient()
            val url = "https://focuslock-b6c0f-default-rtdb.asia-southeast1.firebasedatabase.app/sessions.json"
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: return@withContext list
                if (responseBody == "null") return@withContext list

                val jsonObject = org.json.JSONObject(responseBody)
                val keys = jsonObject.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val sessionObj = jsonObject.getJSONObject(key)
                    val startTime = sessionObj.optLong("startTime", 0L)
                    val duration = sessionObj.optLong("duration", 0L)
                    if (startTime > 0 && duration > 0) {
                        list.add(Session(startTime, duration))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        list
    }
}

fun isSameDay(timeMillis: Long, calendar: Calendar): Boolean {
    val cal = Calendar.getInstance().apply { timeInMillis = timeMillis }
    return cal.get(Calendar.YEAR) == calendar.get(Calendar.YEAR) &&
        cal.get(Calendar.DAY_OF_YEAR) == calendar.get(Calendar.DAY_OF_YEAR)
}

fun getFormattedDate(calendar: Calendar): String {
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DATE, -1) }

    return when {
        isSameDayCalendar(calendar, today) -> "Today"
        isSameDayCalendar(calendar, yesterday) -> "Yesterday"
        else -> {
            val sdf = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
            sdf.format(calendar.time)
        }
    }
}

fun isSameDayCalendar(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
        cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

fun formatDurationMinutes(timeInMs: Long): String {
    val minutes = timeInMs / (1000 * 60)
    val seconds = (timeInMs % (1000 * 60)) / 1000
    return if (minutes > 0) "${minutes}m" else "${seconds}s"
}

fun calculateStreak(allSessions: List<Session>): Int {
    if (allSessions.isEmpty()) return 0
    val uniqueDays = allSessions.map {
        val cal = Calendar.getInstance().apply { timeInMillis = it.startTime }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.timeInMillis
    }.distinct().sortedDescending()

    if (uniqueDays.isEmpty()) return 0

    var streak = 0
    val todayCal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val todayMs = todayCal.timeInMillis
    val yesterdayMs = todayCal.apply { add(Calendar.DATE, -1) }.timeInMillis

    val latestDay = uniqueDays.first()
    if (latestDay != todayMs && latestDay != yesterdayMs) {
        return 0
    }

    var currentCheck = latestDay
    for (day in uniqueDays) {
        if (day == currentCheck) {
            streak++
            val checkCal = Calendar.getInstance().apply { timeInMillis = currentCheck }
            checkCal.add(Calendar.DATE, -1)
            currentCheck = checkCal.timeInMillis
        } else {
            break
        }
    }
    return streak
}

fun calculateMaxStreak(allSessions: List<Session>): Int {
    if (allSessions.isEmpty()) return 0
    val uniqueDays = allSessions.map {
        val cal = Calendar.getInstance().apply { timeInMillis = it.startTime }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.timeInMillis
    }.distinct().sorted()

    if (uniqueDays.isEmpty()) return 0

    var maxStreak = 1
    var currentStreak = 1

    val calPrev = Calendar.getInstance()
    val calCurr = Calendar.getInstance()

    for (i in 1 until uniqueDays.size) {
        calPrev.timeInMillis = uniqueDays[i - 1]
        calCurr.timeInMillis = uniqueDays[i]

        calPrev.add(Calendar.DATE, 1)
        if (isSameDayCalendar(calPrev, calCurr)) {
            currentStreak++
        } else {
            if (currentStreak > maxStreak) {
                maxStreak = currentStreak
            }
            currentStreak = 1
        }
    }
    if (currentStreak > maxStreak) {
        maxStreak = currentStreak
    }
    return maxStreak
}

suspend fun uploadPinToFirebase(pin: String): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            val url = "https://focuslock-b6c0f-default-rtdb.asia-southeast1.firebasedatabase.app/current_lock/pin.json"
            val body = "\"$pin\"".toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(url)
                .put(body)
                .build()

            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
