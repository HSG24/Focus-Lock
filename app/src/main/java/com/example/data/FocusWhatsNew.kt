package com.example.data

import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import androidx.compose.runtime.Immutable
import com.example.BuildConfig

@Immutable
data class WhatsNewItem(
    val title: String,
    val description: String
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
 * Maintenance rule: Whenever a new app update is prepared, prepend a new
 * [WhatsNewRelease] at the top of [releases] with an incremented [CURRENT_RELEASE_ID]
 * written in simple, everyday language.
 */
object FocusWhatsNewCatalog {
    const val CURRENT_RELEASE_ID = 3

    val releases: List<WhatsNewRelease> = listOf(
        WhatsNewRelease(
            releaseId = 3,
            versionLabel = "Latest Update",
            headline = "Movable Lock Button & Smarter Notifications",
            summary = "Move your Back to Lock button wherever you want and enjoy a cleaner, expandable notification summary.",
            items = listOf(
                WhatsNewItem(
                    title = "Movable Back to Lock Button",
                    description = "When you open an allowed app while locked, you can now drag the Back to Lock button anywhere on the screen so it never blocks what you're doing."
                ),
                WhatsNewItem(
                    title = "No More Repeated Notifications",
                    description = "Fixed an issue where the same notification could appear 20+ times in your post-focus summary. Duplicate alerts from the same app are now combined cleanly."
                ),
                WhatsNewItem(
                    title = "Expand & Collapse More Notifications",
                    description = "Tap '+N more notifications' on any app in your summary to view every missed alert, then tap 'Show less' to collapse it back down."
                )
            )
        ),
        WhatsNewRelease(
            releaseId = 2,
            versionLabel = "Previous Update",
            headline = "Cleaner Charts, Custom Pills & What's New",
            summary = "Easier-to-read focus graphs, smarter chart scaling, and adjustable lock screen buttons.",
            items = listOf(
                WhatsNewItem(
                    title = "Only Real Progress Up to Today",
                    description = "Weekly and monthly charts now hide upcoming days and future weeks until they begin, keeping your view focused on what you've actually done."
                ),
                WhatsNewItem(
                    title = "Calendar-Aligned Weeks & Clearer Bars",
                    description = "Months are neatly grouped into calendar weeks inside each month, and shorter sessions automatically scale taller so every focus block stands out."
                ),
                WhatsNewItem(
                    title = "Tap Any Bar for Details",
                    description = "Tap a chart bar to check sessions and time for that period below. Tap the same bar again—or anywhere else—to return to your full summary."
                ),
                WhatsNewItem(
                    title = "Adjustable Minimal Pill Size",
                    description = "Pick your preferred button size from compact to large using the new slider in Settings -> Lock Screen Apps Layout."
                ),
                WhatsNewItem(
                    title = "What's New Guide",
                    description = "Every update now shares a quick, plain-English overview of what changed, and you can revisit it anytime in Settings."
                )
            )
        ),
        WhatsNewRelease(
            releaseId = 1,
            versionLabel = "Previous Update",
            headline = "Themes, Ranks & Offline Sync",
            summary = "Personalize your focus space and keep your stats backed up automatically.",
            items = listOf(
                WhatsNewItem(
                    title = "4 Visual Themes",
                    description = "Switch between Nothing Crimson, Cyber Ice, Solar Amber, and Ultraviolet Pulse across your lock screen, charts, and home widget."
                ),
                WhatsNewItem(
                    title = "Works Offline & Syncs Back",
                    description = "Your focus sessions save on your phone right away even without internet and sync automatically once you're back online."
                ),
                WhatsNewItem(
                    title = "Streaks & Rank Ladder",
                    description = "Track your current streak alongside your all-time best and check your XP rank anytime from the top pill in Stats."
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
