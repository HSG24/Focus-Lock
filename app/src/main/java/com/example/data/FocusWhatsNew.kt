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
    const val CURRENT_RELEASE_ID = 16

    val releases: List<WhatsNewRelease> = listOf(
        WhatsNewRelease(
            releaseId = 16,
            versionLabel = "Final Release",
            headline = "Mindful Quiz Unlock, Sleek New Identity & 100% On-Device Control",
            summary = "Completely rebuilt manual locks with an infinite 10-question quiz, adaptive built-in keypad, and 5-second unlock celebration, moved all stats on-device, added JSON backup & restore, a fresh-install setup flow, exact 1:1 pill previews, and a new Solar Eclipse Focus Ring logo.",
            items = listOf(
                WhatsNewItem(
                    title = "Redesigned Manual Lock & 10-Question Infinite Quiz",
                    description = "Replaced the 6-digit password with a clean, unified manual lock screen and an Unlock Session button that launches a 10-question procedural quiz directly inside the overlay — featuring Odd One Out, Number Reflex, Quick X, Number Flow, Emoji Algebra, Symbol Swap, Solo Hunter, and Easy Unscramble with permanent no-repeat memory.",
                    category = WhatsNewCategory.NEW
                ),
                WhatsNewItem(
                    title = "Adaptive Built-In Keypad & Unlock Celebration",
                    description = "The quiz automatically switches between a sleek numeric pad and a spacious, tall QWERTY A–Z keypad right inside the lock overlay, finishing with a smooth 5-second unlock animation after your 10th correct answer.",
                    category = WhatsNewCategory.NEW
                ),
                WhatsNewItem(
                    title = "Solar Eclipse Focus Ring Logo & Identity",
                    description = "Reimagined FocusLock's launcher icon and in-app emblem with a notched warm-ivory precision ring, floating focal dot, and glowing ember-amber eclipse crescent on matte carbon black.",
                    category = WhatsNewCategory.NEW
                ),
                WhatsNewItem(
                    title = "Full Export & Import Backup Page",
                    description = "Added a dedicated Backup & Restore page in Settings that exports and restores all settings, focus stats, XP, streaks, schedules, widget configs, notification logs, and quiz memory via .json files or clipboard copy/paste.",
                    category = WhatsNewCategory.NEW
                ),
                WhatsNewItem(
                    title = "100% On-Device Stats & Public GitHub Updates",
                    description = "Removed Firebase dependencies across lock sessions, stats, and app updates so everything runs privately and instantaneously on-device (while keeping Share Links intact) and updates pull directly from the public GitHub Releases API.",
                    category = WhatsNewCategory.NEW
                ),
                WhatsNewItem(
                    title = "First-Install Welcome Setup",
                    description = "Added a refined four-step welcome setup experience exclusively for brand-new installations, covering theme selection, daily goal calibration, essential permissions, and optional backup restore.",
                    category = WhatsNewCategory.NEW
                ),
                WhatsNewItem(
                    title = "Exact 1:1 Minimal Pills Live Preview",
                    description = "Rebuilt the Minimal Pills size preview in Settings to match the real lock screen's exact width and scale with zero distortion, complete with a live Single Row / Wraps indicator.",
                    category = WhatsNewCategory.FIX
                ),
                WhatsNewItem(
                    title = "Cleaner Scheduled Lock Progress Layout",
                    description = "Removed the redundant second linear progress bar below the circular countdown timer on the scheduled lock screen and replaced it with a crisp inline session window strip.",
                    category = WhatsNewCategory.FIX
                ),
                WhatsNewItem(
                    title = "Redesigned Scheduled Lock Screen",
                    description = "Rebuilt from the ground up with a minimal, unified monolith layout that brings your countdown ring, session window strip, daily goal, streak, and silenced alerts together in one cohesive card with zero clutter.",
                    category = WhatsNewCategory.NEW
                ),
                WhatsNewItem(
                    title = "Full Theme-Matched Lock & Completion Cards",
                    description = "Every element on the scheduled lock and completion screens now seamlessly follows your active app theme's timer ring, typography, glass surfaces, and dividers.",
                    category = WhatsNewCategory.NEW
                ),
                WhatsNewItem(
                    title = "Manual Dismiss for Completion Screen",
                    description = "Removed the 3-second automatic dismissal on the scheduled focus completion screen and added a Done button so you can review your session summary and dismiss it whenever you are ready.",
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
