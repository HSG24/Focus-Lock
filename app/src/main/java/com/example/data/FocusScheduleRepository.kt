package com.example.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Immutable
import com.example.service.ScheduleAlarmScheduler
import com.example.widget.NothingFocusWidgetUpdater
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

enum class ScheduleRepeatMode(val label: String, val shortLabel: String) {
    DAILY("Every day", "Daily"),
    WEEKDAYS("Mon – Fri", "Weekdays"),
    WEEKENDS("Sat – Sun", "Weekends"),
    CUSTOM("Custom days", "Custom"),
    ONCE("One time", "Once")
}

enum class ScheduledLockModuleType(
    val title: String,
    val subtitle: String
) {
    CLOCK_DATE("Time & Date", "Minimal live clock and date pill"),
    SCHEDULE_BADGE("Schedule Pill", "Active schedule name and window"),
    COUNTDOWN_TIMER("Countdown Timer", "Hero timer showing remaining lock time"),
    SESSION_TIMELINE("Session Timeline", "Dot-matrix window track from start to unlock"),
    FOCUS_MANTRA("Focus Mantra", "Personal intention or quote capsule"),
    DAILY_PROGRESS("Goal & Streak Pill", "Today's focus goal % and active streak"),
    ALLOWED_APPS("Essential Apps", "Quick-launch dock for allowed apps")
}

enum class LockModuleSize(val shortLabel: String, val scaleFactor: Float) {
    SMALL("S", 0.84f),
    MEDIUM("M", 1.0f),
    LARGE("L", 1.18f)
}

enum class LockModuleAlignment(val shortLabel: String) {
    START("Left"),
    CENTER("Center"),
    END("Right")
}

enum class ScheduledTimerStyle(val label: String) {
    DOT_RING("Dot Ring"),
    MINIMAL_ARC("Soft Arc"),
    DIGITS_ONLY("Digits")
}

enum class LockSpacingDensity(val label: String, val gapDp: Int) {
    COMPACT("Compact", 10),
    BALANCED("Balanced", 16),
    AIRY("Airy", 24)
}

enum class ScheduledBottomDockMode(val label: String) {
    CONSOLE("Console"),
    COMPACT("Compact"),
    MINIMAL("Minimal")
}

@Immutable
data class FocusSchedule(
    val id: String,
    val name: String,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val repeatMode: ScheduleRepeatMode = ScheduleRepeatMode.DAILY,
    /** Calendar.MONDAY (2) .. Calendar.SUNDAY (1) values */
    val customDays: Set<Int> = setOf(
        Calendar.MONDAY,
        Calendar.TUESDAY,
        Calendar.WEDNESDAY,
        Calendar.THURSDAY,
        Calendar.FRIDAY
    ),
    val enabled: Boolean = true,
    /** yyyy-MM-dd key when this schedule was skipped or auto-cancelled by a manual OTP session */
    val skippedDateKey: String? = null
) {
    fun durationMinutes(): Int {
        val startTotal = startHour * 60 + startMinute
        val endTotal = endHour * 60 + endMinute
        val diff = endTotal - startTotal
        return if (diff > 0) diff else (24 * 60 + diff)
    }

    fun formattedTimeRange(use24Hour: Boolean = false): String {
        return "${formatClock(startHour, startMinute, use24Hour)} → ${formatClock(endHour, endMinute, use24Hour)}"
    }

    fun repeatSummary(): String = when (repeatMode) {
        ScheduleRepeatMode.DAILY -> "Daily"
        ScheduleRepeatMode.WEEKDAYS -> "Weekdays"
        ScheduleRepeatMode.WEEKENDS -> "Weekends"
        ScheduleRepeatMode.ONCE -> "One-time"
        ScheduleRepeatMode.CUSTOM -> {
            val ordered = listOf(
                Calendar.MONDAY to "M",
                Calendar.TUESDAY to "T",
                Calendar.WEDNESDAY to "W",
                Calendar.THURSDAY to "T",
                Calendar.FRIDAY to "F",
                Calendar.SATURDAY to "S",
                Calendar.SUNDAY to "S"
            )
            val active = ordered.filter { it.first in customDays }.map { it.second }
            if (active.size == 7) "Daily" else if (active.isEmpty()) "Custom" else active.joinToString(" ")
        }
    }

    fun matchesDayOfWeek(dayOfWeek: Int): Boolean = when (repeatMode) {
        ScheduleRepeatMode.DAILY -> true
        ScheduleRepeatMode.WEEKDAYS -> dayOfWeek in setOf(
            Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY
        )
        ScheduleRepeatMode.WEEKENDS -> dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY
        ScheduleRepeatMode.CUSTOM -> dayOfWeek in customDays
        ScheduleRepeatMode.ONCE -> true
    }

    companion object {
        fun formatClock(hour24: Int, minute: Int, use24Hour: Boolean = false): String {
            val safeH = hour24.coerceIn(0, 23)
            val safeM = minute.coerceIn(0, 59)
            return if (use24Hour) {
                String.format(Locale.US, "%02d:%02d", safeH, safeM)
            } else {
                val period = if (safeH < 12) "AM" else "PM"
                val h12 = when {
                    safeH == 0 -> 12
                    safeH > 12 -> safeH - 12
                    else -> safeH
                }
                String.format(Locale.US, "%d:%02d %s", h12, safeM, period)
            }
        }
    }
}

@Immutable
data class ScheduledLockModuleConfig(
    val type: ScheduledLockModuleType,
    val enabled: Boolean = true,
    val size: LockModuleSize = LockModuleSize.MEDIUM,
    val alignment: LockModuleAlignment = LockModuleAlignment.CENTER
)

@Immutable
data class ScheduledLockLayoutConfig(
    val modules: List<ScheduledLockModuleConfig> = DEFAULT_MODULES,
    val timerStyle: ScheduledTimerStyle = ScheduledTimerStyle.DOT_RING,
    val spacingDensity: LockSpacingDensity = LockSpacingDensity.BALANCED,
    val bottomDockMode: ScheduledBottomDockMode = ScheduledBottomDockMode.COMPACT,
    val customMantra: String = "Stay present. Protect your focus."
) {
    companion object {
        val DEFAULT_MODULES = listOf(
            ScheduledLockModuleConfig(ScheduledLockModuleType.CLOCK_DATE, enabled = true, size = LockModuleSize.SMALL),
            ScheduledLockModuleConfig(ScheduledLockModuleType.SCHEDULE_BADGE, enabled = true, size = LockModuleSize.MEDIUM),
            ScheduledLockModuleConfig(ScheduledLockModuleType.COUNTDOWN_TIMER, enabled = true, size = LockModuleSize.LARGE),
            ScheduledLockModuleConfig(ScheduledLockModuleType.ALLOWED_APPS, enabled = true, size = LockModuleSize.MEDIUM),
            ScheduledLockModuleConfig(ScheduledLockModuleType.DAILY_PROGRESS, enabled = true, size = LockModuleSize.MEDIUM)
        )
    }
}

@Immutable
data class UpcomingScheduleWindow(
    val schedule: FocusSchedule,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val isCurrentlyActive: Boolean
)

@Immutable
data class FocusScheduleState(
    val schedules: List<FocusSchedule> = emptyList(),
    val layoutConfig: ScheduledLockLayoutConfig = ScheduledLockLayoutConfig()
)

object FocusScheduleRepository {
    private const val PREFS_NAME = "focus_schedules_prefs"
    private const val KEY_SCHEDULES = "schedules_json"
    private const val KEY_LOCK_LAYOUT = "scheduled_lock_layout_json"

    @Volatile
    private var _state: MutableStateFlow<FocusScheduleState>? = null

    fun observe(context: Context): StateFlow<FocusScheduleState> {
        val appCtx = context.applicationContext
        _state?.let { return it.asStateFlow() }
        synchronized(this) {
            _state?.let { return it.asStateFlow() }
            val prefs = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val flow = MutableStateFlow(readState(prefs))
            _state = flow
            return flow.asStateFlow()
        }
    }

    fun getCurrent(context: Context): FocusScheduleState {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return readState(prefs)
    }

    fun todayDateKey(nowMs: Long = System.currentTimeMillis()): String {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMs }
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }

    fun addOrUpdateSchedule(context: Context, schedule: FocusSchedule) {
        mutate(context) { current ->
            val cleanName = schedule.name.trim().ifBlank { "Focus Block" }
            val normalized = schedule.copy(name = cleanName)
            val existingIdx = current.schedules.indexOfFirst { it.id == normalized.id }
            val updated = current.schedules.toMutableList()
            if (existingIdx >= 0) {
                updated[existingIdx] = normalized
            } else {
                updated.add(normalized)
            }
            updated.sortWith(compareBy({ it.startHour }, { it.startMinute }, { it.name }))
            current.copy(schedules = updated)
        }
        ScheduleAlarmScheduler.syncAllAlarms(context)
        NothingFocusWidgetUpdater.scheduleUpdateAll(context)
    }

    fun createNewSchedule(
        name: String,
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int,
        repeatMode: ScheduleRepeatMode,
        customDays: Set<Int>
    ): FocusSchedule = FocusSchedule(
        id = "sched_${UUID.randomUUID().toString().take(8)}",
        name = name.trim().ifBlank { "Focus Block" },
        startHour = startHour.coerceIn(0, 23),
        startMinute = startMinute.coerceIn(0, 59),
        endHour = endHour.coerceIn(0, 23),
        endMinute = endMinute.coerceIn(0, 59),
        repeatMode = repeatMode,
        customDays = customDays,
        enabled = true,
        skippedDateKey = null
    )

    fun deleteSchedule(context: Context, scheduleId: String) {
        mutate(context) { current ->
            current.copy(schedules = current.schedules.filterNot { it.id == scheduleId })
        }
        ScheduleAlarmScheduler.syncAllAlarms(context)
        NothingFocusWidgetUpdater.scheduleUpdateAll(context)
    }

    fun setScheduleEnabled(context: Context, scheduleId: String, enabled: Boolean) {
        mutate(context) { current ->
            current.copy(
                schedules = current.schedules.map { s ->
                    if (s.id == scheduleId) s.copy(enabled = enabled, skippedDateKey = if (enabled) null else s.skippedDateKey)
                    else s
                }
            )
        }
        ScheduleAlarmScheduler.syncAllAlarms(context)
        NothingFocusWidgetUpdater.scheduleUpdateAll(context)
    }

    fun markSkippedForToday(context: Context, scheduleId: String, nowMs: Long = System.currentTimeMillis()) {
        val key = todayDateKey(nowMs)
        mutate(context) { current ->
            current.copy(
                schedules = current.schedules.map { s ->
                    if (s.id == scheduleId) {
                        if (s.repeatMode == ScheduleRepeatMode.ONCE) {
                            s.copy(enabled = false, skippedDateKey = key)
                        } else {
                            s.copy(skippedDateKey = key)
                        }
                    } else s
                }
            )
        }
        ScheduleAlarmScheduler.syncAllAlarms(context)
        NothingFocusWidgetUpdater.scheduleUpdateAll(context)
    }

    fun markOneTimeCompleted(context: Context, scheduleId: String) {
        mutate(context) { current ->
            current.copy(
                schedules = current.schedules.map { s ->
                    if (s.id == scheduleId && s.repeatMode == ScheduleRepeatMode.ONCE) {
                        s.copy(enabled = false)
                    } else s
                }
            )
        }
        ScheduleAlarmScheduler.syncAllAlarms(context)
        NothingFocusWidgetUpdater.scheduleUpdateAll(context)
    }

    fun unskipForToday(context: Context, scheduleId: String) {
        mutate(context) { current ->
            current.copy(
                schedules = current.schedules.map { s ->
                    if (s.id == scheduleId) s.copy(skippedDateKey = null) else s
                }
            )
        }
        ScheduleAlarmScheduler.syncAllAlarms(context)
        NothingFocusWidgetUpdater.scheduleUpdateAll(context)
    }

    // --- Scheduled Lock Layout Customizer Mutations ---

    fun toggleModule(context: Context, type: ScheduledLockModuleType, enabled: Boolean) {
        // Countdown timer is the core of scheduled locks and always stays visible
        if (type == ScheduledLockModuleType.COUNTDOWN_TIMER && !enabled) return
        mutateLayout(context) { layout ->
            layout.copy(
                modules = layout.modules.map { m ->
                    if (m.type == type) m.copy(enabled = enabled) else m
                }
            )
        }
    }

    fun moveModule(context: Context, type: ScheduledLockModuleType, delta: Int) {
        mutateLayout(context) { layout ->
            val list = layout.modules.toMutableList()
            val idx = list.indexOfFirst { it.type == type }
            val targetIdx = (idx + delta).coerceIn(0, list.lastIndex)
            if (idx >= 0 && targetIdx != idx) {
                val item = list.removeAt(idx)
                list.add(targetIdx, item)
            }
            layout.copy(modules = list)
        }
    }

    fun setModuleSize(context: Context, type: ScheduledLockModuleType, size: LockModuleSize) {
        mutateLayout(context) { layout ->
            layout.copy(
                modules = layout.modules.map { m ->
                    if (m.type == type) m.copy(size = size) else m
                }
            )
        }
    }

    fun setModuleAlignment(context: Context, type: ScheduledLockModuleType, alignment: LockModuleAlignment) {
        mutateLayout(context) { layout ->
            layout.copy(
                modules = layout.modules.map { m ->
                    if (m.type == type) m.copy(alignment = alignment) else m
                }
            )
        }
    }

    fun setTimerStyle(context: Context, style: ScheduledTimerStyle) {
        mutateLayout(context) { it.copy(timerStyle = style) }
    }

    fun setSpacingDensity(context: Context, density: LockSpacingDensity) {
        mutateLayout(context) { it.copy(spacingDensity = density) }
    }

    fun setBottomDockMode(context: Context, mode: ScheduledBottomDockMode) {
        mutateLayout(context) { it.copy(bottomDockMode = mode) }
    }

    fun setCustomMantra(context: Context, mantra: String) {
        mutateLayout(context) { it.copy(customMantra = mantra.trim().take(90)) }
    }

    fun resetLayoutDefaults(context: Context) {
        mutateLayout(context) { ScheduledLockLayoutConfig() }
    }

    // --- Schedule Window & Occurrence Resolution ---

    fun computeNextOccurrence(
        schedule: FocusSchedule,
        nowMs: Long = System.currentTimeMillis()
    ): UpcomingScheduleWindow? {
        if (!schedule.enabled) return null
        val durationMs = schedule.durationMinutes().coerceAtLeast(1) * 60_000L
        val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        // Check from yesterday (for active overnight spans) through next 8 days
        for (dayOffset in -1..7) {
            val startCal = Calendar.getInstance().apply {
                timeInMillis = nowMs
                add(Calendar.DATE, dayOffset)
                set(Calendar.HOUR_OF_DAY, schedule.startHour)
                set(Calendar.MINUTE, schedule.startMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startDateKey = dateFmt.format(startCal.time)
            if (schedule.skippedDateKey == startDateKey) continue
            if (!schedule.matchesDayOfWeek(startCal.get(Calendar.DAY_OF_WEEK))) continue

            val startMs = startCal.timeInMillis
            val endMs = startMs + durationMs

            if (nowMs in startMs until endMs) {
                return UpcomingScheduleWindow(
                    schedule = schedule,
                    startTimeMs = startMs,
                    endTimeMs = endMs,
                    isCurrentlyActive = true
                )
            }
            if (startMs > nowMs) {
                return UpcomingScheduleWindow(
                    schedule = schedule,
                    startTimeMs = startMs,
                    endTimeMs = endMs,
                    isCurrentlyActive = false
                )
            }
        }
        return null
    }

    fun findNextUpcomingWindow(
        schedules: List<FocusSchedule>,
        nowMs: Long = System.currentTimeMillis()
    ): UpcomingScheduleWindow? {
        return schedules
            .mapNotNull { computeNextOccurrence(it, nowMs) }
            .minByOrNull { if (it.isCurrentlyActive) it.startTimeMs - 86_400_000L else it.startTimeMs }
    }

    fun isSkippedToday(schedule: FocusSchedule, nowMs: Long = System.currentTimeMillis()): Boolean {
        return schedule.skippedDateKey == todayDateKey(nowMs)
    }

    fun formatRelativeCountdown(targetMs: Long, nowMs: Long = System.currentTimeMillis()): String {
        val diffMs = (targetMs - nowMs).coerceAtLeast(0L)
        val totalMinutes = (diffMs + 59_999L) / 60_000L
        val days = totalMinutes / (24 * 60)
        val hours = (totalMinutes % (24 * 60)) / 60
        val mins = totalMinutes % 60
        return when {
            days > 0 -> "${days}d ${hours}h"
            hours > 0 && mins > 0 -> "${hours}h ${mins}m"
            hours > 0 -> "${hours}h"
            totalMinutes > 0 -> "${totalMinutes}m"
            else -> "Now"
        }
    }

    // --- Persistence ---

    private fun mutate(context: Context, transform: (FocusScheduleState) -> FocusScheduleState) {
        val appCtx = context.applicationContext
        synchronized(this) {
            val prefs = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val current = _state?.value ?: readState(prefs)
            val next = transform(current)
            writeState(prefs, next)
            if (_state != null) {
                _state?.value = next
            } else {
                _state = MutableStateFlow(next)
            }
        }
    }

    private fun mutateLayout(
        context: Context,
        transform: (ScheduledLockLayoutConfig) -> ScheduledLockLayoutConfig
    ) {
        mutate(context) { current ->
            current.copy(layoutConfig = transform(current.layoutConfig))
        }
    }

    private fun readState(prefs: SharedPreferences): FocusScheduleState {
        val schedules = parseSchedules(prefs.getString(KEY_SCHEDULES, "[]"))
        // Fixed scheduled lock layout (Lock Studio removed per user preference)
        return FocusScheduleState(schedules = schedules, layoutConfig = ScheduledLockLayoutConfig())
    }

    private fun writeState(prefs: SharedPreferences, state: FocusScheduleState) {
        prefs.edit()
            .putString(KEY_SCHEDULES, serializeSchedules(state.schedules))
            .putString(KEY_LOCK_LAYOUT, serializeLayout(state.layoutConfig))
            .apply()
    }

    private fun serializeSchedules(list: List<FocusSchedule>): String {
        val arr = JSONArray()
        for (s in list) {
            val daysArr = JSONArray()
            s.customDays.sorted().forEach { daysArr.put(it) }
            arr.put(
                JSONObject().apply {
                    put("id", s.id)
                    put("name", s.name)
                    put("startHour", s.startHour)
                    put("startMinute", s.startMinute)
                    put("endHour", s.endHour)
                    put("endMinute", s.endMinute)
                    put("repeatMode", s.repeatMode.name)
                    put("customDays", daysArr)
                    put("enabled", s.enabled)
                    if (s.skippedDateKey != null) put("skippedDateKey", s.skippedDateKey)
                }
            )
        }
        return arr.toString()
    }

    private fun parseSchedules(raw: String?): List<FocusSchedule> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList(arr.length()) {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val id = o.optString("id").ifBlank { continue }
                    val repeat = runCatching {
                        ScheduleRepeatMode.valueOf(o.optString("repeatMode", ScheduleRepeatMode.DAILY.name))
                    }.getOrDefault(ScheduleRepeatMode.DAILY)
                    val daysArr = o.optJSONArray("customDays")
                    val days = mutableSetOf<Int>()
                    if (daysArr != null) {
                        for (j in 0 until daysArr.length()) {
                            days.add(daysArr.optInt(j))
                        }
                    }
                    add(
                        FocusSchedule(
                            id = id,
                            name = o.optString("name", "Focus Block"),
                            startHour = o.optInt("startHour", 9).coerceIn(0, 23),
                            startMinute = o.optInt("startMinute", 0).coerceIn(0, 59),
                            endHour = o.optInt("endHour", 10).coerceIn(0, 23),
                            endMinute = o.optInt("endMinute", 0).coerceIn(0, 59),
                            repeatMode = repeat,
                            customDays = days,
                            enabled = o.optBoolean("enabled", true),
                            skippedDateKey = o.optString("skippedDateKey", "").takeIf { it.isNotBlank() }
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun serializeLayout(layout: ScheduledLockLayoutConfig): String {
        val modsArr = JSONArray()
        for (m in layout.modules) {
            modsArr.put(
                JSONObject().apply {
                    put("type", m.type.name)
                    put("enabled", m.enabled)
                    put("size", m.size.name)
                    put("alignment", m.alignment.name)
                }
            )
        }
        return JSONObject().apply {
            put("modules", modsArr)
            put("timerStyle", layout.timerStyle.name)
            put("spacingDensity", layout.spacingDensity.name)
            put("bottomDockMode", layout.bottomDockMode.name)
            put("customMantra", layout.customMantra)
        }.toString()
    }

    private fun parseLayout(raw: String?): ScheduledLockLayoutConfig {
        if (raw.isNullOrBlank()) return ScheduledLockLayoutConfig()
        return runCatching {
            val obj = JSONObject(raw)
            val modsArr = obj.optJSONArray("modules")
            val parsedMods = mutableListOf<ScheduledLockModuleConfig>()
            val seenTypes = mutableSetOf<ScheduledLockModuleType>()
            if (modsArr != null) {
                for (i in 0 until modsArr.length()) {
                    val mo = modsArr.optJSONObject(i) ?: continue
                    val type = runCatching {
                        ScheduledLockModuleType.valueOf(mo.optString("type"))
                    }.getOrNull() ?: continue
                    if (!seenTypes.add(type)) continue
                    val size = runCatching {
                        LockModuleSize.valueOf(mo.optString("size", LockModuleSize.MEDIUM.name))
                    }.getOrDefault(LockModuleSize.MEDIUM)
                    val align = runCatching {
                        LockModuleAlignment.valueOf(mo.optString("alignment", LockModuleAlignment.CENTER.name))
                    }.getOrDefault(LockModuleAlignment.CENTER)
                    val enabled = if (type == ScheduledLockModuleType.COUNTDOWN_TIMER) {
                        true
                    } else {
                        mo.optBoolean("enabled", true)
                    }
                    parsedMods.add(ScheduledLockModuleConfig(type, enabled, size, align))
                }
            }
            // Append any newly introduced module types cleanly
            for (def in ScheduledLockLayoutConfig.DEFAULT_MODULES) {
                if (def.type !in seenTypes) {
                    parsedMods.add(def)
                }
            }
            val timerStyle = runCatching {
                ScheduledTimerStyle.valueOf(obj.optString("timerStyle", ScheduledTimerStyle.DOT_RING.name))
            }.getOrDefault(ScheduledTimerStyle.DOT_RING)
            val density = runCatching {
                LockSpacingDensity.valueOf(obj.optString("spacingDensity", LockSpacingDensity.BALANCED.name))
            }.getOrDefault(LockSpacingDensity.BALANCED)
            val dockMode = runCatching {
                ScheduledBottomDockMode.valueOf(obj.optString("bottomDockMode", ScheduledBottomDockMode.CONSOLE.name))
            }.getOrDefault(ScheduledBottomDockMode.CONSOLE)
            val mantra = obj.optString("customMantra", "Stay present. Protect your focus.")
            ScheduledLockLayoutConfig(
                modules = parsedMods.ifEmpty { ScheduledLockLayoutConfig.DEFAULT_MODULES },
                timerStyle = timerStyle,
                spacingDensity = density,
                bottomDockMode = dockMode,
                customMantra = mantra
            )
        }.getOrDefault(ScheduledLockLayoutConfig())
    }
}
