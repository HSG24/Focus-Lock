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
    const val CURRENT_RELEASE_ID = 14

    val releases: List<WhatsNewRelease> = listOf(
        WhatsNewRelease(
            releaseId = 14,
            versionLabel = "Latest Update",
            headline = "Completion Success Screen & Cleaner Scheduled Lock",
            summary = "Added a 3-second scheduled session success screen that waits for phone unlock when finished while locked, simplified the scheduled lock screen with a medium Goal & Streak pill, and kept all previous schedule reliability upgrades.",
            items = listOf(
                WhatsNewItem(
                    title = "3-Second Scheduled Completion Screen",
                    description = "When a scheduled focus timer ends, a celebratory completion screen with your focused time and encouragement message stays for 3 seconds. If the timer finishes while your phone is locked, it waits and appears for 3 full seconds right after you unlock.",
                    category = WhatsNewCategory.NEW
                ),
                WhatsNewItem(
                    title = "Cleaner Scheduled Lock & Medium Goal Pill",
                    description = "Removed Focus Mantra and Session Timeline from the scheduled lock screen for a cleaner layout, and upgraded the Goal & Streak bar to medium size for better readability.",
                    category = WhatsNewCategory.NEW
                ),
                WhatsNewItem(
                    title = "1-Minute Schedule Time Picker",
                    description = "Schedule start and stop minute buttons now adjust by 1 minute per tap (00 to 59) instead of 5-minute jumps, and new schedules default close to the current time.",
                    category = WhatsNewCategory.NEW
                ),
                WhatsNewItem(
                    title = "Streamlined Scheduled Lock Screen",
                    description = "Removed Lock Studio and locked in a clean Dot Ring layout with balanced spacing and the Compact Bottom Dock directly below your modules.",
                    category = WhatsNewCategory.NEW
                ),
                WhatsNewItem(
                    title = "Reliable Scheduled Focus Triggers",
                    description = "Fixed scheduled locks not starting on time by switching to high-priority AlarmClock scheduling, WakeLock protection, an active foreground schedule watcher, and a one-tap Exact Alarm permission prompt.",
                    category = WhatsNewCategory.FIX
                ),
                WhatsNewItem(
                    title = "Removed Nothing Island",
                    description = "Completely removed Nothing Island from overlays and Settings, restoring the clean in-app Blocked Notifications summary sheet on unlock.",
                    category = WhatsNewCategory.FIX
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
