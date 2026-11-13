package com.example.data

import android.content.Context
import com.example.LockService
import com.example.Session
import com.example.fetchAllSessions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class ShareMode {
    LIVE,
    RANGE
}

enum class RangePreset(val label: String) {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    LAST_7_DAYS("Last 7 Days"),
    LAST_30_DAYS("Last 30 Days")
}

data class SharedStatsSummary(
    val totalFocusMs: Long,
    val sessionCount: Int,
    val averageMs: Long,
    val longestMs: Long
)

data class ShareLinkResult(
    val shareId: String,
    val url: String,
    val mode: ShareMode,
    val startDateMs: Long,
    val endDateMs: Long?,
    val stats: SharedStatsSummary
)

data class SharedLinkItem(
    val shareId: String,
    val url: String,
    val mode: ShareMode,
    val status: String,
    val startDateMs: Long,
    val endDateMs: Long?,
    val dateLabel: String,
    val createdAt: Long,
    val updatedAt: Long,
    val stats: SharedStatsSummary
)

object ShareRepository {
    private const val RTDB_BASE_URL =
        "https://focuslock-b6c0f-default-rtdb.asia-southeast1.firebasedatabase.app"
    const val WEB_VIEWER_BASE_URL = "https://focuslock-b6c0f.web.app"

    private val client = OkHttpClient()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val shareIdChars = "abcdefghijkmnopqrstuvwxyz23456789"
    private val random = SecureRandom()

    fun buildShareUrl(shareId: String): String = "$WEB_VIEWER_BASE_URL/?id=$shareId"

    fun generateShareId(length: Int = 10): String {
        val sb = StringBuilder(length)
        repeat(length) {
            sb.append(shareIdChars[random.nextInt(shareIdChars.length)])
        }
        return sb.toString()
    }

    fun startOfDayMs(timeMs: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timeMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun endOfDayMs(timeMs: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timeMs
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return cal.timeInMillis
    }

    /**
     * Converts a UTC midnight timestamp returned by Material 3 DatePicker/DateRangePicker
     * into the corresponding local calendar date's start-of-day timestamp.
     */
    fun utcDatePickerMillisToLocalStartOfDay(utcMillis: Long): Long {
        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = utcMillis
        }
        val localCal = Calendar.getInstance().apply {
            clear()
            set(
                utcCal.get(Calendar.YEAR),
                utcCal.get(Calendar.MONTH),
                utcCal.get(Calendar.DAY_OF_MONTH),
                0,
                0,
                0
            )
            set(Calendar.MILLISECOND, 0)
        }
        return localCal.timeInMillis
    }

    fun localStartOfDayToUtcPickerMillis(localMs: Long): Long {
        val localCal = Calendar.getInstance().apply { timeInMillis = localMs }
        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(
                localCal.get(Calendar.YEAR),
                localCal.get(Calendar.MONTH),
                localCal.get(Calendar.DAY_OF_MONTH),
                0,
                0,
                0
            )
            set(Calendar.MILLISECOND, 0)
        }
        return utcCal.timeInMillis
    }

    fun dateKeyForMs(timeMs: Long): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(timeMs))
    }

    fun computeStatsForRange(
        sessions: List<Session>,
        startDateMs: Long,
        endDateMs: Long?
    ): SharedStatsSummary {
        val normalizedStart = startOfDayMs(startDateMs)
        val normalizedEnd = endDateMs?.let { endOfDayMs(it) }
        val filtered = sessions.filter { session ->
            session.startTime >= normalizedStart &&
                (normalizedEnd == null || session.startTime <= normalizedEnd)
        }
        val totalFocusMs = filtered.sumOf { it.duration }
        val count = filtered.size
        val averageMs = if (count > 0) totalFocusMs / count else 0L
        val longestMs = if (count > 0) filtered.maxOf { it.duration } else 0L
        return SharedStatsSummary(
            totalFocusMs = totalFocusMs,
            sessionCount = count,
            averageMs = averageMs,
            longestMs = longestMs
        )
    }

    fun computeDailyStatsForRange(
        sessions: List<Session>,
        startDateMs: Long,
        endDateMs: Long?
    ): Map<String, SharedStatsSummary> {
        val normalizedStart = startOfDayMs(startDateMs)
        val normalizedEnd = endDateMs?.let { endOfDayMs(it) }
        val filtered = sessions.filter { session ->
            session.startTime >= normalizedStart &&
                (normalizedEnd == null || session.startTime <= normalizedEnd)
        }
        return filtered
            .groupBy { dateKeyForMs(it.startTime) }
            .mapValues { (_, daySessions) ->
                val total = daySessions.sumOf { it.duration }
                val count = daySessions.size
                val avg = if (count > 0) total / count else 0L
                val longest = if (count > 0) daySessions.maxOf { it.duration } else 0L
                SharedStatsSummary(
                    totalFocusMs = total,
                    sessionCount = count,
                    averageMs = avg,
                    longestMs = longest
                )
            }
    }

    fun formatDurationReadable(ms: Long): String {
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

    fun formatDurationCompact(ms: Long): String {
        if (ms <= 0L) return "0s"
        val minutes = ms / (1000L * 60L)
        val hours = minutes / 60L
        val remMinutes = minutes % 60L
        val seconds = (ms % (1000L * 60L)) / 1000L
        return when {
            hours > 0 -> "${hours}h ${remMinutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "${seconds}s"
        }
    }

    fun formatSingleDate(ms: Long): String {
        val todayStart = startOfDayMs(System.currentTimeMillis())
        val yesterdayStart = startOfDayMs(
            Calendar.getInstance().apply { add(Calendar.DATE, -1) }.timeInMillis
        )
        val targetStart = startOfDayMs(ms)
        return when (targetStart) {
            todayStart -> "Today"
            yesterdayStart -> "Yesterday"
            else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(ms))
        }
    }

    fun formatDateRangeLabel(startMs: Long, endMs: Long?): String {
        val startFmt = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(startMs))
        if (endMs == null) {
            return "From $startFmt · Live"
        }
        val startDay = startOfDayMs(startMs)
        val endDay = startOfDayMs(endMs)
        if (startDay == endDay) {
            return formatSingleDate(startMs)
        }
        val startCal = Calendar.getInstance().apply { timeInMillis = startMs }
        val endCal = Calendar.getInstance().apply { timeInMillis = endMs }
        val sameYear = startCal.get(Calendar.YEAR) == endCal.get(Calendar.YEAR)
        val startShort = if (sameYear) {
            SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(startMs))
        } else {
            startFmt
        }
        val endFull = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(endMs))
        return "$startShort – $endFull"
    }

    fun formatCreatedTimestamp(ms: Long): String {
        if (ms <= 0L) return "Unknown date"
        return SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault()).format(Date(ms))
    }

    suspend fun createRangeShare(
        sessions: List<Session>,
        startDateMs: Long,
        endDateMs: Long
    ): Result<ShareLinkResult> = withContext(Dispatchers.IO) {
        runCatching {
            val normalizedStart = startOfDayMs(startDateMs)
            val normalizedEnd = endOfDayMs(endDateMs)
            val summary = computeStatsForRange(sessions, normalizedStart, normalizedEnd)
            val dailyStats = computeDailyStatsForRange(sessions, normalizedStart, normalizedEnd)
            val shareId = generateShareId()
            val now = System.currentTimeMillis()

            val payload = JSONObject().apply {
                put("id", shareId)
                put("mode", "range")
                put("status", "snapshot")
                put("startDateMs", normalizedStart)
                put("endDateMs", normalizedEnd)
                put("startDateKey", dateKeyForMs(normalizedStart))
                put("endDateKey", dateKeyForMs(normalizedEnd))
                put("dateLabel", formatDateRangeLabel(normalizedStart, normalizedEnd))
                put("createdAt", now)
                put("updatedAt", now)
                put("stats", statsToJson(summary))
                put("dailyStats", dailyStatsToJson(dailyStats))
                put(
                    "activeSession",
                    JSONObject().apply {
                        put("isActive", false)
                        put("startTime", 0L)
                        put("dateKey", "")
                    }
                )
            }

            putJson("$RTDB_BASE_URL/shared_stats/$shareId.json", payload)

            ShareLinkResult(
                shareId = shareId,
                url = buildShareUrl(shareId),
                mode = ShareMode.RANGE,
                startDateMs = normalizedStart,
                endDateMs = normalizedEnd,
                stats = summary
            )
        }
    }

    suspend fun createLiveShare(
        context: Context,
        sessions: List<Session>,
        startDateMs: Long
    ): Result<ShareLinkResult> = withContext(Dispatchers.IO) {
        runCatching {
            // Stop any previously active live share cleanly before creating a new one
            val previousId = FocusSettingsRepository.getCurrent(context).activeLiveShareId
            if (!previousId.isNullOrBlank()) {
                runCatching {
                    val stopNow = System.currentTimeMillis()
                    val stopPayload = JSONObject().apply {
                        put("status", "stopped")
                        put("endDateMs", stopNow)
                        put("endDateKey", dateKeyForMs(stopNow))
                        put("updatedAt", stopNow)
                        put(
                            "activeSession",
                            JSONObject().apply {
                                put("isActive", false)
                                put("startTime", 0L)
                                put("dateKey", "")
                            }
                        )
                    }
                    patchJson("$RTDB_BASE_URL/shared_stats/$previousId.json", stopPayload)
                }
            }

            val normalizedStart = startOfDayMs(startDateMs)
            val summary = computeStatsForRange(sessions, normalizedStart, null)
            val dailyStats = computeDailyStatsForRange(sessions, normalizedStart, null)
            val shareId = generateShareId()
            val now = System.currentTimeMillis()
            val activeStart = LockService.currentSessionStartTimeMs

            val payload = JSONObject().apply {
                put("id", shareId)
                put("mode", "live")
                put("status", "active")
                put("startDateMs", normalizedStart)
                put("endDateMs", JSONObject.NULL)
                put("startDateKey", dateKeyForMs(normalizedStart))
                put("endDateKey", JSONObject.NULL)
                put("dateLabel", formatDateRangeLabel(normalizedStart, null))
                put("createdAt", now)
                put("updatedAt", now)
                put("stats", statsToJson(summary))
                put("dailyStats", dailyStatsToJson(dailyStats))
                put(
                    "activeSession",
                    JSONObject().apply {
                        put("isActive", activeStart > 0L)
                        put("startTime", activeStart)
                        put("dateKey", if (activeStart > 0L) dateKeyForMs(activeStart) else "")
                    }
                )
            }

            putJson("$RTDB_BASE_URL/shared_stats/$shareId.json", payload)
            FocusSettingsRepository.setActiveLiveShare(context, shareId, normalizedStart)

            ShareLinkResult(
                shareId = shareId,
                url = buildShareUrl(shareId),
                mode = ShareMode.LIVE,
                startDateMs = normalizedStart,
                endDateMs = null,
                stats = summary
            )
        }
    }

    suspend fun stopLiveShare(context: Context): Result<Unit> = withContext(Dispatchers.IO) {
        val settings = FocusSettingsRepository.getCurrent(context)
        val shareId = settings.activeLiveShareId ?: return@withContext Result.success(Unit)
        stopLiveShareById(context, shareId, settings.activeLiveShareStartDateMs).map { }
    }

    suspend fun stopLiveShareById(
        context: Context,
        shareId: String,
        startDateMs: Long
    ): Result<SharedLinkItem> = withContext(Dispatchers.IO) {
        runCatching {
            val now = System.currentTimeMillis()
            val sessions = fetchAllSessions()
            val finalStats = if (startDateMs > 0L) {
                computeStatsForRange(sessions, startDateMs, now)
            } else {
                SharedStatsSummary(0L, 0, 0L, 0L)
            }
            val finalDailyStats = if (startDateMs > 0L) {
                computeDailyStatsForRange(sessions, startDateMs, now)
            } else {
                emptyMap()
            }
            val newDateLabel = if (startDateMs > 0L) {
                formatDateRangeLabel(startDateMs, now)
            } else {
                "Stopped"
            }

            val payload = JSONObject().apply {
                put("status", "stopped")
                put("endDateMs", now)
                put("endDateKey", dateKeyForMs(now))
                put("dateLabel", newDateLabel)
                put("updatedAt", now)
                if (startDateMs > 0L) {
                    put("stats", statsToJson(finalStats))
                    put("dailyStats", dailyStatsToJson(finalDailyStats))
                }
                put(
                    "activeSession",
                    JSONObject().apply {
                        put("isActive", false)
                        put("startTime", 0L)
                        put("dateKey", "")
                    }
                )
            }

            patchJson("$RTDB_BASE_URL/shared_stats/$shareId.json", payload)
            if (FocusSettingsRepository.getCurrent(context).activeLiveShareId == shareId) {
                FocusSettingsRepository.clearActiveLiveShare(context)
            }

            SharedLinkItem(
                shareId = shareId,
                url = buildShareUrl(shareId),
                mode = ShareMode.LIVE,
                status = "stopped",
                startDateMs = startDateMs,
                endDateMs = now,
                dateLabel = newDateLabel,
                createdAt = now,
                updatedAt = now,
                stats = finalStats
            )
        }
    }

    suspend fun fetchAllSharedLinks(): Result<List<SharedLinkItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("$RTDB_BASE_URL/shared_stats.json")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    error("Failed to fetch shared links (${response.code})")
                }
                val body = response.body?.string()?.trim().orEmpty()
                if (body.isEmpty() || body == "null") {
                    return@runCatching emptyList()
                }
                val root = JSONObject(body)
                val items = mutableListOf<SharedLinkItem>()
                val keys = root.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val obj = root.optJSONObject(key) ?: continue
                    val id = obj.optString("id", key).ifBlank { key }
                    val modeStr = obj.optString("mode", "range")
                    val mode = if (modeStr == "live") ShareMode.LIVE else ShareMode.RANGE
                    val status = obj.optString("status", if (mode == ShareMode.LIVE) "active" else "snapshot")
                    val startDateMs = obj.optLong("startDateMs", 0L)
                    val endDateMs = if (obj.isNull("endDateMs")) null else obj.optLong("endDateMs", 0L).takeIf { it > 0L }
                    val createdAt = obj.optLong("createdAt", 0L)
                    val updatedAt = obj.optLong("updatedAt", createdAt)
                    val dateLabel = obj.optString("dateLabel", "").ifBlank {
                        if (startDateMs > 0L) formatDateRangeLabel(startDateMs, endDateMs) else "Selected dates"
                    }
                    val statsObj = obj.optJSONObject("stats")
                    val stats = SharedStatsSummary(
                        totalFocusMs = statsObj?.optLong("totalFocusMs", 0L) ?: 0L,
                        sessionCount = statsObj?.optInt("sessionCount", 0) ?: 0,
                        averageMs = statsObj?.optLong("averageMs", 0L) ?: 0L,
                        longestMs = statsObj?.optLong("longestMs", 0L) ?: 0L
                    )
                    items.add(
                        SharedLinkItem(
                            shareId = id,
                            url = buildShareUrl(id),
                            mode = mode,
                            status = status,
                            startDateMs = startDateMs,
                            endDateMs = endDateMs,
                            dateLabel = dateLabel,
                            createdAt = createdAt,
                            updatedAt = updatedAt,
                            stats = stats
                        )
                    )
                }
                items.sortedByDescending { it.createdAt }
            }
        }
    }

    suspend fun deleteSharedLink(context: Context, shareId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("$RTDB_BASE_URL/shared_stats/$shareId.json")
                .delete()
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    error("Failed to delete link (${response.code})")
                }
            }
            if (FocusSettingsRepository.getCurrent(context).activeLiveShareId == shareId) {
                FocusSettingsRepository.clearActiveLiveShare(context)
            }
        }
    }

    suspend fun syncLiveShareOnSessionStart(
        context: Context,
        sessionStartTimeMs: Long
    ) = withContext(Dispatchers.IO) {
        try {
            val settings = FocusSettingsRepository.getCurrent(context)
            val shareId = settings.activeLiveShareId ?: return@withContext
            val payload = JSONObject().apply {
                put("updatedAt", System.currentTimeMillis())
                put(
                    "activeSession",
                    JSONObject().apply {
                        put("isActive", true)
                        put("startTime", sessionStartTimeMs)
                        put("dateKey", dateKeyForMs(sessionStartTimeMs))
                    }
                )
            }
            patchJson("$RTDB_BASE_URL/shared_stats/$shareId.json", payload)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun syncLiveShareOnSessionEnd(context: Context) = withContext(Dispatchers.IO) {
        try {
            val settings = FocusSettingsRepository.getCurrent(context)
            val shareId = settings.activeLiveShareId ?: return@withContext
            val startMs = settings.activeLiveShareStartDateMs
            if (startMs <= 0L) return@withContext

            val sessions = fetchAllSessions()
            val summary = computeStatsForRange(sessions, startMs, null)
            val dailyStats = computeDailyStatsForRange(sessions, startMs, null)

            val payload = JSONObject().apply {
                put("updatedAt", System.currentTimeMillis())
                put("stats", statsToJson(summary))
                put("dailyStats", dailyStatsToJson(dailyStats))
                put(
                    "activeSession",
                    JSONObject().apply {
                        put("isActive", false)
                        put("startTime", 0L)
                        put("dateKey", "")
                    }
                )
            }
            patchJson("$RTDB_BASE_URL/shared_stats/$shareId.json", payload)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun statsToJson(stats: SharedStatsSummary): JSONObject = JSONObject().apply {
        put("totalFocusMs", stats.totalFocusMs)
        put("sessionCount", stats.sessionCount)
        put("averageMs", stats.averageMs)
        put("longestMs", stats.longestMs)
    }

    private fun dailyStatsToJson(daily: Map<String, SharedStatsSummary>): JSONObject = JSONObject().apply {
        for ((dateKey, summary) in daily) {
            put(dateKey, statsToJson(summary))
        }
    }

    private fun putJson(url: String, json: JSONObject) {
        val body = json.toString().toRequestBody(jsonMediaType)
        val request = Request.Builder().url(url).put(body).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("Failed to write share data (${response.code})")
            }
        }
    }

    private fun patchJson(url: String, json: JSONObject) {
        val body = json.toString().toRequestBody(jsonMediaType)
        val request = Request.Builder().url(url).patch(body).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("Failed to update share data (${response.code})")
            }
        }
    }
}
