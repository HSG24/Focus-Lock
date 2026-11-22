package com.example.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Immutable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.Session
import com.example.calculateMaxStreak
import com.example.calculateStreak
import com.example.isSameDay
import com.example.isSameDayCalendar
import com.example.widget.NothingFocusWidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val Context.focusStatsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "focus_stats_datastore"
)

enum class StatsTimeframe(val label: String) {
    DAY("DAY"),
    WEEK("WEEK"),
    MONTH("MONTH")
}

enum class StatsSyncStatus {
    SYNCED,
    SYNCING,
    OFFLINE_CACHED
}

@Immutable
data class CachedSessionRecord(
    val id: String,
    val startTime: Long,
    val duration: Long
) {
    fun toSession(): Session = Session(startTime = startTime, duration = duration)
}

@Immutable
data class FocusStatsState(
    val sessions: List<Session> = emptyList(),
    val syncStatus: StatsSyncStatus = StatsSyncStatus.SYNCED,
    val pendingUploadCount: Int = 0,
    val lastSyncedAtMs: Long = 0L,
    val isInitialLoadComplete: Boolean = false
)

@Immutable
data class StatsChartBucket(
    val index: Int,
    val axisLabel: String,
    val tooltipTitle: String,
    val durationMs: Long,
    val sessionCount: Int,
    val isCurrentPeriod: Boolean = false
)

@Immutable
data class WeekStripDay(
    val dayLetter: String,
    val dayNumber: String,
    val calendarMillis: Long,
    val isSelected: Boolean,
    val isToday: Boolean,
    val isFuture: Boolean,
    val hasFocusData: Boolean
)

@Immutable
data class StatsPeriodReport(
    val timeframe: StatsTimeframe,
    val headerDateLabel: String,
    val totalFocusMs: Long,
    val sessionCount: Int,
    val avgSessionMs: Long,
    val longestSessionMs: Long,
    val currentStreak: Int,
    val maxStreak: Int,
    val goalTargetMs: Long,
    val dailyTargetMs: Long,
    val progressFraction: Float,
    val progressPercent: Int,
    val remainingToGoalMs: Long,
    val isGoalMet: Boolean,
    val canStepForward: Boolean,
    val isCurrentPeriod: Boolean,
    val chartBuckets: List<StatsChartBucket>,
    val weekStrip: List<WeekStripDay>
)

@Immutable
data class MonthClippedWeek(
    val startDay: Int,
    val endDay: Int,
    val startCal: Calendar,
    val endCal: Calendar,
    val endExclusiveCal: Calendar,
    val dayCount: Int
)

@Immutable
data class HeatmapDayCell(
    val calendarMillis: Long,
    val dateLabel: String,
    val dayOfMonth: Int,
    val durationMs: Long,
    val sessionCount: Int,
    val intensityLevel: Int,
    val isToday: Boolean,
    val isFuture: Boolean,
    val isGoalMet: Boolean
)

@Immutable
data class HeatmapWeekColumn(
    val monthLabel: String?,
    val days: List<HeatmapDayCell>
)

@Immutable
data class FocusHeatmapData(
    val weeks: List<HeatmapWeekColumn>,
    val activeDaysCount: Int,
    val goalMetDaysCount: Int,
    val totalFocusMs: Long,
    val currentStreak: Int,
    val maxStreak: Int,
    val windowWeeks: Int
)

object FocusStatsRepository {
    private const val PREFS_NAME = "focus_stats_cache"
    private const val KEY_CACHED_SESSIONS = "cached_sessions_json"
    private const val KEY_PENDING_SESSIONS = "pending_sessions_json"
    private const val KEY_DAILY_TOTALS = "daily_totals_json"
    private const val KEY_HOURLY_TOTALS = "hourly_totals_json"
    private const val KEY_CACHED_STREAK = "cached_current_streak"
    private const val KEY_CACHED_MAX_STREAK = "cached_max_streak"
    private const val KEY_LAST_SYNCED_MS = "last_synced_ms"

    private val DS_CACHED_SESSIONS = stringPreferencesKey(KEY_CACHED_SESSIONS)
    private val DS_PENDING_SESSIONS = stringPreferencesKey(KEY_PENDING_SESSIONS)
    private val DS_DAILY_TOTALS = stringPreferencesKey(KEY_DAILY_TOTALS)
    private val DS_HOURLY_TOTALS = stringPreferencesKey(KEY_HOURLY_TOTALS)
    private val DS_STREAK = intPreferencesKey(KEY_CACHED_STREAK)
    private val DS_MAX_STREAK = intPreferencesKey(KEY_CACHED_MAX_STREAK)

    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val cacheLock = Any()

    @Volatile
    private var appContextRef: Context? = null

    @Volatile
    private var _state: MutableStateFlow<FocusStatsState>? = null

    fun initialize(context: Context) {
        val appCtx = context.applicationContext
        appContextRef = appCtx
        ensureFlowHydrated(appCtx)
    }

    fun observe(context: Context): StateFlow<FocusStatsState> {
        val appCtx = context.applicationContext
        appContextRef = appCtx
        val flow = ensureFlowHydrated(appCtx)
        return flow.asStateFlow()
    }

    fun getCachedSessions(context: Context): List<Session> {
        val appCtx = context.applicationContext
        val prefs = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cached = parseRecords(prefs.getString(KEY_CACHED_SESSIONS, "[]"))
        val pending = parseRecords(prefs.getString(KEY_PENDING_SESSIONS, "[]"))
        return mergeSessionRecords(cached, pending).map { it.toSession() }
    }

    fun getAllSessionRecords(context: Context): List<CachedSessionRecord> {
        val appCtx = context.applicationContext
        val prefs = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cached = parseRecords(prefs.getString(KEY_CACHED_SESSIONS, "[]"))
        val pending = parseRecords(prefs.getString(KEY_PENDING_SESSIONS, "[]"))
        return mergeSessionRecords(cached, pending)
    }

    fun refreshNow(context: Context) {
        val appCtx = context.applicationContext
        appContextRef = appCtx
        synchronized(cacheLock) {
            val prefs = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val cached = parseRecords(prefs.getString(KEY_CACHED_SESSIONS, "[]"))
            val pending = parseRecords(prefs.getString(KEY_PENDING_SESSIONS, "[]"))
            val merged = mergeSessionRecords(cached, pending)
            if (pending.isNotEmpty()) {
                val now = System.currentTimeMillis()
                persistToPrefsAndMirrorAsync(
                    appCtx = appCtx,
                    prefs = prefs,
                    cached = merged,
                    pending = emptyList(),
                    merged = merged,
                    lastSyncedMs = now
                )
            }
            val lastUpdated = prefs.getLong(KEY_LAST_SYNCED_MS, System.currentTimeMillis())
            ensureFlowHydrated(appCtx).value = FocusStatsState(
                sessions = merged.map { it.toSession() },
                syncStatus = StatsSyncStatus.SYNCED,
                pendingUploadCount = 0,
                lastSyncedAtMs = lastUpdated,
                isInitialLoadComplete = true
            )
        }
    }

    suspend fun fetchAuthoritativeSessions(context: Context? = appContextRef): List<Session> {
        val ctx = context?.applicationContext ?: appContextRef ?: return emptyList()
        ensureFlowHydrated(ctx)
        return getCachedSessions(ctx)
    }

    fun recordCompletedSession(
        context: Context,
        startTimeMs: Long,
        durationMs: Long
    ) {
        if (startTimeMs <= 0L || durationMs <= 0L) return
        val appCtx = context.applicationContext
        appContextRef = appCtx
        ensureFlowHydrated(appCtx)

        val recordId = "loc_${startTimeMs}_${durationMs}"
        val newRecord = CachedSessionRecord(
            id = recordId,
            startTime = startTimeMs,
            duration = durationMs
        )

        synchronized(cacheLock) {
            val prefs = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val cached = parseRecords(prefs.getString(KEY_CACHED_SESSIONS, "[]"))
            val legacyPending = parseRecords(prefs.getString(KEY_PENDING_SESSIONS, "[]"))
            val mergedExisting = mergeSessionRecords(cached, legacyPending).toMutableList()
            if (mergedExisting.none { it.id == recordId || (it.startTime == startTimeMs && it.duration == durationMs) }) {
                mergedExisting.add(newRecord)
            }
            val sorted = mergedExisting.sortedBy { it.startTime }
            val now = System.currentTimeMillis()
            persistToPrefsAndMirrorAsync(
                appCtx = appCtx,
                prefs = prefs,
                cached = sorted,
                pending = emptyList(),
                merged = sorted,
                lastSyncedMs = now
            )
            emitState(sorted, now)
        }
        NothingFocusWidgetUpdater.scheduleUpdateAll(appCtx)
    }

    fun replaceAllSessions(
        context: Context,
        records: List<CachedSessionRecord>
    ) {
        val appCtx = context.applicationContext
        appContextRef = appCtx
        ensureFlowHydrated(appCtx)
        val clean = mergeSessionRecords(records, emptyList())
        val now = System.currentTimeMillis()
        synchronized(cacheLock) {
            val prefs = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            persistToPrefsAndMirrorAsync(
                appCtx = appCtx,
                prefs = prefs,
                cached = clean,
                pending = emptyList(),
                merged = clean,
                lastSyncedMs = now
            )
            emitState(clean, now)
        }
    }

    private fun ensureFlowHydrated(appCtx: Context): MutableStateFlow<FocusStatsState> {
        _state?.let { return it }
        synchronized(cacheLock) {
            _state?.let { return it }
            val prefs = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val cached = parseRecords(prefs.getString(KEY_CACHED_SESSIONS, "[]"))
            val legacyPending = parseRecords(prefs.getString(KEY_PENDING_SESSIONS, "[]"))
            val merged = mergeSessionRecords(cached, legacyPending)
            val lastUpdated = prefs.getLong(KEY_LAST_SYNCED_MS, System.currentTimeMillis())
            if (legacyPending.isNotEmpty()) {
                persistToPrefsAndMirrorAsync(
                    appCtx = appCtx,
                    prefs = prefs,
                    cached = merged,
                    pending = emptyList(),
                    merged = merged,
                    lastSyncedMs = lastUpdated
                )
            }
            val initial = FocusStatsState(
                sessions = merged.map { it.toSession() },
                syncStatus = StatsSyncStatus.SYNCED,
                pendingUploadCount = 0,
                lastSyncedAtMs = lastUpdated,
                isInitialLoadComplete = true
            )
            val flow = MutableStateFlow(initial)
            _state = flow
            return flow
        }
    }

    private fun emitState(
        merged: List<CachedSessionRecord>,
        lastUpdatedMs: Long
    ) {
        val flow = _state ?: return
        flow.value = FocusStatsState(
            sessions = merged.map { it.toSession() },
            syncStatus = StatsSyncStatus.SYNCED,
            pendingUploadCount = 0,
            lastSyncedAtMs = lastUpdatedMs,
            isInitialLoadComplete = true
        )
    }

    private fun persistToPrefsAndMirrorAsync(
        appCtx: Context,
        prefs: SharedPreferences,
        cached: List<CachedSessionRecord>,
        pending: List<CachedSessionRecord>,
        merged: List<CachedSessionRecord>,
        lastSyncedMs: Long
    ) {
        val cachedJson = serializeRecords(cached)
        val pendingJson = serializeRecords(pending)
        val mergedSessions = merged.map { it.toSession() }
        val dailyTotalsJson = serializeDailyTotals(mergedSessions)
        val hourlyTotalsJson = serializeHourlyTotals(mergedSessions)
        val currentStreak = calculateStreak(mergedSessions)
        val maxStreak = calculateMaxStreak(mergedSessions)

        prefs.edit()
            .putString(KEY_CACHED_SESSIONS, cachedJson)
            .putString(KEY_PENDING_SESSIONS, pendingJson)
            .putString(KEY_DAILY_TOTALS, dailyTotalsJson)
            .putString(KEY_HOURLY_TOTALS, hourlyTotalsJson)
            .putInt(KEY_CACHED_STREAK, currentStreak)
            .putInt(KEY_CACHED_MAX_STREAK, maxStreak)
            .putLong(KEY_LAST_SYNCED_MS, lastSyncedMs)
            .apply()

        repoScope.launch {
            runCatching {
                appCtx.focusStatsDataStore.edit { ds ->
                    ds[DS_CACHED_SESSIONS] = cachedJson
                    ds[DS_PENDING_SESSIONS] = pendingJson
                    ds[DS_DAILY_TOTALS] = dailyTotalsJson
                    ds[DS_HOURLY_TOTALS] = hourlyTotalsJson
                    ds[DS_STREAK] = currentStreak
                    ds[DS_MAX_STREAK] = maxStreak
                }
            }
        }
    }

    private fun mergeSessionRecords(
        cached: List<CachedSessionRecord>,
        pending: List<CachedSessionRecord>
    ): List<CachedSessionRecord> {
        val byKey = LinkedHashMap<String, CachedSessionRecord>(cached.size + pending.size)
        val seenPairs = HashSet<Pair<Long, Long>>(cached.size + pending.size)
        for (item in cached) {
            if (item.startTime > 0L && item.duration > 0L) {
                if (seenPairs.add(item.startTime to item.duration)) {
                    byKey[item.id] = item
                }
            }
        }
        for (item in pending) {
            if (item.startTime > 0L && item.duration > 0L) {
                if (!byKey.containsKey(item.id) && seenPairs.add(item.startTime to item.duration)) {
                    byKey[item.id] = item
                }
            }
        }
        return byKey.values.sortedBy { it.startTime }
    }

    private fun parseRecords(raw: String?): List<CachedSessionRecord> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList(arr.length()) {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val id = obj.optString("id")
                    val startTime = obj.optLong("startTime", 0L)
                    val duration = obj.optLong("duration", 0L)
                    if (id.isNotBlank() && startTime > 0L && duration > 0L) {
                        add(CachedSessionRecord(id = id, startTime = startTime, duration = duration))
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun serializeRecords(list: List<CachedSessionRecord>): String {
        val arr = JSONArray()
        for (item in list) {
            arr.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("startTime", item.startTime)
                    put("duration", item.duration)
                }
            )
        }
        return arr.toString()
    }

    private fun serializeDailyTotals(sessions: List<Session>): String {
        val grouped = sessions.groupBy { FocusXpRepository.dateKeyForMs(it.startTime) }
            .mapValues { (_, items) -> items.sumOf { it.duration } }
        val obj = JSONObject()
        for ((k, v) in grouped) {
            obj.put(k, v)
        }
        return obj.toString()
    }

    private fun serializeHourlyTotals(sessions: List<Session>): String {
        val grouped = HashMap<String, Long>()
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd_HH", Locale.US)
        for (s in sessions) {
            cal.timeInMillis = s.startTime
            val key = sdf.format(Date(cal.timeInMillis))
            grouped[key] = (grouped[key] ?: 0L) + s.duration
        }
        val obj = JSONObject()
        for ((k, v) in grouped) {
            obj.put(k, v)
        }
        return obj.toString()
    }

    // -------------------------------------------------------------------------
    // Period Aggregation & Infographic Chart Bucketing (Day 24h / Week / Month)
    // -------------------------------------------------------------------------

    fun buildPeriodReport(
        sessions: List<Session>,
        timeframe: StatsTimeframe,
        selectedDate: Calendar,
        dailyGoalMinutes: Long,
        nowCal: Calendar = Calendar.getInstance()
    ): StatsPeriodReport {
        val safeGoalMinutes = dailyGoalMinutes.coerceAtLeast(1L)
        val dailyTargetMs = safeGoalMinutes * 60_000L
        val currentStreak = calculateStreak(sessions)
        val maxStreak = calculateMaxStreak(sessions)

        val todayStart = startOfDayCalendar(nowCal)
        val weekStrip = buildWeekStrip(sessions, selectedDate, todayStart)

        return when (timeframe) {
            StatsTimeframe.DAY -> buildDayReport(
                sessions = sessions,
                selectedDate = selectedDate,
                todayStart = todayStart,
                dailyTargetMs = dailyTargetMs,
                currentStreak = currentStreak,
                maxStreak = maxStreak,
                weekStrip = weekStrip
            )
            StatsTimeframe.WEEK -> buildWeekReport(
                sessions = sessions,
                selectedDate = selectedDate,
                todayStart = todayStart,
                dailyTargetMs = dailyTargetMs,
                currentStreak = currentStreak,
                maxStreak = maxStreak,
                weekStrip = weekStrip
            )
            StatsTimeframe.MONTH -> buildMonthReport(
                sessions = sessions,
                selectedDate = selectedDate,
                todayStart = todayStart,
                dailyTargetMs = dailyTargetMs,
                currentStreak = currentStreak,
                maxStreak = maxStreak,
                weekStrip = weekStrip
            )
        }
    }

    private fun buildDayReport(
        sessions: List<Session>,
        selectedDate: Calendar,
        todayStart: Calendar,
        dailyTargetMs: Long,
        currentStreak: Int,
        maxStreak: Int,
        weekStrip: List<WeekStripDay>
    ): StatsPeriodReport {
        val dayStart = startOfDayCalendar(selectedDate)
        val dayEndMs = dayStart.timeInMillis + 24L * 60L * 60L * 1000L
        val daySessions = sessions.filter { isSameDay(it.startTime, dayStart) }
        val totalMs = daySessions.sumOf { it.duration }
        val count = daySessions.size
        val avgMs = if (count > 0) totalMs / count else 0L
        val longestMs = daySessions.maxOfOrNull { it.duration } ?: 0L

        val hourDurations = LongArray(24)
        val hourCounts = IntArray(24)
        val cal = Calendar.getInstance()

        for (session in daySessions) {
            cal.timeInMillis = session.startTime
            val startHour = cal.get(Calendar.HOUR_OF_DAY).coerceIn(0, 23)
            hourCounts[startHour]++

            var cursorMs = session.startTime
            var remainingMs = session.duration.coerceAtLeast(0L)
            while (remainingMs > 0L && cursorMs < dayEndMs) {
                cal.timeInMillis = cursorMs
                val hour = cal.get(Calendar.HOUR_OF_DAY).coerceIn(0, 23)
                val nextHourCal = (cal.clone() as Calendar).apply {
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    add(Calendar.HOUR_OF_DAY, 1)
                }
                val sliceMax = (nextHourCal.timeInMillis - cursorMs).coerceAtLeast(1L)
                val slice = minOf(remainingMs, sliceMax)
                hourDurations[hour] += slice
                cursorMs += slice
                remainingMs -= slice
            }
        }

        val currentHour = if (isSameDayCalendar(dayStart, todayStart)) {
            Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        } else {
            -1
        }

        val buckets = (0 until 24).map { hour ->
            val axisLabel = when (hour) {
                0 -> "12AM"
                6 -> "6AM"
                12 -> "12PM"
                18 -> "6PM"
                23 -> "12AM"
                else -> ""
            }
            val startLabel = formatHourCompact(hour)
            val endLabel = formatHourCompact((hour + 1) % 24)
            StatsChartBucket(
                index = hour,
                axisLabel = axisLabel,
                tooltipTitle = "$startLabel – $endLabel",
                durationMs = hourDurations[hour],
                sessionCount = hourCounts[hour],
                isCurrentPeriod = hour == currentHour
            )
        }

        val yesterdayStart = (todayStart.clone() as Calendar).apply { add(Calendar.DATE, -1) }
        val dateText = SimpleDateFormat("EEE, MMM d", Locale.US).format(dayStart.time)
            .uppercase(Locale.US)
        val headerLabel = when {
            isSameDayCalendar(dayStart, todayStart) -> "TODAY • $dateText"
            isSameDayCalendar(dayStart, yesterdayStart) -> "YESTERDAY • $dateText"
            else -> SimpleDateFormat("EEE, MMM d, yyyy", Locale.US).format(dayStart.time)
                .uppercase(Locale.US)
        }

        val progressFraction = (totalMs.toFloat() / dailyTargetMs.toFloat()).coerceIn(0f, 1f)
        val progressPercent = ((totalMs.toFloat() / dailyTargetMs.toFloat()) * 100f)
            .toInt()
            .coerceIn(0, 999)
        val remainingMs = (dailyTargetMs - totalMs).coerceAtLeast(0L)
        val isToday = isSameDayCalendar(dayStart, todayStart)

        return StatsPeriodReport(
            timeframe = StatsTimeframe.DAY,
            headerDateLabel = headerLabel,
            totalFocusMs = totalMs,
            sessionCount = count,
            avgSessionMs = avgMs,
            longestSessionMs = longestMs,
            currentStreak = currentStreak,
            maxStreak = maxStreak,
            goalTargetMs = dailyTargetMs,
            dailyTargetMs = dailyTargetMs,
            progressFraction = progressFraction,
            progressPercent = progressPercent,
            remainingToGoalMs = remainingMs,
            isGoalMet = totalMs >= dailyTargetMs,
            canStepForward = dayStart.before(todayStart),
            isCurrentPeriod = isToday,
            chartBuckets = buckets,
            weekStrip = weekStrip
        )
    }

    private fun buildWeekReport(
        sessions: List<Session>,
        selectedDate: Calendar,
        todayStart: Calendar,
        dailyTargetMs: Long,
        currentStreak: Int,
        maxStreak: Int,
        weekStrip: List<WeekStripDay>
    ): StatsPeriodReport {
        val slice = findMonthClippedWeek(selectedDate)
        val currentSlice = findMonthClippedWeek(todayStart)

        val weekSessions = sessions.filter {
            it.startTime >= slice.startCal.timeInMillis && it.startTime < slice.endExclusiveCal.timeInMillis
        }
        val totalMs = weekSessions.sumOf { it.duration }
        val count = weekSessions.size
        val avgMs = if (count > 0) totalMs / count else 0L
        val longestMs = weekSessions.maxOfOrNull { it.duration } ?: 0L

        val dayLabelFmt = SimpleDateFormat("EEE", Locale.US)
        val tooltipDateFmt = SimpleDateFormat("EEE, MMM d", Locale.US)
        val buckets = (0 until slice.dayCount).mapNotNull { offset ->
            val dayCal = (slice.startCal.clone() as Calendar).apply { add(Calendar.DATE, offset) }
            if (dayCal.after(todayStart)) {
                null
            } else {
                val daySessions = weekSessions.filter { isSameDay(it.startTime, dayCal) }
                val dayMs = daySessions.sumOf { it.duration }
                StatsChartBucket(
                    index = offset,
                    axisLabel = "${dayLabelFmt.format(dayCal.time).take(3)}\n${dayCal.get(Calendar.DAY_OF_MONTH)}",
                    tooltipTitle = tooltipDateFmt.format(dayCal.time),
                    durationMs = dayMs,
                    sessionCount = daySessions.size,
                    isCurrentPeriod = isSameDayCalendar(dayCal, todayStart)
                )
            }
        }.mapIndexed { idx, bucket -> bucket.copy(index = idx) }

        val isCurrentWeek = isSameDayCalendar(slice.startCal, currentSlice.startCal)
        val headerLabel = if (isCurrentWeek) {
            "CURRENT WEEK"
        } else if (slice.startDay == slice.endDay) {
            SimpleDateFormat("MMM d, yyyy", Locale.US).format(slice.startCal.time).uppercase(Locale.US)
        } else {
            val startStr = SimpleDateFormat("MMM d", Locale.US).format(slice.startCal.time).uppercase(Locale.US)
            val endStr = SimpleDateFormat("MMM d, yyyy", Locale.US).format(slice.endCal.time).uppercase(Locale.US)
            "$startStr – $endStr"
        }

        val weekTargetMs = dailyTargetMs * slice.dayCount.toLong()
        val progressFraction = (totalMs.toFloat() / weekTargetMs.toFloat()).coerceIn(0f, 1f)
        val progressPercent = ((totalMs.toFloat() / weekTargetMs.toFloat()) * 100f)
            .toInt()
            .coerceIn(0, 999)
        val remainingMs = (weekTargetMs - totalMs).coerceAtLeast(0L)

        return StatsPeriodReport(
            timeframe = StatsTimeframe.WEEK,
            headerDateLabel = headerLabel,
            totalFocusMs = totalMs,
            sessionCount = count,
            avgSessionMs = avgMs,
            longestSessionMs = longestMs,
            currentStreak = currentStreak,
            maxStreak = maxStreak,
            goalTargetMs = weekTargetMs,
            dailyTargetMs = dailyTargetMs,
            progressFraction = progressFraction,
            progressPercent = progressPercent,
            remainingToGoalMs = remainingMs,
            isGoalMet = totalMs >= weekTargetMs,
            canStepForward = slice.startCal.before(currentSlice.startCal),
            isCurrentPeriod = isCurrentWeek,
            chartBuckets = buckets,
            weekStrip = weekStrip
        )
    }

    private fun buildMonthReport(
        sessions: List<Session>,
        selectedDate: Calendar,
        todayStart: Calendar,
        dailyTargetMs: Long,
        currentStreak: Int,
        maxStreak: Int,
        weekStrip: List<WeekStripDay>
    ): StatsPeriodReport {
        val monthStart = startOfMonthCalendar(selectedDate)
        val nextMonthStart = (monthStart.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
        val currentMonthStart = startOfMonthCalendar(todayStart)
        val daysInMonth = monthStart.getActualMaximum(Calendar.DAY_OF_MONTH)

        val monthSessions = sessions.filter {
            it.startTime >= monthStart.timeInMillis && it.startTime < nextMonthStart.timeInMillis
        }
        val totalMs = monthSessions.sumOf { it.duration }
        val count = monthSessions.size
        val avgMs = if (count > 0) totalMs / count else 0L
        val longestMs = monthSessions.maxOfOrNull { it.duration } ?: 0L

        val tooltipDateFmt = SimpleDateFormat("EEE, MMM d", Locale.US)
        val buckets = (1..daysInMonth).mapNotNull { dayOfMonth ->
            val dayCal = (monthStart.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, dayOfMonth)
            }
            if (dayCal.after(todayStart)) {
                null
            } else {
                val daySessions = monthSessions.filter { isSameDay(it.startTime, dayCal) }
                val dayMs = daySessions.sumOf { it.duration }
                StatsChartBucket(
                    index = dayOfMonth - 1,
                    axisLabel = "$dayOfMonth",
                    tooltipTitle = tooltipDateFmt.format(dayCal.time),
                    durationMs = dayMs,
                    sessionCount = daySessions.size,
                    isCurrentPeriod = isSameDayCalendar(dayCal, todayStart)
                )
            }
        }

        val headerLabel = SimpleDateFormat("MMMM yyyy", Locale.US).format(monthStart.time)
            .uppercase(Locale.US)
        val monthTargetMs = dailyTargetMs * daysInMonth.toLong()
        val progressFraction = (totalMs.toFloat() / monthTargetMs.toFloat()).coerceIn(0f, 1f)
        val progressPercent = ((totalMs.toFloat() / monthTargetMs.toFloat()) * 100f)
            .toInt()
            .coerceIn(0, 999)
        val remainingMs = (monthTargetMs - totalMs).coerceAtLeast(0L)

        return StatsPeriodReport(
            timeframe = StatsTimeframe.MONTH,
            headerDateLabel = headerLabel,
            totalFocusMs = totalMs,
            sessionCount = count,
            avgSessionMs = avgMs,
            longestSessionMs = longestMs,
            currentStreak = currentStreak,
            maxStreak = maxStreak,
            goalTargetMs = monthTargetMs,
            dailyTargetMs = dailyTargetMs,
            progressFraction = progressFraction,
            progressPercent = progressPercent,
            remainingToGoalMs = remainingMs,
            isGoalMet = totalMs >= monthTargetMs,
            canStepForward = monthStart.before(currentMonthStart),
            isCurrentPeriod = isSameDayCalendar(monthStart, currentMonthStart),
            chartBuckets = buckets,
            weekStrip = weekStrip
        )
    }

    private fun buildWeekStrip(
        sessions: List<Session>,
        selectedDate: Calendar,
        todayStart: Calendar
    ): List<WeekStripDay> {
        val monday = startOfMondayWeekCalendar(selectedDate)
        val letters = listOf("M", "T", "W", "T", "F", "S", "S")
        return (0 until 7).map { offset ->
            val dayCal = (monday.clone() as Calendar).apply { add(Calendar.DATE, offset) }
            val hasFocus = sessions.any { isSameDay(it.startTime, dayCal) }
            WeekStripDay(
                dayLetter = letters[offset],
                dayNumber = dayCal.get(Calendar.DAY_OF_MONTH).toString(),
                calendarMillis = dayCal.timeInMillis,
                isSelected = isSameDayCalendar(dayCal, selectedDate),
                isToday = isSameDayCalendar(dayCal, todayStart),
                isFuture = dayCal.after(todayStart) && !isSameDayCalendar(dayCal, todayStart),
                hasFocusData = hasFocus
            )
        }
    }

    fun startOfDayCalendar(source: Calendar): Calendar {
        return (source.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    fun startOfMondayWeekCalendar(source: Calendar): Calendar {
        val cal = startOfDayCalendar(source)
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        val daysFromMonday = when (dow) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }
        cal.add(Calendar.DATE, -daysFromMonday)
        return cal
    }

    fun startOfMonthCalendar(source: Calendar): Calendar {
        return startOfDayCalendar(source).apply {
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }

    fun getMonthClippedWeeks(anchor: Calendar): List<MonthClippedWeek> {
        val monthStart = startOfMonthCalendar(anchor)
        val daysInMonth = monthStart.getActualMaximum(Calendar.DAY_OF_MONTH)
        val result = mutableListOf<MonthClippedWeek>()
        var cursorDay = 1
        while (cursorDay <= daysInMonth) {
            val startCal = (monthStart.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, cursorDay)
            }
            val daysUntilSunday = when (startCal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> 6
                Calendar.TUESDAY -> 5
                Calendar.WEDNESDAY -> 4
                Calendar.THURSDAY -> 3
                Calendar.FRIDAY -> 2
                Calendar.SATURDAY -> 1
                Calendar.SUNDAY -> 0
                else -> 0
            }
            val endDay = minOf(cursorDay + daysUntilSunday, daysInMonth)
            val endCal = (monthStart.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, endDay)
            }
            val endExclusiveCal = (endCal.clone() as Calendar).apply {
                add(Calendar.DATE, 1)
            }
            result.add(
                MonthClippedWeek(
                    startDay = cursorDay,
                    endDay = endDay,
                    startCal = startCal,
                    endCal = endCal,
                    endExclusiveCal = endExclusiveCal,
                    dayCount = (endDay - cursorDay + 1)
                )
            )
            cursorDay = endDay + 1
        }
        return result
    }

    fun findMonthClippedWeek(date: Calendar): MonthClippedWeek {
        val dayOfMonth = date.get(Calendar.DAY_OF_MONTH)
        val weeks = getMonthClippedWeeks(date)
        return weeks.firstOrNull { dayOfMonth in it.startDay..it.endDay } ?: weeks.first()
    }

    fun stepMonthClippedWeekBackward(selectedDate: Calendar): Calendar {
        val slice = findMonthClippedWeek(selectedDate)
        return (slice.startCal.clone() as Calendar).apply { add(Calendar.DATE, -1) }
    }

    fun stepMonthClippedWeekForward(
        selectedDate: Calendar,
        nowCal: Calendar = Calendar.getInstance()
    ): Calendar {
        val todayStart = startOfDayCalendar(nowCal)
        val slice = findMonthClippedWeek(selectedDate)
        val candidate = (slice.endCal.clone() as Calendar).apply { add(Calendar.DATE, 1) }
        return if (candidate.after(todayStart)) todayStart else candidate
    }

    private fun formatHourCompact(hour24: Int): String {
        return when {
            hour24 == 0 -> "12 AM"
            hour24 < 12 -> "$hour24 AM"
            hour24 == 12 -> "12 PM"
            else -> "${hour24 - 12} PM"
        }
    }

    fun formatDurationHero(ms: Long): String {
        if (ms <= 0L) return "0s"
        val totalSeconds = ms / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }

    fun formatDurationShort(ms: Long): String {
        if (ms <= 0L) return "0m"
        val totalMinutes = ms / 60_000L
        val hours = totalMinutes / 60L
        val remMins = totalMinutes % 60L
        return when {
            hours > 0 && remMins > 0 -> "${hours}h ${remMins}m"
            hours > 0 -> "${hours}h"
            totalMinutes > 0 -> "${totalMinutes}m"
            else -> "${(ms / 1000L).coerceAtLeast(1L)}s"
        }
    }

    /**
     * Builds a Monday-aligned rolling [weeksCount]-week focus consistency heatmap ending
     * on the current week, scaling each day's intensity (0..4) against [dailyGoalMinutes].
     */
    fun buildHeatmapData(
        sessions: List<Session>,
        dailyGoalMinutes: Long,
        weeksCount: Int = 14,
        nowCal: Calendar = Calendar.getInstance()
    ): FocusHeatmapData {
        val safeWeeks = weeksCount.coerceIn(4, 26)
        val dailyTargetMs = dailyGoalMinutes.coerceAtLeast(1L) * 60_000L
        val todayStart = startOfDayCalendar(nowCal)
        val currentWeekMonday = startOfMondayWeekCalendar(todayStart)
        val startMonday = (currentWeekMonday.clone() as Calendar).apply {
            add(Calendar.DATE, -((safeWeeks - 1) * 7))
        }

        val dayTotals = HashMap<String, Long>()
        val dayCounts = HashMap<String, Int>()
        val keyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dateLabelFmt = SimpleDateFormat("EEE, MMM d", Locale.US)
        val monthFmt = SimpleDateFormat("MMM", Locale.US)

        for (session in sessions) {
            if (session.startTime <= 0L || session.duration <= 0L) continue
            val key = keyFmt.format(Date(session.startTime))
            dayTotals[key] = (dayTotals[key] ?: 0L) + session.duration
            dayCounts[key] = (dayCounts[key] ?: 0) + 1
        }

        val weeks = ArrayList<HeatmapWeekColumn>(safeWeeks)
        var activeDays = 0
        var goalMetDays = 0
        var totalWindowMs = 0L
        var lastLabeledMonth = -1

        for (weekIdx in 0 until safeWeeks) {
            val weekStart = (startMonday.clone() as Calendar).apply {
                add(Calendar.DATE, weekIdx * 7)
            }
            var monthLabel: String? = null
            val days = ArrayList<HeatmapDayCell>(7)

            for (dayOffset in 0 until 7) {
                val dayCal = (weekStart.clone() as Calendar).apply {
                    add(Calendar.DATE, dayOffset)
                }
                val dayMonth = dayCal.get(Calendar.MONTH)
                val dayOfMonth = dayCal.get(Calendar.DAY_OF_MONTH)
                if (monthLabel == null) {
                    if (weekIdx == 0 && dayOffset == 0) {
                        monthLabel = monthFmt.format(dayCal.time)
                        lastLabeledMonth = dayMonth
                    } else if (dayOfMonth == 1 && dayMonth != lastLabeledMonth) {
                        monthLabel = monthFmt.format(dayCal.time)
                        lastLabeledMonth = dayMonth
                    }
                }

                val isToday = isSameDayCalendar(dayCal, todayStart)
                val isFuture = dayCal.after(todayStart) && !isToday
                val key = keyFmt.format(dayCal.time)
                val durationMs = if (isFuture) 0L else (dayTotals[key] ?: 0L)
                val count = if (isFuture) 0 else (dayCounts[key] ?: 0)
                val ratio = if (durationMs > 0L) {
                    durationMs.toFloat() / dailyTargetMs.toFloat()
                } else {
                    0f
                }
                val isGoalMet = !isFuture && durationMs >= dailyTargetMs
                val level = when {
                    isFuture || durationMs <= 0L -> 0
                    isGoalMet -> 4
                    ratio >= 0.70f -> 3
                    ratio >= 0.35f -> 2
                    else -> 1
                }

                if (!isFuture && durationMs > 0L) {
                    activeDays++
                    totalWindowMs += durationMs
                }
                if (isGoalMet) {
                    goalMetDays++
                }

                days.add(
                    HeatmapDayCell(
                        calendarMillis = dayCal.timeInMillis,
                        dateLabel = dateLabelFmt.format(dayCal.time),
                        dayOfMonth = dayOfMonth,
                        durationMs = durationMs,
                        sessionCount = count,
                        intensityLevel = level,
                        isToday = isToday,
                        isFuture = isFuture,
                        isGoalMet = isGoalMet
                    )
                )
            }

            weeks.add(
                HeatmapWeekColumn(
                    monthLabel = monthLabel,
                    days = days
                )
            )
        }

        return FocusHeatmapData(
            weeks = weeks,
            activeDaysCount = activeDays,
            goalMetDaysCount = goalMetDays,
            totalFocusMs = totalWindowMs,
            currentStreak = calculateStreak(sessions),
            maxStreak = calculateMaxStreak(sessions),
            windowWeeks = safeWeeks
        )
    }
}
