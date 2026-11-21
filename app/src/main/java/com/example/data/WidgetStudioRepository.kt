package com.example.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Immutable
import com.example.widget.NothingFocusWidgetUpdater
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

enum class NothingWidgetKind(
    val id: String,
    val title: String,
    val shortTitle: String,
    val sizeBadge: String,
    val subtitle: String
) {
    GOAL_RING(
        id = "goal_ring",
        title = "Focus Goal Ring",
        shortTitle = "Ring",
        sizeBadge = "2×2",
        subtitle = "Dot-matrix daily goal ring, streak & rank"
    ),
    DOT_CLOCK(
        id = "dot_clock",
        title = "Dot-Matrix Focus Clock",
        shortTitle = "Clock",
        sizeBadge = "2×2",
        subtitle = "Bold Nothing dot numerals & 1-tap quick lock"
    ),
    HEATMAP_MATRIX(
        id = "heatmap_matrix",
        title = "Consistency Matrix",
        shortTitle = "Matrix",
        sizeBadge = "4×2",
        subtitle = "7-week Nothing dot consistency grid & streak"
    ),
    NEXT_SCHEDULE(
        id = "next_schedule",
        title = "Next Schedule",
        shortTitle = "Schedule",
        sizeBadge = "4×2",
        subtitle = "Upcoming scheduled lock window & countdown"
    ),
    GOAL_BAR(
        id = "goal_bar",
        title = "Daily Goal Capsule",
        shortTitle = "Goal Bar",
        sizeBadge = "4×1",
        subtitle = "Minimal pill progress bar & circular lock button"
    ),
    STREAK_RANK(
        id = "streak_rank",
        title = "Streak & Rank Badge",
        shortTitle = "Streak",
        sizeBadge = "2×2",
        subtitle = "Circular Nothing streak flame, best & XP tier"
    )
}

enum class WidgetCornerStyle(val label: String, val radiusDp: Int) {
    SOFT("Soft", 24),
    PILL("Curved", 32)
}

enum class WidgetSurfaceStyle(val label: String) {
    SOLID("Obsidian"),
    ELEVATED("Tonal"),
    GLASS("Frosted")
}

@Immutable
data class SingleWidgetConfig(
    val useNothingMono: Boolean = true,
    val cornerStyle: WidgetCornerStyle = WidgetCornerStyle.PILL,
    val surfaceStyle: WidgetSurfaceStyle = WidgetSurfaceStyle.SOLID,
    val showHeaderDots: Boolean = true,
    val showSecondaryMetric: Boolean = true,
    val enableQuickLockTap: Boolean = true,
    val use24HourClock: Boolean = false
)

@Immutable
data class WidgetStudioState(
    val configs: Map<NothingWidgetKind, SingleWidgetConfig> = NothingWidgetKind.entries.associateWith {
        SingleWidgetConfig()
    }
) {
    fun configFor(kind: NothingWidgetKind): SingleWidgetConfig = configs[kind] ?: SingleWidgetConfig()
}

object WidgetStudioRepository {
    private const val PREFS_NAME = "focus_widget_studio_prefs"
    private const val KEY_CONFIGS_JSON = "widget_configs_json"

    @Volatile
    private var _state: MutableStateFlow<WidgetStudioState>? = null

    fun observe(context: Context): StateFlow<WidgetStudioState> {
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

    fun getCurrent(context: Context): WidgetStudioState {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return readState(prefs)
    }

    fun getConfig(context: Context, kind: NothingWidgetKind): SingleWidgetConfig {
        return getCurrent(context).configFor(kind)
    }

    fun updateWidgetConfig(
        context: Context,
        kind: NothingWidgetKind,
        transform: (SingleWidgetConfig) -> SingleWidgetConfig
    ) {
        val appCtx = context.applicationContext
        synchronized(this) {
            val prefs = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val current = _state?.value ?: readState(prefs)
            val updatedMap = current.configs.toMutableMap()
            updatedMap[kind] = transform(current.configFor(kind))
            val next = WidgetStudioState(configs = updatedMap)
            writeState(prefs, next)
            if (_state != null) {
                _state?.value = next
            } else {
                _state = MutableStateFlow(next)
            }
        }
        NothingFocusWidgetUpdater.scheduleUpdateAll(appCtx)
    }

    private fun readState(prefs: SharedPreferences): WidgetStudioState {
        val raw = prefs.getString(KEY_CONFIGS_JSON, null)
        if (raw.isNullOrBlank()) return WidgetStudioState()
        return runCatching {
            val root = JSONObject(raw)
            val map = NothingWidgetKind.entries.associateWith { kind ->
                val obj = root.optJSONObject(kind.id)
                if (obj == null) {
                    SingleWidgetConfig()
                } else {
                    val corner = runCatching {
                        WidgetCornerStyle.valueOf(obj.optString("cornerStyle", WidgetCornerStyle.PILL.name))
                    }.getOrDefault(WidgetCornerStyle.PILL)
                    val surface = runCatching {
                        WidgetSurfaceStyle.valueOf(obj.optString("surfaceStyle", WidgetSurfaceStyle.SOLID.name))
                    }.getOrDefault(WidgetSurfaceStyle.SOLID)
                    SingleWidgetConfig(
                        useNothingMono = obj.optBoolean("useNothingMono", true),
                        cornerStyle = corner,
                        surfaceStyle = surface,
                        showHeaderDots = obj.optBoolean("showHeaderDots", true),
                        showSecondaryMetric = obj.optBoolean("showSecondaryMetric", true),
                        enableQuickLockTap = obj.optBoolean("enableQuickLockTap", true),
                        use24HourClock = obj.optBoolean("use24HourClock", false)
                    )
                }
            }
            WidgetStudioState(configs = map)
        }.getOrDefault(WidgetStudioState())
    }

    private fun writeState(prefs: SharedPreferences, state: WidgetStudioState) {
        val root = JSONObject()
        for ((kind, cfg) in state.configs) {
            root.put(
                kind.id,
                JSONObject().apply {
                    put("useNothingMono", cfg.useNothingMono)
                    put("cornerStyle", cfg.cornerStyle.name)
                    put("surfaceStyle", cfg.surfaceStyle.name)
                    put("showHeaderDots", cfg.showHeaderDots)
                    put("showSecondaryMetric", cfg.showSecondaryMetric)
                    put("enableQuickLockTap", cfg.enableQuickLockTap)
                    put("use24HourClock", cfg.use24HourClock)
                }
            )
        }
        prefs.edit().putString(KEY_CONFIGS_JSON, root.toString()).apply()
    }
}
