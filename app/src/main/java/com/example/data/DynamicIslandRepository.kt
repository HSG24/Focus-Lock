package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.provider.Settings
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.service.DynamicIslandOverlayService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

enum class IslandWidthPreset(val label: String, val compactWidthDp: Int) {
    COMPACT("Compact", 196),
    STANDARD("Standard", 232),
    WIDE("Wide", 268)
}

enum class IslandPreviewKind(val label: String) {
    NOTIFICATION_SUMMARY("Notifications"),
    UPCOMING_SCHEDULE("Schedule"),
    MILESTONE("Milestone")
}

enum class MilestoneKind {
    HALF_GOAL,
    GOAL_REACHED,
    STREAK_EXTENDED,
    RANK_UP
}

@Immutable
data class DynamicIslandConfig(
    val enabled: Boolean = true,
    val enableNotificationSummary: Boolean = true,
    val enableScheduleAlert: Boolean = true,
    val enableMilestones: Boolean = true,
    val widthPreset: IslandWidthPreset = IslandWidthPreset.STANDARD,
    val compactHeightDp: Int = 38,
    val topOffsetDp: Int = 8,
    /** 0 = stay until user taps/dismisses; >0 = auto-collapse/dismiss after N seconds */
    val autoDismissSeconds: Int = 8,
    val notificationPersistUntilCleared: Boolean = true,
    val scheduleLeadMinutes: Int = 10,
    val useNothingMonoStyle: Boolean = true,
    val showStatusLed: Boolean = true,
    val useRealAppIcons: Boolean = true,
    val swipeUpToDismiss: Boolean = true,
    val enableHaptics: Boolean = true
)

sealed interface DynamicIslandEvent {
    val id: String

    @Immutable
    data class NotificationSummary(
        override val id: String = "notif_summary",
        val groups: List<BlockedAppGroup>,
        val totalCount: Int
    ) : DynamicIslandEvent

    @Immutable
    data class UpcomingSchedule(
        override val id: String,
        val scheduleId: String,
        val scheduleName: String,
        val timeRangeLabel: String,
        val startsAtMs: Long
    ) : DynamicIslandEvent

    @Immutable
    data class Milestone(
        override val id: String = "milestone_${System.currentTimeMillis()}",
        val kind: MilestoneKind,
        val title: String,
        val subtitle: String,
        val badgeText: String
    ) : DynamicIslandEvent
}

@Immutable
data class DynamicIslandState(
    val config: DynamicIslandConfig = DynamicIslandConfig(),
    val activeEvent: DynamicIslandEvent? = null,
    val isExpanded: Boolean = false,
    /** Non-null while showing a temporary live preview from Settings (auto-hides after N ms). */
    val previewAutoHideMs: Long? = null,
    /** Increments on each live preview tweak so auto-hide countdown smoothly resets without flicker. */
    val previewPulseSeq: Long = 0L
)

object DynamicIslandRepository {
    private const val PREFS_NAME = "focus_dynamic_island_prefs"
    private const val KEY_ENABLED = "di_enabled"
    private const val KEY_NOTIF_SUMMARY = "di_notif_summary"
    private const val KEY_SCHEDULE_ALERT = "di_schedule_alert"
    private const val KEY_MILESTONES = "di_milestones"
    private const val KEY_WIDTH_PRESET = "di_width_preset"
    private const val KEY_COMPACT_HEIGHT = "di_compact_height_dp"
    private const val KEY_TOP_OFFSET = "di_top_offset_dp"
    private const val KEY_AUTO_DISMISS_SEC = "di_auto_dismiss_sec"
    private const val KEY_NOTIF_PERSIST = "di_notif_persist"
    private const val KEY_SCHEDULE_LEAD_MIN = "di_schedule_lead_min"
    private const val KEY_NOTHING_MONO = "di_nothing_mono"
    private const val KEY_STATUS_LED = "di_status_led"
    private const val KEY_REAL_APP_ICONS = "di_real_app_icons"
    private const val KEY_SWIPE_DISMISS = "di_swipe_dismiss"
    private const val KEY_HAPTICS = "di_haptics"

    private const val KEY_LAST_HALF_GOAL_DAY = "di_last_half_goal_day"
    private const val KEY_LAST_FULL_GOAL_DAY = "di_last_full_goal_day"
    private const val KEY_LAST_STREAK_COUNT = "di_last_streak_count"

    @Volatile
    private var _state: MutableStateFlow<DynamicIslandState>? = null

    private val realAppIconCache = ConcurrentHashMap<String, ImageBitmap>()

    fun observe(context: Context): StateFlow<DynamicIslandState> {
        val appCtx = context.applicationContext
        _state?.let { return it.asStateFlow() }
        synchronized(this) {
            _state?.let { return it.asStateFlow() }
            val prefs = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val flow = MutableStateFlow(DynamicIslandState(config = readConfig(prefs)))
            _state = flow
            return flow.asStateFlow()
        }
    }

    fun getConfig(context: Context): DynamicIslandConfig {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return readConfig(prefs)
    }

    fun updateConfig(context: Context, transform: (DynamicIslandConfig) -> DynamicIslandConfig) {
        val appCtx = context.applicationContext
        synchronized(this) {
            val prefs = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val currentState = _state?.value ?: DynamicIslandState(config = readConfig(prefs))
            val nextConfig = transform(currentState.config)
            writeConfig(prefs, nextConfig)
            val nextEvent = if (!nextConfig.enabled) null else currentState.activeEvent
            val nextState = currentState.copy(
                config = nextConfig,
                activeEvent = nextEvent,
                isExpanded = if (nextEvent == null) false else currentState.isExpanded
            )
            if (_state != null) {
                _state?.value = nextState
            } else {
                _state = MutableStateFlow(nextState)
            }
            if (!nextConfig.enabled) {
                DynamicIslandOverlayService.stop(appCtx)
            }
        }
    }

    fun showNotificationSummary(
        context: Context,
        blockerState: NotificationBlockerState,
        forceShow: Boolean = false
    ) {
        val appCtx = context.applicationContext
        val cfg = getConfig(appCtx)
        if (!forceShow && (!cfg.enabled || !cfg.enableNotificationSummary)) return
        val groups = blockerState.groupedLastSummary
        val count = blockerState.totalBlockedCount
        if (groups.isEmpty() || count <= 0) return

        presentEvent(
            context = appCtx,
            event = DynamicIslandEvent.NotificationSummary(
                groups = groups,
                totalCount = count
            )
        )
    }

    fun showUpcomingSchedule(
        context: Context,
        schedule: FocusSchedule,
        startsAtMs: Long,
        forceShow: Boolean = false
    ) {
        val appCtx = context.applicationContext
        val cfg = getConfig(appCtx)
        if (!forceShow && (!cfg.enabled || !cfg.enableScheduleAlert)) return
        presentEvent(
            context = appCtx,
            event = DynamicIslandEvent.UpcomingSchedule(
                id = "sched_warn_${schedule.id}_${startsAtMs}",
                scheduleId = schedule.id,
                scheduleName = schedule.name,
                timeRangeLabel = schedule.formattedTimeRange(),
                startsAtMs = startsAtMs
            )
        )
    }

    fun showMilestone(
        context: Context,
        kind: MilestoneKind,
        title: String,
        subtitle: String,
        badgeText: String,
        forceShow: Boolean = false
    ) {
        val appCtx = context.applicationContext
        val cfg = getConfig(appCtx)
        if (!forceShow && (!cfg.enabled || !cfg.enableMilestones)) return
        presentEvent(
            context = appCtx,
            event = DynamicIslandEvent.Milestone(
                kind = kind,
                title = title,
                subtitle = subtitle,
                badgeText = badgeText
            )
        )
    }

    fun evaluatePostSessionMilestones(
        context: Context,
        previousXpState: FocusXpState,
        updatedXpState: FocusXpState,
        dailyGoalMinutes: Long
    ) {
        val appCtx = context.applicationContext
        val cfg = getConfig(appCtx)
        if (!cfg.enabled || !cfg.enableMilestones) return

        val goalMs = dailyGoalMinutes.coerceAtLeast(1L) * 60_000L
        val prevRatio = previousXpState.todayFocusMs.toFloat() / goalMs.toFloat()
        val nextRatio = updatedXpState.todayFocusMs.toFloat() / goalMs.toFloat()
        val todayKey = FocusScheduleRepository.todayDateKey()
        val prefs = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        when {
            updatedXpState.rank.title != previousXpState.rank.title &&
                updatedXpState.totalXp > previousXpState.totalXp -> {
                showMilestone(
                    context = appCtx,
                    kind = MilestoneKind.RANK_UP,
                    title = "Rank Up · ${updatedXpState.rank.title}",
                    subtitle = "${updatedXpState.totalXp} Focus XP earned",
                    badgeText = "LEVEL UP"
                )
            }
            nextRatio >= 1f && prevRatio < 1f && prefs.getString(KEY_LAST_FULL_GOAL_DAY, null) != todayKey -> {
                prefs.edit().putString(KEY_LAST_FULL_GOAL_DAY, todayKey).apply()
                showMilestone(
                    context = appCtx,
                    kind = MilestoneKind.GOAL_REACHED,
                    title = "Daily Goal Complete",
                    subtitle = "${FocusStatsRepository.formatDurationShort(updatedXpState.todayFocusMs)} focused today",
                    badgeText = "100%"
                )
            }
            nextRatio >= 0.5f && prevRatio < 0.5f && prefs.getString(KEY_LAST_HALF_GOAL_DAY, null) != todayKey -> {
                prefs.edit().putString(KEY_LAST_HALF_GOAL_DAY, todayKey).apply()
                showMilestone(
                    context = appCtx,
                    kind = MilestoneKind.HALF_GOAL,
                    title = "Halfway to Goal",
                    subtitle = "${FocusStatsRepository.formatDurationShort(updatedXpState.todayFocusMs)} logged so far",
                    badgeText = "50%"
                )
            }
            updatedXpState.currentStreak > previousXpState.currentStreak &&
                updatedXpState.currentStreak > prefs.getInt(KEY_LAST_STREAK_COUNT, 0) -> {
                prefs.edit().putInt(KEY_LAST_STREAK_COUNT, updatedXpState.currentStreak).apply()
                showMilestone(
                    context = appCtx,
                    kind = MilestoneKind.STREAK_EXTENDED,
                    title = "${updatedXpState.currentStreak}-Day Streak",
                    subtitle = "Focus consistency unbroken",
                    badgeText = "${updatedXpState.currentStreak}D"
                )
            }
        }
    }

    fun setExpanded(context: Context, expanded: Boolean) {
        val flow = ensureFlow(context.applicationContext)
        if (flow.value.activeEvent != null) {
            flow.value = flow.value.copy(isExpanded = expanded)
        }
    }

    /**
     * Displays (or keeps alive and updates) a temporary live Dynamic Island preview at the top
     * of the screen for [durationMs] whenever the user tweaks size, offset, or visual style.
     */
    fun showLivePreview(
        context: Context,
        event: DynamicIslandEvent,
        durationMs: Long = 3500L
    ) {
        val appCtx = context.applicationContext
        val flow = ensureFlow(appCtx)
        val prev = flow.value
        val sameType = prev.activeEvent?.let { it::class == event::class } == true
        flow.value = prev.copy(
            activeEvent = event,
            isExpanded = if (sameType) prev.isExpanded else false,
            previewAutoHideMs = durationMs.coerceAtLeast(1500L),
            previewPulseSeq = prev.previewPulseSeq + 1L
        )
        if (runCatching { Settings.canDrawOverlays(appCtx) }.getOrDefault(false)) {
            DynamicIslandOverlayService.show(appCtx)
        }
    }

    fun dismissActive(context: Context) {
        val appCtx = context.applicationContext
        val flow = ensureFlow(appCtx)
        flow.value = flow.value.copy(
            activeEvent = null,
            isExpanded = false,
            previewAutoHideMs = null
        )
        DynamicIslandOverlayService.stop(appCtx)
    }

    fun getCachedRealAppIcon(packageName: String): ImageBitmap? = realAppIconCache[packageName]

    suspend fun loadRealAppIconAsync(context: Context, packageName: String): ImageBitmap? {
        realAppIconCache[packageName]?.let { return it }
        return withContext(Dispatchers.IO) {
            runCatching {
                val appCtx = context.applicationContext
                val drawable = appCtx.packageManager.getApplicationIcon(packageName)
                val px = (40f * appCtx.resources.displayMetrics.density).roundToInt().coerceIn(48, 160)
                val bitmap = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, px, px)
                drawable.draw(canvas)
                val img = bitmap.asImageBitmap()
                realAppIconCache.putIfAbsent(packageName, img)
                realAppIconCache[packageName] ?: img
            }.getOrNull()
        }
    }

    private fun presentEvent(context: Context, event: DynamicIslandEvent) {
        val appCtx = context.applicationContext
        val flow = ensureFlow(appCtx)
        flow.value = flow.value.copy(
            activeEvent = event,
            isExpanded = false,
            previewAutoHideMs = null
        )
        // Launch system-wide top overlay when overlay permission is granted;
        // MainActivity also observes this flow as seamless in-app fallback.
        if (runCatching { Settings.canDrawOverlays(appCtx) }.getOrDefault(false)) {
            DynamicIslandOverlayService.show(appCtx)
        }
    }

    private fun ensureFlow(appCtx: Context): MutableStateFlow<DynamicIslandState> {
        _state?.let { return it }
        synchronized(this) {
            _state?.let { return it }
            val prefs = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val flow = MutableStateFlow(DynamicIslandState(config = readConfig(prefs)))
            _state = flow
            return flow
        }
    }

    private fun readConfig(prefs: SharedPreferences): DynamicIslandConfig {
        val width = runCatching {
            IslandWidthPreset.valueOf(prefs.getString(KEY_WIDTH_PRESET, IslandWidthPreset.STANDARD.name)!!)
        }.getOrDefault(IslandWidthPreset.STANDARD)
        return DynamicIslandConfig(
            enabled = prefs.getBoolean(KEY_ENABLED, true),
            enableNotificationSummary = prefs.getBoolean(KEY_NOTIF_SUMMARY, true),
            enableScheduleAlert = prefs.getBoolean(KEY_SCHEDULE_ALERT, true),
            enableMilestones = prefs.getBoolean(KEY_MILESTONES, true),
            widthPreset = width,
            compactHeightDp = prefs.getInt(KEY_COMPACT_HEIGHT, 38).coerceIn(32, 48),
            topOffsetDp = prefs.getInt(KEY_TOP_OFFSET, 8).coerceIn(0, 40),
            autoDismissSeconds = prefs.getInt(KEY_AUTO_DISMISS_SEC, 8).coerceIn(0, 20),
            notificationPersistUntilCleared = prefs.getBoolean(KEY_NOTIF_PERSIST, true),
            scheduleLeadMinutes = prefs.getInt(KEY_SCHEDULE_LEAD_MIN, 10).coerceIn(1, 30),
            useNothingMonoStyle = prefs.getBoolean(KEY_NOTHING_MONO, true),
            showStatusLed = prefs.getBoolean(KEY_STATUS_LED, true),
            useRealAppIcons = prefs.getBoolean(KEY_REAL_APP_ICONS, true),
            swipeUpToDismiss = prefs.getBoolean(KEY_SWIPE_DISMISS, true),
            enableHaptics = prefs.getBoolean(KEY_HAPTICS, true)
        )
    }

    private fun writeConfig(prefs: SharedPreferences, cfg: DynamicIslandConfig) {
        prefs.edit()
            .putBoolean(KEY_ENABLED, cfg.enabled)
            .putBoolean(KEY_NOTIF_SUMMARY, cfg.enableNotificationSummary)
            .putBoolean(KEY_SCHEDULE_ALERT, cfg.enableScheduleAlert)
            .putBoolean(KEY_MILESTONES, cfg.enableMilestones)
            .putString(KEY_WIDTH_PRESET, cfg.widthPreset.name)
            .putInt(KEY_COMPACT_HEIGHT, cfg.compactHeightDp)
            .putInt(KEY_TOP_OFFSET, cfg.topOffsetDp)
            .putInt(KEY_AUTO_DISMISS_SEC, cfg.autoDismissSeconds)
            .putBoolean(KEY_NOTIF_PERSIST, cfg.notificationPersistUntilCleared)
            .putInt(KEY_SCHEDULE_LEAD_MIN, cfg.scheduleLeadMinutes)
            .putBoolean(KEY_NOTHING_MONO, cfg.useNothingMonoStyle)
            .putBoolean(KEY_STATUS_LED, cfg.showStatusLed)
            .putBoolean(KEY_REAL_APP_ICONS, cfg.useRealAppIcons)
            .putBoolean(KEY_SWIPE_DISMISS, cfg.swipeUpToDismiss)
            .putBoolean(KEY_HAPTICS, cfg.enableHaptics)
            .apply()
    }
}
