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
    const val CURRENT_RELEASE_ID = 9

    val releases: List<WhatsNewRelease> = listOf(
        WhatsNewRelease(
            releaseId = 9,
            versionLabel = "Latest Update",
            headline = "Minimal Stats, Share & Fluid Pill Motion",
            summary = "Completely redesigned Fitbit-inspired Stats & Share screens, unmistakable selected-bar spotlights, iOS-style elastic pill animations across every theme, static dark glass, and the classic black Focus slider.",
            items = listOf(
                WhatsNewItem(
                    title = "Minimal Pill Stats Redesign",
                    description = "Rebuilt Stats from scratch with a calm, uncluttered health-app layout: single-row floating date pill, horizontal full-pill goal progress bar, capsule bar chart with spring entrance, and four airy metric capsules.",
                    category = WhatsNewCategory.NEW
                ),
                WhatsNewItem(
                    title = "Fluid Elastic Pill Animations",
                    description = "Added iOS-style rubber-band morphing pills and directional label/icon glide across the navigation bar, Stats tabs, Share selectors, and Settings controls in every theme—triggered only by your touch and swipes.",
                    category = WhatsNewCategory.NEW
                ),
                WhatsNewItem(
                    title = "Minimal Share Screen Redesign",
                    description = "Streamlined Share into borderless circular-icon cards, elastic segmented controls for Live, Range, and Your Links, rolling preview totals, and directional step transitions.",
                    category = WhatsNewCategory.NEW
                ),
                WhatsNewItem(
                    title = "Dark Liquid Glass & Nordic Sage",
                    description = "Rebuilt Liquid Glass into a deep obsidian dark-glass finish with subtle specular highlights and touch refraction—replacing light frosted panels—alongside calm basalt Nordic Sage.",
                    category = WhatsNewCategory.NEW
                ),
                WhatsNewItem(
                    title = "Borderless Pill Redesign",
                    description = "Every card, panel, chip, button, badge, and dialog across all four themes now uses softer circular and shorter pill-like proportions with zero harsh outlines.",
                    category = WhatsNewCategory.NEW
                ),
                WhatsNewItem(
                    title = "Categorized What's New",
                    description = "Release notes are now cleanly organized into dedicated New and Fixes sections so updates are effortless to scan.",
                    category = WhatsNewCategory.NEW
                ),
                WhatsNewItem(
                    title = "Minimal Visual Redesign",
                    description = "Decluttered Home, Stats, Settings, and Lock screens with a centered daily-goal ring, softer tonal surfaces, compact theme swatches, and a collapsible Widget Studio.",
                    category = WhatsNewCategory.NEW
                ),
                WhatsNewItem(
                    title = "Fluid Swipe Gestures",
                    description = "Slide or tap the Focus pill to start locking, swipe across the bottom capsule to scrub tabs, drag across Stats charts to inspect bars, pull to refresh Stats, and swipe left on PIN dots to delete.",
                    category = WhatsNewCategory.NEW
                ),
                WhatsNewItem(
                    title = "Refined Tactile Haptics",
                    description = "Added crisp haptic feedback across buttons, switches, sliders, swipes, link actions, and PIN entry, plus a subtle shake when an incorrect PIN is entered.",
                    category = WhatsNewCategory.NEW
                ),
                WhatsNewItem(
                    title = "Unmistakable Selected Stats Bars",
                    description = "Selected bars in Stats now pop slightly wider with a theme-matched luminous gradient, soft vertical column spotlight, floating top indicator dot, and a filled date pill underneath across all four themes.",
                    category = WhatsNewCategory.FIX
                ),
                WhatsNewItem(
                    title = "Static Dark Glass & Classic Focus Slider",
                    description = "Removed all continuous live idle animations from Liquid Glass and restored the classic black Focus slider track with theme-colored circular lock thumb on Crimson, Solar Amber, and Nordic Sage.",
                    category = WhatsNewCategory.FIX
                ),
                WhatsNewItem(
                    title = "Universal Contrast & Visibility",
                    description = "Tuned text, pill fills, dialogs, and elevated surfaces across Crimson, Dark Glass, Solar Amber, and Nordic Sage for crisp legibility everywhere.",
                    category = WhatsNewCategory.FIX
                ),
                WhatsNewItem(
                    title = "Smarter Stats Bar Interaction",
                    description = "Empty (0s) bars in Stats are no longer clickable; tapping or swiping above no-data bars now passes straight through for smooth scrolling and tab switching while bars with data stay interactive.",
                    category = WhatsNewCategory.FIX
                ),
                WhatsNewItem(
                    title = "Reliability & Accuracy Fixes",
                    description = "Fixed Stats calendar date shifts, offline streak calculations, sub-hour goal labels, widget preview values, and lock session persistence across background restarts.",
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
