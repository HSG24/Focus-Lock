package com.example.data

import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import androidx.compose.runtime.Immutable
import com.example.BuildConfig

enum class WhatsNewCategory {
    NEW,
    FIX
}

@Immutable
data class WhatsNewItem(
    val title: String,
    val description: String,
    val category: WhatsNewCategory = WhatsNewCategory.NEW
)

@Immutable
data class WhatsNewRelease(
    val releaseId: Int,
    val versionLabel: String,
    val headline: String,
    val summary: String,
    val items: List<WhatsNewItem>
)

/**
 * Plain-language release notes shown automatically after an app update and kept
 * permanently accessible in Settings -> App Updates -> What's New.
 *
 * Maintenance rule: Whenever a new app update is prepared, update the active
 * [WhatsNewRelease] in [releases] with categorized NEW and FIX highlights
 * and increment [CURRENT_RELEASE_ID].
 */
object FocusWhatsNewCatalog {
    const val CURRENT_RELEASE_ID = 12

    val releases: List<WhatsNewRelease> = listOf(
        WhatsNewRelease(
            releaseId = 12,
            versionLabel = "Latest Update",
            headline = "Schedules, Nothing Widgets & Dynamic Island",
            summary = "Automated timer-only Focus Schedules with live Lock Studio & bottom console, 5 new Nothing-inspired widgets, a customizable Dynamic Island system, and shareable image recap cards.",
            items = listOf(
                WhatsNewItem(
                    title = "Schedule Tab & Custom Lock Studio",
                    description = "Added a dedicated Schedule tab for multiple daily, weekday, weekend, custom-day, or one-time focus schedules. Scheduled locks run in strict timer-only mode with a live dot-matrix Session Timeline, a customizable bottom focus dock (silenced counter, stay-locked check-in & unlock ETA), and a real-time Lock Studio.",
                    category = WhatsNewCategory.NEW
                ),
                WhatsNewItem(
                    title = "5 New Nothing-Inspired Home Widgets",
                    description = "Added Dot-Matrix Focus Clock, Consistency Matrix Heatmap, Next Schedule Countdown, Daily Goal Capsule Bar, and Streak & Rank Badge widgets. Every widget supports an independent Nothing Mono vs App Theme toggle, surface/corner styles, and live previews in Widget Studio.",
                    category = WhatsNewCategory.NEW
                ),
                WhatsNewItem(
                    title = "Nothing Dynamic Island & Minimal Studio",
                    description = "Added a dot-matrix Dynamic Island for Unlock Notification Summaries (with real full-color app icons on expand), Upcoming Schedule Alerts (with Skip Today inside the expanded card), and Goal & Streak Milestones — configured in a compact 3-tab studio that pops a live preview whenever you adjust size or look.",
                    category = WhatsNewCategory.NEW
                ),
                WhatsNewItem(
                    title = "Shareable Image Recap Cards",
                    description = "Added a Recap Image Card studio inside Share with Today, 7 Days, 30 Days, and Custom Date selection, customizable themes, Streak & Best Session cards, goal progress, and mini heatmap, exporting crisp PNG cards to any app.",
                    category = WhatsNewCategory.NEW
                )
            )
        )
    )
}

object FocusWhatsNewRepository {
    private const val PREFS_NAME = "focus_whats_new_prefs"
    private const val KEY_LAST_SEEN_RELEASE_ID = "last_seen_release_id"
    private const val KEY_LAST_SEEN_VERSION_NAME = "last_seen_version_name"
    private const val KEY_LAST_SEEN_VERSION_CODE = "last_seen_version_code"

    fun getInstalledVersionCode(context: Context): Long {
        val pkgInfo: PackageInfo? = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    android.content.pm.PackageManager.PackageInfoFlags.of(0L)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
        }.getOrNull()
        return if (pkgInfo != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pkgInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pkgInfo.versionCode.toLong()
            }
        } else {
            BuildConfig.VERSION_CODE.toLong()
        }
    }

    /**
     * Returns true when the app has updated since the user last dismissed the What's New sheet:
     * either the installed app version/code changed or [FocusWhatsNewCatalog.CURRENT_RELEASE_ID] advanced.
     */
    fun shouldShowAutoWhatsNew(
        context: Context,
        installedVersionName: String,
        installedVersionCode: Long = getInstalledVersionCode(context),
        currentReleaseId: Int = FocusWhatsNewCatalog.CURRENT_RELEASE_ID
    ): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val seenReleaseId = prefs.getInt(KEY_LAST_SEEN_RELEASE_ID, 0)
        if (seenReleaseId < currentReleaseId) {
            return true
        }
        val seenVersionName = prefs.getString(KEY_LAST_SEEN_VERSION_NAME, null)
        val seenVersionCode = prefs.getLong(KEY_LAST_SEEN_VERSION_CODE, -1L)
        if (seenVersionName != null && seenVersionName != installedVersionName.trim()) {
            return true
        }
        if (seenVersionCode >= 0L && seenVersionCode != installedVersionCode) {
            return true
        }
        return false
    }

    fun markWhatsNewSeen(
        context: Context,
        installedVersionName: String,
        installedVersionCode: Long = getInstalledVersionCode(context),
        currentReleaseId: Int = FocusWhatsNewCatalog.CURRENT_RELEASE_ID
    ) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_LAST_SEEN_RELEASE_ID, currentReleaseId)
            .putString(KEY_LAST_SEEN_VERSION_NAME, installedVersionName.trim())
            .putLong(KEY_LAST_SEEN_VERSION_CODE, installedVersionCode)
            .apply()
    }
}
