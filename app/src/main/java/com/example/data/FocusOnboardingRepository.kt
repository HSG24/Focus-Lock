package com.example.data

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build

/**
 * Tracks first-install Welcome Setup completion.
 *
 * Strictly differentiates brand-new installations (no prior version of FocusLock
 * installed on the phone) from in-place updates over an already-installed version:
 *  - Brand-new install (`firstInstallTime == lastUpdateTime`, no prior data):
 *    shows the setup flow and keeps it active across activity recreations until completed.
 *  - Update over an existing install (`lastUpdateTime > firstInstallTime`,
 *    `MY_PACKAGE_REPLACED`, or existing app data): permanently marks setup completed
 *    so upgrading users are never interrupted.
 */
object FocusOnboardingRepository {
    private const val PREFS_NAME = "focus_onboarding_prefs"
    private const val KEY_SETUP_COMPLETED = "first_install_setup_completed"
    private const val KEY_SETUP_PENDING_INSTALL_TIME = "first_install_setup_pending_time"

    fun shouldShowFirstInstallSetup(context: Context): Boolean {
        val appCtx = context.applicationContext
        val prefs = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_SETUP_COMPLETED, false)) {
            return false
        }

        val pkgInfo = getPackageInfoCompat(appCtx)
        val firstInstallTime = pkgInfo?.firstInstallTime ?: 0L
        val lastUpdateTime = pkgInfo?.lastUpdateTime ?: 0L

        // If this fresh install already started setup (e.g., user picked a theme or went to
        // Android Settings to grant permissions and Activity recreated), keep showing setup
        // until the user explicitly finishes or restores a backup.
        val pendingInstallTime = prefs.getLong(KEY_SETUP_PENDING_INSTALL_TIME, -1L)
        if (pendingInstallTime > 0L && pendingInstallTime == firstInstallTime) {
            return true
        }

        // Authoritative OS check: Android sets lastUpdateTime > firstInstallTime whenever
        // an APK is installed on top of an already-installed version of the app.
        val isUpdatedOverExistingApp = firstInstallTime > 0L && lastUpdateTime > firstInstallTime
        if (isUpdatedOverExistingApp || hasExistingUserFootprint(appCtx)) {
            prefs.edit()
                .putBoolean(KEY_SETUP_COMPLETED, true)
                .remove(KEY_SETUP_PENDING_INSTALL_TIME)
                .apply()
            return false
        }

        // Genuine fresh install: record pending install timestamp so mid-setup setting
        // changes (theme/goal) won't accidentally look like an existing install on recreate.
        if (firstInstallTime > 0L) {
            prefs.edit()
                .putLong(KEY_SETUP_PENDING_INSTALL_TIME, firstInstallTime)
                .apply()
        }
        return true
    }

    fun markSetupCompleted(context: Context) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SETUP_COMPLETED, true)
            .remove(KEY_SETUP_PENDING_INSTALL_TIME)
            .apply()
    }

    private fun getPackageInfoCompat(appCtx: Context): PackageInfo? {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                appCtx.packageManager.getPackageInfo(
                    appCtx.packageName,
                    PackageManager.PackageInfoFlags.of(0L)
                )
            } else {
                @Suppress("DEPRECATION")
                appCtx.packageManager.getPackageInfo(appCtx.packageName, 0)
            }
        }.getOrNull()
    }

    private fun hasExistingUserFootprint(appCtx: Context): Boolean {
        val whatsNewPrefs = appCtx.getSharedPreferences("focus_whats_new_prefs", Context.MODE_PRIVATE)
        if (whatsNewPrefs.getInt("last_seen_release_id", 0) > 0) return true

        val settingsPrefs = appCtx.getSharedPreferences("focus_settings", Context.MODE_PRIVATE)
        if (settingsPrefs.all.isNotEmpty()) return true

        val statsPrefs = appCtx.getSharedPreferences("focus_stats_cache", Context.MODE_PRIVATE)
        val cachedSessions = statsPrefs.getString("cached_sessions_json", null)
        if (!cachedSessions.isNullOrBlank() && cachedSessions != "[]") return true

        val schedulePrefs = appCtx.getSharedPreferences("focus_schedules_prefs", Context.MODE_PRIVATE)
        val schedules = schedulePrefs.getString("schedules_json", null)
        if (!schedules.isNullOrBlank() && schedules != "[]") return true

        val xpPrefs = appCtx.getSharedPreferences("focus_xp_prefs", Context.MODE_PRIVATE)
        if (xpPrefs.getInt("total_xp", 0) > 0) return true

        return false
    }
}
