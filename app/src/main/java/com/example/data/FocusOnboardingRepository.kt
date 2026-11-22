package com.example.data

import android.content.Context

/**
 * Tracks first-install Sanctuary Setup completion.
 *
 * Strictly differentiates brand-new installs from updates to an existing install:
 * if any prior app footprint exists (seen What's New history, cached sessions,
 * schedules, custom settings, or XP history), setup is automatically marked
 * completed so upgrading users are never interrupted by first-time onboarding.
 */
object FocusOnboardingRepository {
    private const val PREFS_NAME = "focus_onboarding_prefs"
    private const val KEY_SETUP_COMPLETED = "first_install_setup_completed"

    fun shouldShowFirstInstallSetup(context: Context): Boolean {
        val appCtx = context.applicationContext
        val prefs = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_SETUP_COMPLETED, false)) {
            return false
        }

        // Upgrade detection: check whether this install already has prior app state
        if (hasExistingUserFootprint(appCtx)) {
            prefs.edit().putBoolean(KEY_SETUP_COMPLETED, true).apply()
            return false
        }

        return true
    }

    fun markSetupCompleted(context: Context) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SETUP_COMPLETED, true)
            .apply()
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
