package com.example.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

@Immutable
data class BlockedNotificationItem(
    val id: String,
    val packageName: String,
    val appLabel: String,
    val title: String,
    val snippet: String,
    val timestampMs: Long
)

@Immutable
data class BlockedAppGroup(
    val packageName: String,
    val appLabel: String,
    val items: List<BlockedNotificationItem>
) {
    val count: Int get() = items.size
}

@Immutable
data class NotificationBlockerState(
    val activeSessionBlocked: List<BlockedNotificationItem> = emptyList(),
    val lastSessionSummary: List<BlockedNotificationItem> = emptyList(),
    val showSummarySheet: Boolean = false
) {
    val groupedLastSummary: List<BlockedAppGroup>
        get() = lastSessionSummary
            .groupBy { it.packageName }
            .map { (pkg, list) ->
                BlockedAppGroup(
                    packageName = pkg,
                    appLabel = list.firstOrNull()?.appLabel ?: pkg,
                    items = list.sortedByDescending { it.timestampMs }
                )
            }
            .sortedByDescending { it.count }

    val totalBlockedCount: Int get() = lastSessionSummary.size
}

object NotificationBlockerRepository {
    private const val PREFS_NAME = "focus_blocked_notifications"
    private const val KEY_ACTIVE_ITEMS = "active_blocked_items"
    private const val KEY_SUMMARY_ITEMS = "last_summary_items"
    private const val KEY_SHOW_SHEET = "show_summary_sheet"

    @Volatile
    private var _state: MutableStateFlow<NotificationBlockerState>? = null

    fun observe(context: Context): StateFlow<NotificationBlockerState> {
        val appContext = context.applicationContext
        _state?.let { return it.asStateFlow() }
        synchronized(this) {
            _state?.let { return it.asStateFlow() }
            val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val flow = MutableStateFlow(readState(prefs))
            _state = flow
            return flow.asStateFlow()
        }
    }

    fun getCurrent(context: Context): NotificationBlockerState {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return readState(prefs)
    }

    @Synchronized
    fun startFreshSession(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_ACTIVE_ITEMS, "[]")
            .apply()
        _state?.value = readState(prefs)
    }

    @Synchronized
    fun recordBlockedNotification(
        context: Context,
        packageName: String,
        appLabel: String,
        title: String,
        snippet: String,
        timestampMs: Long = System.currentTimeMillis()
    ) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val active = parseItems(prefs.getString(KEY_ACTIVE_ITEMS, "[]")).toMutableList()
        val item = BlockedNotificationItem(
            id = UUID.randomUUID().toString(),
            packageName = packageName,
            appLabel = appLabel.ifBlank { packageName },
            title = title.ifBlank { appLabel },
            snippet = snippet.take(160),
            timestampMs = timestampMs
        )
        active.add(0, item)
        prefs.edit()
            .putString(KEY_ACTIVE_ITEMS, serializeItems(active.take(250)))
            .apply()
        _state?.value = readState(prefs)
    }

    /**
     * Finalizes the current lock session's blocked notifications log.
     * Returns true if any notifications were intercepted and the summary sheet should open.
     */
    @Synchronized
    fun finalizeSessionOnUnlock(context: Context): Boolean {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val active = parseItems(prefs.getString(KEY_ACTIVE_ITEMS, "[]"))
        if (active.isEmpty()) {
            prefs.edit()
                .putString(KEY_ACTIVE_ITEMS, "[]")
                .putBoolean(KEY_SHOW_SHEET, false)
                .apply()
            _state?.value = readState(prefs)
            return false
        }

        prefs.edit()
            .putString(KEY_SUMMARY_ITEMS, serializeItems(active))
            .putString(KEY_ACTIVE_ITEMS, "[]")
            .putBoolean(KEY_SHOW_SHEET, true)
            .apply()
        _state?.value = readState(prefs)
        return true
    }

    @Synchronized
    fun openLastSummarySheet(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = parseItems(prefs.getString(KEY_SUMMARY_ITEMS, "[]"))
        if (existing.isNotEmpty()) {
            prefs.edit().putBoolean(KEY_SHOW_SHEET, true).apply()
            _state?.value = readState(prefs)
        }
    }

    @Synchronized
    fun dismissSummarySheet(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SHOW_SHEET, false).apply()
        _state?.value = readState(prefs)
    }

    @Synchronized
    fun clearSummaryLog(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_SUMMARY_ITEMS, "[]")
            .putBoolean(KEY_SHOW_SHEET, false)
            .apply()
        _state?.value = readState(prefs)
    }

    private fun readState(prefs: SharedPreferences): NotificationBlockerState {
        return NotificationBlockerState(
            activeSessionBlocked = parseItems(prefs.getString(KEY_ACTIVE_ITEMS, "[]")),
            lastSessionSummary = parseItems(prefs.getString(KEY_SUMMARY_ITEMS, "[]")),
            showSummarySheet = prefs.getBoolean(KEY_SHOW_SHEET, false)
        )
    }

    private fun parseItems(raw: String?): List<BlockedNotificationItem> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    add(
                        BlockedNotificationItem(
                            id = obj.optString("id"),
                            packageName = obj.optString("packageName"),
                            appLabel = obj.optString("appLabel"),
                            title = obj.optString("title"),
                            snippet = obj.optString("snippet"),
                            timestampMs = obj.optLong("timestampMs", 0L)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun serializeItems(items: List<BlockedNotificationItem>): String {
        val arr = JSONArray()
        for (item in items) {
            arr.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("packageName", item.packageName)
                    put("appLabel", item.appLabel)
                    put("title", item.title)
                    put("snippet", item.snippet)
                    put("timestampMs", item.timestampMs)
                }
            )
        }
        return arr.toString()
    }
}
