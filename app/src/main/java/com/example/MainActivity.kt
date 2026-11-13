package com.example

import android.app.AppOpsManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Process
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.FocusSettingsRepository
import com.example.ui.screens.FocusHomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class Screen { HOME, STATS }

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsState by FocusSettingsRepository.observe(this).collectAsStateWithLifecycle()
            var currentScreen by remember { mutableStateOf(Screen.HOME) }
            var showSettings by remember { mutableStateOf(false) }

            MyApplicationTheme(themeMode = settingsState.themeMode) {
                Crossfade(
                    targetState = showSettings,
                    animationSpec = tween(durationMillis = 200),
                    label = "root_navigation"
                ) { isSettingsOpen ->
                    if (isSettingsOpen) {
                        SettingsScreen(
                            settingsState = settingsState,
                            onBack = { showSettings = false }
                        )
                    } else {
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            containerColor = MaterialTheme.colorScheme.background,
                            contentWindowInsets = WindowInsets(0, 0, 0, 0),
                            bottomBar = {
                                Column {
                                    HorizontalDivider(
                                        thickness = 0.5.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant
                                    )
                                    NavigationBar(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        tonalElevation = 0.dp
                                    ) {
                                        NavigationBarItem(
                                            icon = {
                                                Icon(
                                                    imageVector = if (currentScreen == Screen.HOME) {
                                                        Icons.Filled.Lock
                                                    } else {
                                                        Icons.Outlined.Lock
                                                    },
                                                    contentDescription = "Focus"
                                                )
                                            },
                                            label = { Text("Focus") },
                                            selected = currentScreen == Screen.HOME,
                                            onClick = { currentScreen = Screen.HOME },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            modifier = Modifier.testTag("nav_home")
                                        )
                                        NavigationBarItem(
                                            icon = {
                                                Icon(
                                                    imageVector = if (currentScreen == Screen.STATS) {
                                                        Icons.Filled.Assessment
                                                    } else {
                                                        Icons.Outlined.Assessment
                                                    },
                                                    contentDescription = "Stats"
                                                )
                                            },
                                            label = { Text("Stats") },
                                            selected = currentScreen == Screen.STATS,
                                            onClick = { currentScreen = Screen.STATS },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            modifier = Modifier.testTag("nav_stats")
                                        )
                                    }
                                }
                            }
                        ) { innerPadding ->
                            when (currentScreen) {
                                Screen.HOME -> FocusHomeScreen(
                                    onOpenSettings = { showSettings = true },
                                    modifier = Modifier.padding(innerPadding)
                                )
                                Screen.STATS -> StatsScreen(
                                    settingsState = settingsState,
                                    onOpenSettings = { showSettings = true },
                                    modifier = Modifier.padding(innerPadding)
                                )
                            }
                        }
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
