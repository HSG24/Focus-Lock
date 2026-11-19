package com.example.data

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import com.example.ui.theme.AppVisualTheme
import com.example.ui.theme.FocusThemePalette
import com.example.widget.NothingFocusWidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

enum class LockAppLayout(
    val title: String,
    val subtitle: String
) {
    PILL_ROW("Minimal Pills", "Sleek horizontal capsules"),
    ICON_DOCK("Floating Dock", "Minimal icon-only bar"),
    COMPACT_LIST("Sleek List", "Grouped rows with arrows"),
    SPLIT_CHIPS("Split Chips", "2-column tonal badges")
}

@Immutable
data class AllowedApp(
    val id: String,
    val label: String,
    val packageName: String,
    val isPreset: Boolean = false
)

@Immutable
data class InstalledAppInfo(
    val label: String,
    val packageName: String
)

@Immutable
data class FocusSettingsState(
    val appTheme: AppVisualTheme = AppVisualTheme.LIQUID_GLASS,
    val lockAppLayout: LockAppLayout = LockAppLayout.PILL_ROW,
    val minimalPillButtonSizeDp: Int = FocusSettingsRepository.DEFAULT_PILL_BUTTON_SIZE_DP,
    val allowedApps: List<AllowedApp> = FocusSettingsRepository.DEFAULT_ALLOWED_APPS,
    val dailyGoalMinutes: Long = 240L,
    val showSessionTimer: Boolean = true,
    val hapticKeypad: Boolean = true,
    val autoClearWrongPin: Boolean = true,
    val appSwitchGraceSeconds: Int = 2,
    val blockNotificationsDuringLock: Boolean = false,
    val blockPowerMenuDuringLock: Boolean = true,
    val widgetShowDottedRing: Boolean = true,
    val widgetShowStudyHours: Boolean = true,
    val widgetShowGoalPercent: Boolean = true,
    val widgetShowStreakBadge: Boolean = true,
    val widgetShowRankXpTag: Boolean = true,
    val activeLiveShareId: String? = null,
    val activeLiveShareStartDateMs: Long = 0L
)

object FocusSettingsRepository {
    private const val PREFS_NAME = "focus_settings"
    private const val KEY_APP_THEME = "app_visual_theme"
    private const val KEY_LOCK_LAYOUT = "lock_app_layout"
    private const val KEY_MINIMAL_PILL_SIZE_DP = "minimal_pill_button_size_dp"
    private const val KEY_ALLOWED_APPS = "allowed_apps_json"
    private const val KEY_DAILY_GOAL = "daily_goal_min"
    private const val KEY_SHOW_TIMER = "show_session_timer"
    private const val KEY_HAPTIC_KEYPAD = "haptic_keypad"
    private const val KEY_AUTO_CLEAR_PIN = "auto_clear_wrong_pin"
    private const val KEY_GRACE_SECONDS = "app_switch_grace_sec"
    private const val KEY_BLOCK_NOTIFICATIONS = "block_notifications_during_lock"
    private const val KEY_BLOCK_POWER_MENU = "block_power_menu_during_lock"
    private const val KEY_WIDGET_DOTTED_RING = "widget_show_dotted_ring"
    private const val KEY_WIDGET_STUDY_HOURS = "widget_show_study_hours"
    private const val KEY_WIDGET_GOAL_PERCENT = "widget_show_goal_percent"
    private const val KEY_WIDGET_STREAK_BADGE = "widget_show_streak_badge"
    private const val KEY_WIDGET_RANK_XP = "widget_show_rank_xp"
    private const val KEY_ACTIVE_LIVE_SHARE_ID = "active_live_share_id"
    private const val KEY_ACTIVE_LIVE_SHARE_START_MS = "active_live_share_start_ms"

    const val MIN_PILL_BUTTON_SIZE_DP = 28
    const val MAX_PILL_BUTTON_SIZE_DP = 52
    const val DEFAULT_PILL_BUTTON_SIZE_DP = 40

    const val PRESET_PHONE_PKG = "__builtin_phone__"
    const val PRESET_GOOGLE_PKG = "__builtin_google__"
    const val PRESET_XIAOMI_PKG = "__builtin_xiaomi__"

    val DEFAULT_ALLOWED_APPS = listOf(
        AllowedApp(id = "preset_phone", label = "Phone", packageName = PRESET_PHONE_PKG, isPreset = true),
        AllowedApp(id = "preset_google", label = "Google", packageName = PRESET_GOOGLE_PKG, isPreset = true)
    )

    val PHONE_PACKAGES = setOf(
        "com.google.android.dialer",
        "com.android.dialer",
        "com.android.phone",
        "com.samsung.android.dialer",
        "com.samsung.android.incallui",
        "com.android.contacts",
        "com.android.incallui"
    )

    val GOOGLE_PACKAGES = setOf(
        "com.google.android.googlequicksearchbox",
        "com.google.ar.lens",
        "com.google.lens"
    )

    val XIAOMI_PACKAGES = setOf(
        "com.xiaomi.smarthome",
        "com.xiaomi.mihome"
    )

    @Volatile
    private var _state: MutableStateFlow<FocusSettingsState>? = null

    @Volatile
    private var listener: SharedPreferences.OnSharedPreferenceChangeListener? = null

    private val appIconMap = ConcurrentHashMap<String, ImageBitmap>()
    private val _appIconsState = MutableStateFlow<Map<String, ImageBitmap>>(emptyMap())
    val appIconsState: StateFlow<Map<String, ImageBitmap>> = _appIconsState.asStateFlow()
    private val missingAppIconPackages = Collections.synchronizedSet(HashSet<String>())

    fun observe(context: Context): StateFlow<FocusSettingsState> {
        val appContext = context.applicationContext
        val existing = _state
        if (existing != null) return existing.asStateFlow()

        synchronized(this) {
            val doubleCheck = _state
            if (doubleCheck != null) return doubleCheck.asStateFlow()

            val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val flow = MutableStateFlow(readState(prefs))
            val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { sp, _ ->
                flow.value = readState(sp)
            }
            prefs.registerOnSharedPreferenceChangeListener(prefListener)
            listener = prefListener
            _state = flow
            return flow.asStateFlow()
        }
    }

    fun getCurrent(context: Context): FocusSettingsState {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return readState(prefs)
    }

    private fun readState(prefs: SharedPreferences): FocusSettingsState {
        val themeRaw = prefs.getString(KEY_APP_THEME, AppVisualTheme.LIQUID_GLASS.name)
        val appTheme = AppVisualTheme.fromIdOrName(themeRaw)

        val layoutName = prefs.getString(KEY_LOCK_LAYOUT, LockAppLayout.PILL_ROW.name)
        val lockLayout = runCatching { LockAppLayout.valueOf(layoutName ?: LockAppLayout.PILL_ROW.name) }
            .getOrDefault(LockAppLayout.PILL_ROW)

        val pillSizeDp = prefs.getInt(KEY_MINIMAL_PILL_SIZE_DP, DEFAULT_PILL_BUTTON_SIZE_DP)
            .coerceIn(MIN_PILL_BUTTON_SIZE_DP, MAX_PILL_BUTTON_SIZE_DP)

        val allowedApps = if (prefs.contains(KEY_ALLOWED_APPS)) {
            parseAllowedApps(prefs.getString(KEY_ALLOWED_APPS, "[]") ?: "[]")
        } else {
            DEFAULT_ALLOWED_APPS
        }

        return FocusSettingsState(
            appTheme = appTheme,
            lockAppLayout = lockLayout,
            minimalPillButtonSizeDp = pillSizeDp,
            allowedApps = allowedApps,
            dailyGoalMinutes = prefs.getLong(KEY_DAILY_GOAL, 240L).coerceIn(5L, 720L),
            showSessionTimer = prefs.getBoolean(KEY_SHOW_TIMER, true),
            hapticKeypad = prefs.getBoolean(KEY_HAPTIC_KEYPAD, true),
            autoClearWrongPin = prefs.getBoolean(KEY_AUTO_CLEAR_PIN, true),
            appSwitchGraceSeconds = prefs.getInt(KEY_GRACE_SECONDS, 2).coerceIn(1, 5),
            blockNotificationsDuringLock = prefs.getBoolean(KEY_BLOCK_NOTIFICATIONS, false),
            blockPowerMenuDuringLock = prefs.getBoolean(KEY_BLOCK_POWER_MENU, true),
            widgetShowDottedRing = prefs.getBoolean(KEY_WIDGET_DOTTED_RING, true),
            widgetShowStudyHours = prefs.getBoolean(KEY_WIDGET_STUDY_HOURS, true),
            widgetShowGoalPercent = prefs.getBoolean(KEY_WIDGET_GOAL_PERCENT, true),
            widgetShowStreakBadge = prefs.getBoolean(KEY_WIDGET_STREAK_BADGE, true),
            widgetShowRankXpTag = prefs.getBoolean(KEY_WIDGET_RANK_XP, true),
            activeLiveShareId = prefs.getString(KEY_ACTIVE_LIVE_SHARE_ID, null)?.takeIf { it.isNotBlank() },
            activeLiveShareStartDateMs = prefs.getLong(KEY_ACTIVE_LIVE_SHARE_START_MS, 0L)
        )
    }

    fun setAppTheme(context: Context, theme: AppVisualTheme) {
        update(context) { putString(KEY_APP_THEME, theme.name) }
        NothingFocusWidgetUpdater.scheduleUpdateAll(context)
    }

    fun setLockAppLayout(context: Context, layout: LockAppLayout) {
        update(context) { putString(KEY_LOCK_LAYOUT, layout.name) }
    }

    fun setMinimalPillButtonSizeDp(context: Context, sizeDp: Int) {
        val clamped = sizeDp.coerceIn(MIN_PILL_BUTTON_SIZE_DP, MAX_PILL_BUTTON_SIZE_DP)
        update(context) { putInt(KEY_MINIMAL_PILL_SIZE_DP, clamped) }
    }

    fun setDailyGoalMinutes(context: Context, minutes: Long) {
        update(context) { putLong(KEY_DAILY_GOAL, minutes.coerceIn(5L, 720L)) }
        NothingFocusWidgetUpdater.scheduleUpdateAll(context)
    }

    fun setShowSessionTimer(context: Context, enabled: Boolean) {
        update(context) { putBoolean(KEY_SHOW_TIMER, enabled) }
    }

    fun setHapticKeypad(context: Context, enabled: Boolean) {
        update(context) { putBoolean(KEY_HAPTIC_KEYPAD, enabled) }
    }

    fun setAutoClearWrongPin(context: Context, enabled: Boolean) {
        update(context) { putBoolean(KEY_AUTO_CLEAR_PIN, enabled) }
    }

    fun setAppSwitchGraceSeconds(context: Context, seconds: Int) {
        update(context) { putInt(KEY_GRACE_SECONDS, seconds.coerceIn(1, 5)) }
    }

    fun setBlockNotificationsDuringLock(context: Context, enabled: Boolean) {
        update(context) { putBoolean(KEY_BLOCK_NOTIFICATIONS, enabled) }
    }

    fun setBlockPowerMenuDuringLock(context: Context, enabled: Boolean) {
        update(context) { putBoolean(KEY_BLOCK_POWER_MENU, enabled) }
    }

    fun setWidgetShowDottedRing(context: Context, enabled: Boolean) {
        update(context) { putBoolean(KEY_WIDGET_DOTTED_RING, enabled) }
        NothingFocusWidgetUpdater.scheduleUpdateAll(context)
    }

    fun setWidgetShowStudyHours(context: Context, enabled: Boolean) {
        update(context) { putBoolean(KEY_WIDGET_STUDY_HOURS, enabled) }
        NothingFocusWidgetUpdater.scheduleUpdateAll(context)
    }

    fun setWidgetShowGoalPercent(context: Context, enabled: Boolean) {
        update(context) { putBoolean(KEY_WIDGET_GOAL_PERCENT, enabled) }
        NothingFocusWidgetUpdater.scheduleUpdateAll(context)
    }

    fun setWidgetShowStreakBadge(context: Context, enabled: Boolean) {
        update(context) { putBoolean(KEY_WIDGET_STREAK_BADGE, enabled) }
        NothingFocusWidgetUpdater.scheduleUpdateAll(context)
    }

    fun setWidgetShowRankXpTag(context: Context, enabled: Boolean) {
        update(context) { putBoolean(KEY_WIDGET_RANK_XP, enabled) }
        NothingFocusWidgetUpdater.scheduleUpdateAll(context)
    }

    fun setActiveLiveShare(context: Context, shareId: String?, startDateMs: Long) {
        update(context) {
            if (shareId.isNullOrBlank()) {
                remove(KEY_ACTIVE_LIVE_SHARE_ID)
                remove(KEY_ACTIVE_LIVE_SHARE_START_MS)
            } else {
                putString(KEY_ACTIVE_LIVE_SHARE_ID, shareId)
                putLong(KEY_ACTIVE_LIVE_SHARE_START_MS, startDateMs)
            }
        }
    }

    fun clearActiveLiveShare(context: Context) {
        setActiveLiveShare(context, null, 0L)
    }

    fun addAllowedApp(context: Context, app: AllowedApp) {
        val current = getCurrent(context).allowedApps
        if (current.any { it.packageName == app.packageName }) return
        val updated = current + app
        saveAllowedApps(context, updated)
    }

    fun removeAllowedApp(context: Context, appIdOrPkg: String) {
        val current = getCurrent(context).allowedApps
        val updated = current.filterNot { it.id == appIdOrPkg || it.packageName == appIdOrPkg }
        saveAllowedApps(context, updated)
    }

    fun resetAllowedAppsToDefaults(context: Context) {
        saveAllowedApps(context, DEFAULT_ALLOWED_APPS)
    }

    private fun saveAllowedApps(context: Context, apps: List<AllowedApp>) {
        val json = serializeAllowedApps(apps)
        update(context) { putString(KEY_ALLOWED_APPS, json) }
    }

    private inline fun update(context: Context, block: SharedPreferences.Editor.() -> Unit) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply(block).apply()
        _state?.value = readState(prefs)
    }

    private fun serializeAllowedApps(apps: List<AllowedApp>): String {
        val array = JSONArray()
        for (app in apps) {
            val obj = JSONObject().apply {
                put("id", app.id)
                put("label", app.label)
                put("packageName", app.packageName)
                put("isPreset", app.isPreset)
            }
            array.put(obj)
        }
        return array.toString()
    }

    private fun parseAllowedApps(raw: String): List<AllowedApp> {
        return try {
            val array = JSONArray(raw)
            val result = mutableListOf<AllowedApp>()
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val id = obj.optString("id")
                val label = obj.optString("label")
                val pkg = obj.optString("packageName")
                val isPreset = obj.optBoolean("isPreset", false)
                if (id.isNotBlank() && label.isNotBlank() && pkg.isNotBlank()) {
                    if (id == "preset_xiaomi" || pkg == PRESET_XIAOMI_PKG) continue
                    result.add(AllowedApp(id = id, label = label, packageName = pkg, isPreset = isPreset))
                }
            }
            result
        } catch (e: Exception) {
            DEFAULT_ALLOWED_APPS
        }
    }

    suspend fun loadLaunchableApps(context: Context): List<InstalledAppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = try {
            pm.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
        } catch (e: Exception) {
            pm.queryIntentActivities(launcherIntent, 0)
        }

        val ownPkg = context.packageName
        resolveInfos
            .mapNotNull { info ->
                val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
                if (pkg == ownPkg) return@mapNotNull null
                val label = info.loadLabel(pm)?.toString()?.trim().orEmpty().ifBlank { pkg }
                InstalledAppInfo(label = label, packageName = pkg)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    /**
     * Theme-independent cache key so monochrome icon alpha masks are decoded once per package
     * and tinted instantaneously on the GPU whenever the active theme changes.
     */
    fun iconCacheKey(packageName: String, @Suppress("UNUSED_PARAMETER") theme: AppVisualTheme = AppVisualTheme.LIQUID_GLASS): String =
        packageName

    fun getCachedAppIconBitmap(
        packageName: String,
        theme: AppVisualTheme = AppVisualTheme.LIQUID_GLASS
    ): ImageBitmap? {
        if (packageName.startsWith("__builtin_")) return null
        return appIconMap[iconCacheKey(packageName, theme)]
    }

    fun loadAppIconBitmap(
        context: Context,
        packageName: String,
        theme: AppVisualTheme = getCurrent(context).appTheme
    ): ImageBitmap? {
        if (packageName.startsWith("__builtin_") || missingAppIconPackages.contains(packageName)) {
            return null
        }
        val key = iconCacheKey(packageName, theme)
        appIconMap[key]?.let { return it }
        return try {
            val appCtx = context.applicationContext
            val targetPx = (48f * appCtx.resources.displayMetrics.density).roundToInt().coerceIn(64, 192)
            val drawable = appCtx.packageManager.getApplicationIcon(packageName)
            val imageBitmap = toMonochromeTintedBitmap(drawable, targetPx, android.graphics.Color.WHITE).asImageBitmap()
            appIconMap.putIfAbsent(key, imageBitmap)
            val cached = appIconMap[key] ?: imageBitmap
            _appIconsState.value = HashMap(appIconMap)
            cached
        } catch (_: Exception) {
            missingAppIconPackages.add(packageName)
            null
        }
    }

    suspend fun loadAppIconBitmapAsync(
        context: Context,
        packageName: String,
        theme: AppVisualTheme = getCurrent(context).appTheme
    ): ImageBitmap? {
        if (packageName.startsWith("__builtin_") || missingAppIconPackages.contains(packageName)) {
            return null
        }
        val key = iconCacheKey(packageName, theme)
        appIconMap[key]?.let { return it }
        return withContext(Dispatchers.IO) {
            loadAppIconBitmap(context.applicationContext, packageName, theme)
        }
    }

    suspend fun preloadAppIcons(
        context: Context,
        apps: List<AllowedApp>,
        theme: AppVisualTheme = getCurrent(context).appTheme
    ) = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val targetPx = (48f * appContext.resources.displayMetrics.density).roundToInt().coerceIn(64, 192)
        val pm = appContext.packageManager
        var addedAny = false
        for (app in apps) {
            val pkg = app.packageName
            val key = iconCacheKey(pkg, theme)
            if (!pkg.startsWith("__builtin_") &&
                !appIconMap.containsKey(key) &&
                !missingAppIconPackages.contains(pkg)
            ) {
                try {
                    val drawable = pm.getApplicationIcon(pkg)
                    val imageBitmap = toMonochromeTintedBitmap(drawable, targetPx, android.graphics.Color.WHITE).asImageBitmap()
                    if (appIconMap.putIfAbsent(key, imageBitmap) == null) {
                        addedAny = true
                    }
                } catch (_: Exception) {
                    missingAppIconPackages.add(pkg)
                }
            }
        }
        if (addedAny) {
            _appIconsState.value = HashMap(appIconMap)
        }
    }

    suspend fun canDrawOverlaysAsync(context: Context): Boolean = withContext(Dispatchers.IO) {
        runCatching { Settings.canDrawOverlays(context.applicationContext) }.getOrDefault(false)
    }

    suspend fun hasUsageStatsPermissionAsync(context: Context): Boolean = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        runCatching {
            val appOps = appContext.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), appContext.packageName)
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), appContext.packageName)
            }
            mode == AppOpsManager.MODE_ALLOWED
        }.getOrDefault(false)
    }

    internal fun toMonochromeTintedBitmap(
        drawable: Drawable,
        targetPx: Int,
        accentColorArgb: Int
    ): Bitmap {
        val size = targetPx.coerceAtLeast(32)

        // 1. Prefer Android 13+ AdaptiveIconDrawable.monochrome native vector layer when present
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && drawable is AdaptiveIconDrawable) {
            drawable.monochrome?.let { monoLayer ->
                val rawMono = renderDrawableLayer(monoLayer, size, expandAdaptiveSafeZone = true)
                return tintAlphaMaskBitmap(rawMono, accentColorArgb)
            }
        }

        // 2. For AdaptiveIconDrawable without monochrome layer, isolate its foreground glyph layer
        //    instead of flattening its solid background plate.
        val rawBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            drawable is AdaptiveIconDrawable &&
            drawable.foreground != null
        ) {
            renderDrawableLayer(drawable.foreground, size, expandAdaptiveSafeZone = true)
        } else {
            renderDrawableLayer(drawable, size, expandAdaptiveSafeZone = false)
        }

        return extractSilhouetteAndTint(rawBitmap, accentColorArgb)
    }

    private fun renderDrawableLayer(
        drawable: Drawable,
        size: Int,
        expandAdaptiveSafeZone: Boolean
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val pad = if (expandAdaptiveSafeZone) (size * 0.22f).roundToInt() else 0
        val oldBounds = drawable.copyBounds()
        drawable.setBounds(-pad, -pad, size + pad, size + pad)
        drawable.draw(canvas)
        drawable.bounds = oldBounds
        return bitmap
    }

    private fun tintAlphaMaskBitmap(maskBitmap: Bitmap, accentColorArgb: Int): Bitmap {
        val size = maskBitmap.width
        val output = Bitmap.createBitmap(size, maskBitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = PorterDuffColorFilter(accentColorArgb, PorterDuff.Mode.SRC_IN)
        }
        canvas.drawBitmap(maskBitmap, 0f, 0f, paint)
        return output
    }

    private fun extractSilhouetteAndTint(src: Bitmap, accentColorArgb: Int): Bitmap {
        val w = src.width
        val h = src.height
        val total = w * h
        if (total == 0) return src

        val pixels = IntArray(total)
        src.getPixels(pixels, 0, w, 0, 0, w, h)

        val lums = FloatArray(total)
        val alphas = IntArray(total)
        val hist = IntArray(256)
        var opaqueCount = 0

        for (i in 0 until total) {
            val c = pixels[i]
            val a = (c ushr 24) and 0xFF
            alphas[i] = a
            if (a > 24) {
                val r = (c ushr 16) and 0xFF
                val g = (c ushr 8) and 0xFF
                val b = c and 0xFF
                val lum = (0.299f * r + 0.587f * g + 0.114f * b).coerceIn(0f, 255f)
                lums[i] = lum
                hist[lum.toInt().coerceIn(0, 255)]++
                opaqueCount++
            }
        }

        if (opaqueCount == 0) {
            return Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        }

        val opaqueFraction = opaqueCount.toFloat() / total.toFloat()

        // Compute Otsu threshold on opaque pixels to check whether this icon is an opaque badge
        // enclosing an inner foreground glyph (common in legacy icons or boxed adaptive foregrounds).
        var sumAll = 0.0
        for (bin in 0..255) sumAll += bin * hist[bin].toDouble()

        var sumBg = 0.0
        var weightBg = 0
        var bestVar = -1.0
        var threshold = 128f
        var bestMeanDiff = 0.0

        for (bin in 0..255) {
            weightBg += hist[bin]
            if (weightBg == 0) continue
            val weightFg = opaqueCount - weightBg
            if (weightFg == 0) break

            sumBg += bin * hist[bin].toDouble()
            val meanBg = sumBg / weightBg
            val meanFg = (sumAll - sumBg) / weightFg
            val diff = meanBg - meanFg
            val betweenVar = weightBg.toDouble() * weightFg.toDouble() * diff * diff
            if (betweenVar > bestVar) {
                bestVar = betweenVar
                threshold = ((meanBg + meanFg) * 0.5).toFloat()
                bestMeanDiff = kotlin.math.abs(diff)
            }
        }

        var useLuminanceForegroundCutout = false
        var keepBrightSide = true

        if (opaqueFraction > 0.42f && bestMeanDiff > 58.0) {
            // Inspect outer-ring opaque pixels to identify the badge background luminance side.
            var edgeTotal = 0
            var edgeBright = 0
            val marginX = (w * 0.22f).roundToInt().coerceAtLeast(2)
            val marginY = (h * 0.22f).roundToInt().coerceAtLeast(2)

            for (y in 0 until h) {
                for (x in 0 until w) {
                    val idx = y * w + x
                    if (alphas[idx] <= 24) continue
                    val inOuterBand = x < marginX || x >= w - marginX || y < marginY || y >= h - marginY
                    if (inOuterBand) {
                        edgeTotal++
                        if (lums[idx] >= threshold) edgeBright++
                    }
                }
            }

            if (edgeTotal > 12) {
                val edgeBrightRatio = edgeBright.toFloat() / edgeTotal.toFloat()
                if (edgeBrightRatio >= 0.72f) {
                    useLuminanceForegroundCutout = true
                    keepBrightSide = false // Bright background badge, dark inner glyph
                } else if (edgeBrightRatio <= 0.28f) {
                    useLuminanceForegroundCutout = true
                    keepBrightSide = true // Dark/colored background badge, bright inner glyph
                }
            }
        }

        val rAccent = (accentColorArgb ushr 16) and 0xFF
        val gAccent = (accentColorArgb ushr 8) and 0xFF
        val bAccent = accentColorArgb and 0xFF
        val rgbAccent = (rAccent shl 16) or (gAccent shl 8) or bAccent
        val outPixels = IntArray(total)

        for (i in 0 until total) {
            val a = alphas[i]
            if (a <= 4) {
                outPixels[i] = 0
                continue
            }
            val finalAlpha = if (useLuminanceForegroundCutout) {
                val lum = lums[i]
                val maskFactor = if (keepBrightSide) {
                    ((lum - (threshold - 14f)) / 28f).coerceIn(0f, 1f)
                } else {
                    (((threshold + 14f) - lum) / 28f).coerceIn(0f, 1f)
                }
                (a * maskFactor).roundToInt().coerceIn(0, 255)
            } else {
                a
            }
            outPixels[i] = if (finalAlpha > 0) (finalAlpha shl 24) or rgbAccent else 0
        }

        return Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply {
            setPixels(outPixels, 0, w, 0, 0, w, h)
        }
    }
}
