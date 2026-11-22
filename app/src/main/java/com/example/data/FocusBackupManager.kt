package com.example.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Immutable
import com.example.BuildConfig
import com.example.service.ScheduleAlarmScheduler
import com.example.widget.NothingFocusWidgetUpdater
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Immutable
data class BackupPreviewSummary(
    val schemaVersion: Int,
    val exportedAtMs: Long,
    val appVersion: String,
    val sessionCount: Int,
    val totalFocusMs: Long,
    val scheduleCount: Int,
    val allowedAppsCount: Int,
    val totalXp: Int,
    val quizHistoryCount: Int,
    val rawRoot: JSONObject
)

sealed class BackupValidationResult {
    data class Valid(val summary: BackupPreviewSummary) : BackupValidationResult()
    data class Invalid(val reason: String) : BackupValidationResult()
}

@Immutable
data class CurrentDataSnapshot(
    val sessionCount: Int,
    val totalFocusMs: Long,
    val scheduleCount: Int,
    val allowedAppsCount: Int,
    val totalXp: Int,
    val quizHistoryCount: Int
)

/**
 * Complete on-device JSON export & import backup engine for FocusLock.
 *
 * Captures and restores:
 * - All FocusSettingsRepository preferences (theme, layout, pill size, allowed apps, goal, toggles, widgets)
 * - All FocusStatsRepository on-device focus sessions
 * - All FocusXpRepository totals, streaks, daily focus maps, bonus/penalty records & XP history
 * - All FocusScheduleRepository schedules & lock layout config
 * - All WidgetStudioRepository widget configurations
 * - All NotificationBlockerRepository summary logs
 * - All FocusQuizEngine asked-question signatures (no-repeat memory)
 */
object FocusBackupManager {
    const val SCHEMA_VERSION = 1
    private const val FORMAT_IDENTIFIER = "focuslock_backup"

    private const val PREFS_SETTINGS = "focus_settings"
    private const val PREFS_XP = "focus_xp_prefs"
    private const val PREFS_SCHEDULES = "focus_schedules_prefs"
    private const val PREFS_WIDGETS = "focus_widget_studio_prefs"
    private const val PREFS_NOTIFICATIONS = "focus_blocked_notifications"

    fun suggestedFileName(nowMs: Long = System.currentTimeMillis()): String {
        val stamp = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US).format(Date(nowMs))
        return "focuslock_backup_$stamp.json"
    }

    fun getCurrentSnapshot(context: Context): CurrentDataSnapshot {
        val appCtx = context.applicationContext
        val sessions = FocusStatsRepository.getAllSessionRecords(appCtx)
        val settings = FocusSettingsRepository.getCurrent(appCtx)
        val schedules = FocusScheduleRepository.getCurrent(appCtx).schedules
        val xp = FocusXpRepository.getCurrent(appCtx)
        val quizCount = FocusQuizEngine.getAskedCount(appCtx)
        return CurrentDataSnapshot(
            sessionCount = sessions.size,
            totalFocusMs = sessions.sumOf { it.duration },
            scheduleCount = schedules.size,
            allowedAppsCount = settings.allowedApps.size,
            totalXp = xp.totalXp,
            quizHistoryCount = quizCount
        )
    }

    fun exportToJson(context: Context): String {
        val appCtx = context.applicationContext
        val nowMs = System.currentTimeMillis()
        val sessions = FocusStatsRepository.getAllSessionRecords(appCtx)
        val askedSignatures = FocusQuizEngine.getAskedSignatures(appCtx)

        val sessionsArr = JSONArray()
        for (rec in sessions) {
            sessionsArr.put(
                JSONObject().apply {
                    put("id", rec.id)
                    put("startTime", rec.startTime)
                    put("duration", rec.duration)
                }
            )
        }

        val quizArr = JSONArray()
        for (sig in askedSignatures) {
            quizArr.put(sig)
        }

        val root = JSONObject().apply {
            put("format", FORMAT_IDENTIFIER)
            put("schemaVersion", SCHEMA_VERSION)
            put("appVersion", BuildConfig.VERSION_NAME)
            put("exportedAtMs", nowMs)
            put("settingsPrefs", dumpSharedPrefs(appCtx.getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE)))
            put("sessions", sessionsArr)
            put("xpPrefs", dumpSharedPrefs(appCtx.getSharedPreferences(PREFS_XP, Context.MODE_PRIVATE)))
            put("schedulePrefs", dumpSharedPrefs(appCtx.getSharedPreferences(PREFS_SCHEDULES, Context.MODE_PRIVATE)))
            put("widgetStudioPrefs", dumpSharedPrefs(appCtx.getSharedPreferences(PREFS_WIDGETS, Context.MODE_PRIVATE)))
            put("notificationBlockerPrefs", dumpSharedPrefs(appCtx.getSharedPreferences(PREFS_NOTIFICATIONS, Context.MODE_PRIVATE)))
            put("quizAskedSignatures", quizArr)
        }
        return root.toString(2)
    }

    fun validateBackupJson(rawJson: String): BackupValidationResult {
        val trimmed = rawJson.trim()
        if (trimmed.isEmpty()) {
            return BackupValidationResult.Invalid("Backup data is empty.")
        }

        val root = try {
            JSONObject(trimmed)
        } catch (_: Exception) {
            return BackupValidationResult.Invalid("Invalid JSON format. Please choose a valid FocusLock backup file.")
        }

        val format = root.optString("format", "")
        if (format != FORMAT_IDENTIFIER) {
            return BackupValidationResult.Invalid("Unrecognized backup file. Expected a FocusLock backup.")
        }

        val schemaVersion = root.optInt("schemaVersion", -1)
        if (schemaVersion < 1 || schemaVersion > SCHEMA_VERSION) {
            return BackupValidationResult.Invalid("Unsupported backup schema version ($schemaVersion).")
        }

        val sessionsArr = root.optJSONArray("sessions")
        var sessionCount = 0
        var totalFocusMs = 0L
        if (sessionsArr != null) {
            for (i in 0 until sessionsArr.length()) {
                val obj = sessionsArr.optJSONObject(i) ?: continue
                val start = obj.optLong("startTime", 0L)
                val dur = obj.optLong("duration", 0L)
                if (start > 0L && dur > 0L) {
                    sessionCount++
                    totalFocusMs += dur
                }
            }
        }

        val schedulePrefsObj = root.optJSONObject("schedulePrefs")
        val scheduleCount = countJsonArrayInsidePrefs(schedulePrefsObj, "schedules_json")

        val settingsPrefsObj = root.optJSONObject("settingsPrefs")
        val allowedAppsCount = if (settingsPrefsObj?.has("allowed_apps_json") == true) {
            countJsonArrayInsidePrefs(settingsPrefsObj, "allowed_apps_json")
        } else {
            FocusSettingsRepository.DEFAULT_ALLOWED_APPS.size
        }

        val xpPrefsObj = root.optJSONObject("xpPrefs")
        val totalXp = readTypedIntFromPrefsObj(xpPrefsObj, "total_xp", 0)

        val quizArr = root.optJSONArray("quizAskedSignatures")
        val quizCount = quizArr?.length() ?: 0

        return BackupValidationResult.Valid(
            BackupPreviewSummary(
                schemaVersion = schemaVersion,
                exportedAtMs = root.optLong("exportedAtMs", 0L),
                appVersion = root.optString("appVersion", "Unknown"),
                sessionCount = sessionCount,
                totalFocusMs = totalFocusMs,
                scheduleCount = scheduleCount,
                allowedAppsCount = allowedAppsCount,
                totalXp = totalXp,
                quizHistoryCount = quizCount,
                rawRoot = root
            )
        )
    }

    fun applyValidatedBackup(context: Context, summary: BackupPreviewSummary): Result<Unit> = runCatching {
        val appCtx = context.applicationContext
        val root = summary.rawRoot

        // 1. Settings
        root.optJSONObject("settingsPrefs")?.let { obj ->
            restoreSharedPrefs(appCtx.getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE), obj)
        }
        FocusSettingsRepository.reloadFromPrefs(appCtx)

        // 2. Sessions (on-device stats)
        val sessionsArr = root.optJSONArray("sessions")
        val parsedRecords = buildList {
            if (sessionsArr != null) {
                for (i in 0 until sessionsArr.length()) {
                    val obj = sessionsArr.optJSONObject(i) ?: continue
                    val startTime = obj.optLong("startTime", 0L)
                    val duration = obj.optLong("duration", 0L)
                    val id = obj.optString("id").ifBlank { "loc_${startTime}_${duration}" }
                    if (startTime > 0L && duration > 0L) {
                        add(CachedSessionRecord(id = id, startTime = startTime, duration = duration))
                    }
                }
            }
        }
        FocusStatsRepository.replaceAllSessions(appCtx, parsedRecords)

        // 3. XP & Streaks
        root.optJSONObject("xpPrefs")?.let { obj ->
            restoreSharedPrefs(appCtx.getSharedPreferences(PREFS_XP, Context.MODE_PRIVATE), obj)
        }
        FocusXpRepository.reloadFromPrefs(appCtx)

        // 4. Schedules & Layout
        root.optJSONObject("schedulePrefs")?.let { obj ->
            restoreSharedPrefs(appCtx.getSharedPreferences(PREFS_SCHEDULES, Context.MODE_PRIVATE), obj)
        }
        FocusScheduleRepository.reloadFromPrefs(appCtx)

        // 5. Widget Studio
        root.optJSONObject("widgetStudioPrefs")?.let { obj ->
            restoreSharedPrefs(appCtx.getSharedPreferences(PREFS_WIDGETS, Context.MODE_PRIVATE), obj)
        }
        WidgetStudioRepository.reloadFromPrefs(appCtx)

        // 6. Blocked Notifications
        root.optJSONObject("notificationBlockerPrefs")?.let { obj ->
            restoreSharedPrefs(appCtx.getSharedPreferences(PREFS_NOTIFICATIONS, Context.MODE_PRIVATE), obj)
        }
        NotificationBlockerRepository.reloadFromPrefs(appCtx)

        // 7. Quiz Asked Signatures
        val quizArr = root.optJSONArray("quizAskedSignatures")
        val signatures = buildList {
            if (quizArr != null) {
                for (i in 0 until quizArr.length()) {
                    val sig = quizArr.optString(i, "").trim()
                    if (sig.isNotEmpty()) add(sig)
                }
            }
        }
        FocusQuizEngine.replaceAskedSignatures(appCtx, signatures)

        // 8. Re-sync alarms, home widgets, and mark first-install setup done
        FocusOnboardingRepository.markSetupCompleted(appCtx)
        runCatching { ScheduleAlarmScheduler.syncAllAlarms(appCtx) }
        runCatching { NothingFocusWidgetUpdater.scheduleUpdateAll(appCtx) }
    }

    private fun dumpSharedPrefs(prefs: SharedPreferences): JSONObject {
        val result = JSONObject()
        for ((key, value) in prefs.all) {
            if (value == null) continue
            val entry = JSONObject()
            when (value) {
                is Boolean -> {
                    entry.put("type", "boolean")
                    entry.put("value", value)
                }
                is Int -> {
                    entry.put("type", "int")
                    entry.put("value", value)
                }
                is Long -> {
                    entry.put("type", "long")
                    entry.put("value", value)
                }
                is Float -> {
                    entry.put("type", "float")
                    entry.put("value", value.toDouble())
                }
                is String -> {
                    entry.put("type", "string")
                    entry.put("value", value)
                }
                is Set<*> -> {
                    val arr = JSONArray()
                    value.filterIsInstance<String>().forEach { arr.put(it) }
                    entry.put("type", "stringSet")
                    entry.put("value", arr)
                }
                else -> continue
            }
            result.put(key, entry)
        }
        return result
    }

    private fun restoreSharedPrefs(prefs: SharedPreferences, data: JSONObject) {
        val editor = prefs.edit().clear()
        val keys = data.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val entry = data.optJSONObject(key) ?: continue
            when (entry.optString("type")) {
                "boolean" -> editor.putBoolean(key, entry.optBoolean("value", false))
                "int" -> editor.putInt(key, entry.optInt("value", 0))
                "long" -> editor.putLong(key, entry.optLong("value", 0L))
                "float" -> editor.putFloat(key, entry.optDouble("value", 0.0).toFloat())
                "string" -> editor.putString(key, entry.optString("value", ""))
                "stringSet" -> {
                    val arr = entry.optJSONArray("value")
                    val set = LinkedHashSet<String>()
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            set.add(arr.optString(i, ""))
                        }
                    }
                    editor.putStringSet(key, set)
                }
            }
        }
        editor.commit()
    }

    private fun countJsonArrayInsidePrefs(prefsObj: JSONObject?, key: String): Int {
        val entry = prefsObj?.optJSONObject(key) ?: return 0
        val raw = entry.optString("value", "")
        if (raw.isBlank()) return 0
        return runCatching { JSONArray(raw).length() }.getOrDefault(0)
    }

    private fun readTypedIntFromPrefsObj(prefsObj: JSONObject?, key: String, fallback: Int): Int {
        val entry = prefsObj?.optJSONObject(key) ?: return fallback
        return entry.optInt("value", fallback)
    }
}
