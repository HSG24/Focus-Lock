package com.example.data

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

enum class LockAppLayout(
    val title: String,
    val subtitle: String
) {
    PILL_ROW("Minimal Pills", "Sleek horizontal capsules"),
    GLASS_GRID("Bento Grid", "Clean 3-column icon tiles"),
    ICON_DOCK("Floating Dock", "Minimal icon-only bar"),
    COMPACT_LIST("Sleek List", "Grouped rows with arrows"),
    SPLIT_CHIPS("Split Chips", "2-column tonal badges")
}

data class AllowedApp(
    val id: String,
    val label: String,
    val packageName: String,
    val isPreset: Boolean = false
)

data class InstalledAppInfo(
    val label: String,
    val packageName: String
)

data class FocusSettingsState(
    val lockAppLayout: LockAppLayout = LockAppLayout.PILL_ROW,
    val allowedApps: List<AllowedApp> = FocusSettingsRepository.DEFAULT_ALLOWED_APPS,
    val dailyGoalMinutes: Long = 60L,
    val showSessionTimer: Boolean = true,
    val hapticKeypad: Boolean = true,
    val autoClearWrongPin: Boolean = true,
    val appSwitchGraceSeconds: Int = 2,
    val activeLiveShareId: String? = null,
    val activeLiveShareStartDateMs: Long = 0L
)

object FocusSettingsRepository {
    private const val PREFS_NAME = "focus_settings"
    private const val KEY_LOCK_LAYOUT = "lock_app_layout"
    private const val KEY_ALLOWED_APPS = "allowed_apps_json"
    private const val KEY_DAILY_GOAL = "daily_goal_min"
    private const val KEY_SHOW_TIMER = "show_session_timer"
    private const val KEY_HAPTIC_KEYPAD = "haptic_keypad"
    private const val KEY_AUTO_CLEAR_PIN = "auto_clear_wrong_pin"
    private const val KEY_GRACE_SECONDS = "app_switch_grace_sec"
    private const val KEY_ACTIVE_LIVE_SHARE_ID = "active_live_share_id"
    private const val KEY_ACTIVE_LIVE_SHARE_START_MS = "active_live_share_start_ms"

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
        val layoutName = prefs.getString(KEY_LOCK_LAYOUT, LockAppLayout.PILL_ROW.name)
        val lockLayout = runCatching { LockAppLayout.valueOf(layoutName ?: LockAppLayout.PILL_ROW.name) }
            .getOrDefault(LockAppLayout.PILL_ROW)

        val allowedApps = if (prefs.contains(KEY_ALLOWED_APPS)) {
            parseAllowedApps(prefs.getString(KEY_ALLOWED_APPS, "[]") ?: "[]")
        } else {
            DEFAULT_ALLOWED_APPS
        }

        return FocusSettingsState(
            lockAppLayout = lockLayout,
            allowedApps = allowedApps,
            dailyGoalMinutes = prefs.getLong(KEY_DAILY_GOAL, 60L).coerceIn(5L, 720L),
            showSessionTimer = prefs.getBoolean(KEY_SHOW_TIMER, true),
            hapticKeypad = prefs.getBoolean(KEY_HAPTIC_KEYPAD, true),
            autoClearWrongPin = prefs.getBoolean(KEY_AUTO_CLEAR_PIN, true),
            appSwitchGraceSeconds = prefs.getInt(KEY_GRACE_SECONDS, 2).coerceIn(1, 5),
            activeLiveShareId = prefs.getString(KEY_ACTIVE_LIVE_SHARE_ID, null)?.takeIf { it.isNotBlank() },
            activeLiveShareStartDateMs = prefs.getLong(KEY_ACTIVE_LIVE_SHARE_START_MS, 0L)
        )
    }

    fun setLockAppLayout(context: Context, layout: LockAppLayout) {
        update(context) { putString(KEY_LOCK_LAYOUT, layout.name) }
    }

    fun setDailyGoalMinutes(context: Context, minutes: Long) {
        update(context) { putLong(KEY_DAILY_GOAL, minutes.coerceIn(5L, 720L)) }
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

    fun loadAppIconBitmap(context: Context, packageName: String): ImageBitmap? {
        if (packageName.startsWith("__builtin_")) return null
        return try {
            val drawable = context.packageManager.getApplicationIcon(packageName)
            drawableToBitmap(drawable).asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 96
        val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 96
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
