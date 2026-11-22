package com.example.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Immutable
import com.example.Session
import com.example.calculateStreak
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class FocusRank(
    val title: String,
    val minXp: Int,
    val nextTierXp: Int?,
    val badgeCode: String
) {
    INITIATE("Initiate", 0, 500, "TIER 01"),
    APPRENTICE("Apprentice", 500, 1500, "TIER 02"),
    TACTICIAN("Tactician", 1500, 3500, "TIER 03"),
    VANGUARD("Vanguard", 3500, 7000, "TIER 04"),
    MONK("Monk", 7000, 15000, "TIER 05"),
    SOVEREIGN("Sovereign", 15000, null, "TIER 06");

    companion object {
        fun forXp(totalXp: Int): FocusRank {
            val clamped = totalXp.coerceAtLeast(0)
            return when {
                clamped >= SOVEREIGN.minXp -> SOVEREIGN
                clamped >= MONK.minXp -> MONK
                clamped >= VANGUARD.minXp -> VANGUARD
                clamped >= TACTICIAN.minXp -> TACTICIAN
                clamped >= APPRENTICE.minXp -> APPRENTICE
                else -> INITIATE
            }
        }
    }
}

@Immutable
data class XpHistoryEntry(
    val id: String,
    val timestampMs: Long,
    val dateKey: String,
    val deltaXp: Int,
    val reason: String,
    val resultingTotalXp: Int
)

@Immutable
data class FocusXpState(
    val totalXp: Int = 0,
    val rank: FocusRank = FocusRank.INITIATE,
    val progressToNextRank: Float = 0f,
    val xpToNextRank: Int = 500,
    val todayFocusMs: Long = 0L,
    val currentStreak: Int = 0,
    val trackingStartDateKey: String = "",
    val history: List<XpHistoryEntry> = emptyList()
)

object FocusXpRepository {
    private const val PREFS_NAME = "focus_xp_prefs"
    private const val KEY_TOTAL_XP = "total_xp"
    private const val KEY_HISTORY_JSON = "xp_history_json"
    private const val KEY_TRACKING_START_KEY = "tracking_start_date_key"
    private const val KEY_EVALUATED_DAYS = "evaluated_penalty_days_json"
    private const val KEY_BONUS_AWARDED_DAYS = "bonus_awarded_days_json"
    private const val KEY_DAILY_FOCUS_MS_MAP = "daily_focus_ms_map_json"
    private const val KEY_CACHED_STREAK = "cached_streak"

    const val XP_PER_MINUTE = 10
    const val DAILY_GOAL_BONUS_XP = 100
    const val MISSED_GOAL_PENALTY_XP = 150

    @Volatile
    private var _state: MutableStateFlow<FocusXpState>? = null

    fun observe(context: Context): StateFlow<FocusXpState> {
        val appContext = context.applicationContext
        _state?.let { return it.asStateFlow() }
        synchronized(this) {
            _state?.let { return it.asStateFlow() }
            val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ensureTrackingStartInitialized(prefs)
            val flow = MutableStateFlow(readState(prefs))
            _state = flow
            return flow.asStateFlow()
        }
    }

    fun getCurrent(context: Context): FocusXpState {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        ensureTrackingStartInitialized(prefs)
        return readState(prefs)
    }

    @Synchronized
    fun reloadFromPrefs(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        ensureTrackingStartInitialized(prefs)
        _state?.value = readState(prefs)
    }

    fun dateKeyForMs(timeMs: Long): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(timeMs))
    }

    private fun parseDateKeyToCal(dateKey: String): Calendar? {
        return runCatching {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
            val date = sdf.parse(dateKey) ?: return null
            Calendar.getInstance().apply {
                time = date
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        }.getOrNull()
    }

    private fun ensureTrackingStartInitialized(prefs: SharedPreferences): String {
        val existing = prefs.getString(KEY_TRACKING_START_KEY, null)?.takeIf { it.isNotBlank() }
        if (existing != null) return existing
        val todayKey = dateKeyForMs(System.currentTimeMillis())
        prefs.edit().putString(KEY_TRACKING_START_KEY, todayKey).apply()
        return todayKey
    }

    @Synchronized
    fun recordCompletedSession(
        context: Context,
        sessionStartTimeMs: Long,
        durationMs: Long,
        dailyGoalMinutes: Long
    ) {
        if (durationMs <= 0L) return
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        ensureTrackingStartInitialized(prefs)

        val dateKey = dateKeyForMs(if (sessionStartTimeMs > 0L) sessionStartTimeMs else System.currentTimeMillis())
        val dailyMap = readLongMap(prefs.getString(KEY_DAILY_FOCUS_MS_MAP, "{}"))
        val updatedDayMs = (dailyMap[dateKey] ?: 0L) + durationMs
        dailyMap[dateKey] = updatedDayMs

        var totalXp = prefs.getInt(KEY_TOTAL_XP, 0).coerceAtLeast(0)
        val history = readHistory(prefs.getString(KEY_HISTORY_JSON, "[]")).toMutableList()
        val now = System.currentTimeMillis()

        val completedMinutes = (durationMs / 60_000L).toInt().coerceAtLeast(0)
        val earnedXp = completedMinutes * XP_PER_MINUTE
        if (earnedXp > 0) {
            totalXp += earnedXp
            history.add(
                0,
                XpHistoryEntry(
                    id = UUID.randomUUID().toString(),
                    timestampMs = now,
                    dateKey = dateKey,
                    deltaXp = earnedXp,
                    reason = "Focused ${completedMinutes}m (+${XP_PER_MINUTE} XP/min)",
                    resultingTotalXp = totalXp
                )
            )
        }

        val bonusDays = readStringSet(prefs.getString(KEY_BONUS_AWARDED_DAYS, "[]")).toMutableSet()
        val goalMs = dailyGoalMinutes.coerceAtLeast(1L) * 60_000L
        if (!bonusDays.contains(dateKey) && updatedDayMs >= goalMs) {
            bonusDays.add(dateKey)
            totalXp += DAILY_GOAL_BONUS_XP
            history.add(
                0,
                XpHistoryEntry(
                    id = UUID.randomUUID().toString(),
                    timestampMs = now + 1L,
                    dateKey = dateKey,
                    deltaXp = DAILY_GOAL_BONUS_XP,
                    reason = "Daily study goal reached (+$DAILY_GOAL_BONUS_XP XP)",
                    resultingTotalXp = totalXp
                )
            )
        }

        val computedStreak = calculateStreakFromDailyMap(dailyMap, Calendar.getInstance())
        val currentStreak = maxOf(computedStreak, 1)

        prefs.edit()
            .putInt(KEY_TOTAL_XP, totalXp)
            .putString(KEY_HISTORY_JSON, serializeHistory(history.take(60)))
            .putString(KEY_BONUS_AWARDED_DAYS, serializeStringSet(bonusDays))
            .putString(KEY_DAILY_FOCUS_MS_MAP, serializeLongMap(dailyMap))
            .putInt(KEY_CACHED_STREAK, currentStreak)
            .apply()

        _state?.value = readState(prefs)
    }

    @Synchronized
    fun syncWithRemoteSessions(
        context: Context,
        sessions: List<Session>,
        dailyGoalMinutes: Long
    ) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        ensureTrackingStartInitialized(prefs)

        val dailyMap = readLongMap(prefs.getString(KEY_DAILY_FOCUS_MS_MAP, "{}"))
        val remoteByDay = sessions.groupBy { dateKeyForMs(it.startTime) }
            .mapValues { (_, list) -> list.sumOf { it.duration } }

        for ((key, remoteTotalMs) in remoteByDay) {
            dailyMap[key] = maxOf(dailyMap[key] ?: 0L, remoteTotalMs)
        }

        val streak = calculateStreak(sessions)
        prefs.edit()
            .putString(KEY_DAILY_FOCUS_MS_MAP, serializeLongMap(dailyMap))
            .putInt(KEY_CACHED_STREAK, streak)
            .apply()

        evaluateMissedDailyGoalsInternal(prefs, dailyGoalMinutes, Calendar.getInstance())
        _state?.value = readState(prefs)
    }

    @Synchronized
    fun evaluateMissedDailyGoals(
        context: Context,
        dailyGoalMinutes: Long,
        nowCal: Calendar = Calendar.getInstance()
    ) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        ensureTrackingStartInitialized(prefs)
        evaluateMissedDailyGoalsInternal(prefs, dailyGoalMinutes, nowCal)
        _state?.value = readState(prefs)
    }

    private fun evaluateMissedDailyGoalsInternal(
        prefs: SharedPreferences,
        dailyGoalMinutes: Long,
        nowCal: Calendar
    ) {
        val startKey = ensureTrackingStartInitialized(prefs)
        val startCal = parseDateKeyToCal(startKey) ?: return
        val todayStartCal = (nowCal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val evaluatedDays = readStringSet(prefs.getString(KEY_EVALUATED_DAYS, "[]")).toMutableSet()
        val dailyMap = readLongMap(prefs.getString(KEY_DAILY_FOCUS_MS_MAP, "{}"))
        var totalXp = prefs.getInt(KEY_TOTAL_XP, 0).coerceAtLeast(0)
        val history = readHistory(prefs.getString(KEY_HISTORY_JSON, "[]")).toMutableList()

        val goalMs = dailyGoalMinutes.coerceAtLeast(1L) * 60_000L
        var modified = false

        val cursor = startCal.clone() as Calendar
        var safetyCounter = 0
        while (cursor.before(todayStartCal) && safetyCounter < 90) {
            safetyCounter++
            val dayKey = dateKeyForMs(cursor.timeInMillis)
            if (!evaluatedDays.contains(dayKey)) {
                evaluatedDays.add(dayKey)
                modified = true
                val focusedMs = dailyMap[dayKey] ?: 0L
                if (focusedMs < goalMs) {
                    val newXp = (totalXp - MISSED_GOAL_PENALTY_XP).coerceAtLeast(0)
                    val actualDelta = newXp - totalXp
                    totalXp = newXp
                    history.add(
                        0,
                        XpHistoryEntry(
                            id = UUID.randomUUID().toString(),
                            timestampMs = System.currentTimeMillis(),
                            dateKey = dayKey,
                            deltaXp = actualDelta,
                            reason = "Missed ${formatGoalLabel(dailyGoalMinutes)} daily goal ($dayKey)",
                            resultingTotalXp = totalXp
                        )
                    )
                }
            }
            cursor.add(Calendar.DATE, 1)
        }

        // Also reconcile today's daily bonus if user crossed goal via remote sessions
        val todayKey = dateKeyForMs(todayStartCal.timeInMillis)
        val todayMs = dailyMap[todayKey] ?: 0L
        val bonusDays = readStringSet(prefs.getString(KEY_BONUS_AWARDED_DAYS, "[]")).toMutableSet()
        if (!bonusDays.contains(todayKey) && todayMs >= goalMs) {
            bonusDays.add(todayKey)
            totalXp += DAILY_GOAL_BONUS_XP
            history.add(
                0,
                XpHistoryEntry(
                    id = UUID.randomUUID().toString(),
                    timestampMs = System.currentTimeMillis(),
                    dateKey = todayKey,
                    deltaXp = DAILY_GOAL_BONUS_XP,
                    reason = "Daily study goal reached (+$DAILY_GOAL_BONUS_XP XP)",
                    resultingTotalXp = totalXp
                )
            )
            modified = true
        }

        if (modified) {
            prefs.edit()
                .putInt(KEY_TOTAL_XP, totalXp)
                .putString(KEY_EVALUATED_DAYS, serializeStringSet(evaluatedDays))
                .putString(KEY_BONUS_AWARDED_DAYS, serializeStringSet(bonusDays))
                .putString(KEY_HISTORY_JSON, serializeHistory(history.take(60)))
                .apply()
        }
    }

    private fun readState(prefs: SharedPreferences): FocusXpState {
        val totalXp = prefs.getInt(KEY_TOTAL_XP, 0).coerceAtLeast(0)
        val rank = FocusRank.forXp(totalXp)
        val nextXp = rank.nextTierXp
        val progress = if (nextXp == null) {
            1f
        } else {
            val span = (nextXp - rank.minXp).coerceAtLeast(1)
            ((totalXp - rank.minXp).toFloat() / span.toFloat()).coerceIn(0f, 1f)
        }
        val remaining = if (nextXp == null) 0 else (nextXp - totalXp).coerceAtLeast(0)

        val todayKey = dateKeyForMs(System.currentTimeMillis())
        val dailyMap = readLongMap(prefs.getString(KEY_DAILY_FOCUS_MS_MAP, "{}"))
        val todayMs = dailyMap[todayKey] ?: 0L
        val streak = prefs.getInt(KEY_CACHED_STREAK, 0).coerceAtLeast(0)
        val trackingStart = prefs.getString(KEY_TRACKING_START_KEY, todayKey).orEmpty().ifBlank { todayKey }
        val history = readHistory(prefs.getString(KEY_HISTORY_JSON, "[]"))

        return FocusXpState(
            totalXp = totalXp,
            rank = rank,
            progressToNextRank = progress,
            xpToNextRank = remaining,
            todayFocusMs = todayMs,
            currentStreak = streak,
            trackingStartDateKey = trackingStart,
            history = history
        )
    }

    private fun readHistory(raw: String?): List<XpHistoryEntry> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    add(
                        XpHistoryEntry(
                            id = obj.optString("id"),
                            timestampMs = obj.optLong("timestampMs", 0L),
                            dateKey = obj.optString("dateKey"),
                            deltaXp = obj.optInt("deltaXp", 0),
                            reason = obj.optString("reason"),
                            resultingTotalXp = obj.optInt("resultingTotalXp", 0)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun serializeHistory(list: List<XpHistoryEntry>): String {
        val arr = JSONArray()
        for (entry in list) {
            arr.put(
                JSONObject().apply {
                    put("id", entry.id)
                    put("timestampMs", entry.timestampMs)
                    put("dateKey", entry.dateKey)
                    put("deltaXp", entry.deltaXp)
                    put("reason", entry.reason)
                    put("resultingTotalXp", entry.resultingTotalXp)
                }
            )
        }
        return arr.toString()
    }

    private fun readStringSet(raw: String?): Set<String> {
        if (raw.isNullOrBlank()) return emptySet()
        return runCatching {
            val arr = JSONArray(raw)
            buildSet {
                for (i in 0 until arr.length()) {
                    val v = arr.optString(i)
                    if (v.isNotBlank()) add(v)
                }
            }
        }.getOrDefault(emptySet())
    }

    private fun serializeStringSet(set: Set<String>): String {
        val arr = JSONArray()
        for (item in set) arr.put(item)
        return arr.toString()
    }

    private fun readLongMap(raw: String?): MutableMap<String, Long> {
        if (raw.isNullOrBlank()) return mutableMapOf()
        return runCatching {
            val obj = JSONObject(raw)
            val map = mutableMapOf<String, Long>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                map[k] = obj.optLong(k, 0L)
            }
            map
        }.getOrDefault(mutableMapOf())
    }

    private fun serializeLongMap(map: Map<String, Long>): String {
        val obj = JSONObject()
        for ((k, v) in map.entries.sortedByDescending { it.key }.take(60)) {
            obj.put(k, v)
        }
        return obj.toString()
    }

    private fun calculateStreakFromDailyMap(dailyMap: Map<String, Long>, nowCal: Calendar): Int {
        val cursor = (nowCal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayKey = dateKeyForMs(cursor.timeInMillis)
        if ((dailyMap[todayKey] ?: 0L) <= 0L) {
            cursor.add(Calendar.DATE, -1)
        }
        var streak = 0
        while (streak < 365) {
            val key = dateKeyForMs(cursor.timeInMillis)
            if ((dailyMap[key] ?: 0L) > 0L) {
                streak++
                cursor.add(Calendar.DATE, -1)
            } else {
                break
            }
        }
        return streak
    }

    private fun formatGoalLabel(goalMinutes: Long): String {
        val safe = goalMinutes.coerceAtLeast(1L)
        val hours = safe / 60L
        val mins = safe % 60L
        return when {
            hours > 0L && mins > 0L -> "${hours}h ${mins}m"
            hours > 0L -> "${hours}h"
            else -> "${mins}m"
        }
    }
}
