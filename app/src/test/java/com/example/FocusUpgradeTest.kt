package com.example

import android.app.Notification
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.service.notification.StatusBarNotification
import androidx.test.core.app.ApplicationProvider
import com.example.data.FocusRank
import com.example.data.FocusSettingsRepository
import com.example.data.FocusXpRepository
import com.example.data.NotificationBlockerRepository
import com.example.service.FocusNotificationBlockerService
import com.example.service.FocusPowerBlockerAccessibilityService
import com.example.ui.theme.AppVisualTheme
import com.example.ui.theme.FocusThemePalette
import com.example.widget.NothingFocusWidget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class FocusUpgradeTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("focus_xp_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("focus_blocked_notifications", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("focus_settings", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun focusRank_mapsAllSixTiersAccurately() {
        assertEquals(FocusRank.INITIATE, FocusRank.forXp(0))
        assertEquals(FocusRank.INITIATE, FocusRank.forXp(499))
        assertEquals(FocusRank.APPRENTICE, FocusRank.forXp(500))
        assertEquals(FocusRank.APPRENTICE, FocusRank.forXp(1499))
        assertEquals(FocusRank.TACTICIAN, FocusRank.forXp(1500))
        assertEquals(FocusRank.TACTICIAN, FocusRank.forXp(3499))
        assertEquals(FocusRank.VANGUARD, FocusRank.forXp(3500))
        assertEquals(FocusRank.VANGUARD, FocusRank.forXp(6999))
        assertEquals(FocusRank.MONK, FocusRank.forXp(7000))
        assertEquals(FocusRank.MONK, FocusRank.forXp(14999))
        assertEquals(FocusRank.SOVEREIGN, FocusRank.forXp(15000))
        assertEquals(FocusRank.SOVEREIGN, FocusRank.forXp(50000))
    }

    @Test
    fun xpRepository_awardsMinutesAndDailyBonusOncePerDay() {
        val now = System.currentTimeMillis()
        // 30 mins -> 300 XP, goal 60 mins (not met yet)
        FocusXpRepository.recordCompletedSession(
            context = context,
            sessionStartTimeMs = now,
            durationMs = 30 * 60_000L,
            dailyGoalMinutes = 60L
        )
        var state = FocusXpRepository.getCurrent(context)
        assertEquals(300, state.totalXp)
        assertEquals(FocusRank.INITIATE, state.rank)

        // Another 35 mins -> +350 XP + 100 bonus = 750 XP (Apprentice)
        FocusXpRepository.recordCompletedSession(
            context = context,
            sessionStartTimeMs = now,
            durationMs = 35 * 60_000L,
            dailyGoalMinutes = 60L
        )
        state = FocusXpRepository.getCurrent(context)
        assertEquals(750, state.totalXp)
        assertEquals(FocusRank.APPRENTICE, state.rank)

        // Another 10 mins same day -> +100 XP, no duplicate daily bonus = 850 XP
        FocusXpRepository.recordCompletedSession(
            context = context,
            sessionStartTimeMs = now,
            durationMs = 10 * 60_000L,
            dailyGoalMinutes = 60L
        )
        state = FocusXpRepository.getCurrent(context)
        assertEquals(850, state.totalXp)
    }

    @Test
    fun xpRepository_deductsPenaltyForMissedPreviousDayAndClampsAtZero() {
        val yesterday = Calendar.getInstance().apply {
            add(Calendar.DATE, -1)
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val yesterdayKey = FocusXpRepository.dateKeyForMs(yesterday.timeInMillis)

        // Seed tracking start date to yesterday and 200 XP
        context.getSharedPreferences("focus_xp_prefs", Context.MODE_PRIVATE).edit()
            .putString("tracking_start_date_key", yesterdayKey)
            .putInt("total_xp", 200)
            .commit()

        FocusXpRepository.evaluateMissedDailyGoals(
            context = context,
            dailyGoalMinutes = 240L,
            nowCal = Calendar.getInstance()
        )
        var state = FocusXpRepository.getCurrent(context)
        assertEquals(50, state.totalXp) // 200 - 150 = 50
        assertTrue(state.history.any { it.deltaXp == -150 })

        // Re-running evaluation on same day must not penalize twice
        FocusXpRepository.evaluateMissedDailyGoals(
            context = context,
            dailyGoalMinutes = 240L,
            nowCal = Calendar.getInstance()
        )
        state = FocusXpRepository.getCurrent(context)
        assertEquals(50, state.totalXp)

        // Evaluate a second missed day (simulate 2 days earlier) -> clamps cleanly at 0
        val twoDaysAgo = Calendar.getInstance().apply { add(Calendar.DATE, -2) }
        val twoDaysKey = FocusXpRepository.dateKeyForMs(twoDaysAgo.timeInMillis)
        context.getSharedPreferences("focus_xp_prefs", Context.MODE_PRIVATE).edit()
            .putString("tracking_start_date_key", twoDaysKey)
            .commit()
        FocusXpRepository.evaluateMissedDailyGoals(
            context = context,
            dailyGoalMinutes = 240L,
            nowCal = Calendar.getInstance()
        )
        state = FocusXpRepository.getCurrent(context)
        assertEquals(0, state.totalXp)
    }

    @Test
    fun notificationBlocker_exemptsCallsAndAlarmsAndBlocksDistractions() {
        assertTrue(FocusNotificationBlockerService.isCallOrExemptPackage("com.android.dialer"))
        assertTrue(FocusNotificationBlockerService.isCallOrExemptPackage("com.google.android.dialer"))
        assertTrue(FocusNotificationBlockerService.isCallOrExemptPackage("com.android.server.telecom"))
        assertTrue(FocusNotificationBlockerService.isCallOrExemptPackage("com.nothing.dialer"))
        assertFalse(FocusNotificationBlockerService.isCallOrExemptPackage("com.whatsapp"))
        assertFalse(FocusNotificationBlockerService.isCallOrExemptPackage("com.instagram.android"))

        val callNotification = Notification.Builder(context, "test")
            .setCategory(Notification.CATEGORY_CALL)
            .setContentTitle("Incoming call")
            .build()
        val callSbn = StatusBarNotification(
            "com.some.voip",
            "com.some.voip",
            1,
            null,
            1000,
            1000,
            0,
            callNotification,
            android.os.Process.myUserHandle(),
            System.currentTimeMillis()
        )
        assertTrue(FocusNotificationBlockerService.shouldAllowNotification(callSbn))

        val alarmNotification = Notification.Builder(context, "test")
            .setCategory(Notification.CATEGORY_ALARM)
            .setContentTitle("Alarm")
            .build()
        val alarmSbn = StatusBarNotification(
            "com.google.android.deskclock",
            "com.google.android.deskclock",
            2,
            null,
            1000,
            1000,
            0,
            alarmNotification,
            android.os.Process.myUserHandle(),
            System.currentTimeMillis()
        )
        assertTrue(FocusNotificationBlockerService.shouldAllowNotification(alarmSbn))

        val chatNotification = Notification.Builder(context, "test")
            .setCategory(Notification.CATEGORY_MESSAGE)
            .setContentTitle("Alice")
            .setContentText("Hey!")
            .build()
        val chatSbn = StatusBarNotification(
            "com.instagram.android",
            "com.instagram.android",
            3,
            null,
            1000,
            1000,
            0,
            chatNotification,
            android.os.Process.myUserHandle(),
            System.currentTimeMillis()
        )
        assertFalse(FocusNotificationBlockerService.shouldAllowNotification(chatSbn))
    }

    @Test
    fun notificationBlockerRepository_groupsAndClearsSummary() {
        NotificationBlockerRepository.startFreshSession(context)
        NotificationBlockerRepository.recordBlockedNotification(context, "com.whatsapp", "WhatsApp", "Sam", "Hello")
        NotificationBlockerRepository.recordBlockedNotification(context, "com.whatsapp", "WhatsApp", "Sam", "Are you free?")
        NotificationBlockerRepository.recordBlockedNotification(context, "com.discord", "Discord", "#general", "New message")

        val hasBlocked = NotificationBlockerRepository.finalizeSessionOnUnlock(context)
        assertTrue(hasBlocked)

        val state = NotificationBlockerRepository.getCurrent(context)
        assertTrue(state.showSummarySheet)
        assertEquals(3, state.totalBlockedCount)
        assertEquals(2, state.groupedLastSummary.size)
        assertEquals(2, state.groupedLastSummary.first().count)

        NotificationBlockerRepository.clearSummaryLog(context)
        val cleared = NotificationBlockerRepository.getCurrent(context)
        assertFalse(cleared.showSummarySheet)
        assertEquals(0, cleared.totalBlockedCount)
    }

    @Test
    fun notificationBlockerRepository_deduplicatesRepeatedIdenticalAndProgressNotifications() {
        NotificationBlockerRepository.startFreshSession(context)

        // Simulate an app reposting the exact same notification 25 times
        for (i in 1..25) {
            NotificationBlockerRepository.recordBlockedNotification(
                context = context,
                packageName = "com.instagram.android",
                appLabel = "Instagram",
                title = "alice_dev ",
                snippet = "Sent a reel  ",
                timestampMs = 1_700_000_000_000L + i * 1_000L,
                notificationKey = "0|com.instagram.android|101|null|1001"
            )
        }
        // Plus one genuinely different notification from the same app
        NotificationBlockerRepository.recordBlockedNotification(
            context = context,
            packageName = "com.instagram.android",
            appLabel = "Instagram",
            title = "bob_designs",
            snippet = "Liked your message",
            timestampMs = 1_700_000_050_000L,
            notificationKey = "0|com.instagram.android|102|null|1001"
        )
        // Plus an ongoing progress notification updated 10 times under the same key
        for (pct in 10..100 step 10) {
            NotificationBlockerRepository.recordBlockedNotification(
                context = context,
                packageName = "com.spotify.music",
                appLabel = "Spotify",
                title = "Downloading playlist",
                snippet = "$pct% complete",
                timestampMs = 1_700_000_060_000L + pct,
                notificationKey = "0|com.spotify.music|99|null|1002",
                isOngoingOrProgress = true
            )
        }

        assertTrue(NotificationBlockerRepository.finalizeSessionOnUnlock(context))
        val state = NotificationBlockerRepository.getCurrent(context)

        // 25 identical Instagram alerts collapsed into 1 + 1 distinct Instagram alert + 1 Spotify progress = 3 total
        assertEquals(3, state.totalBlockedCount)
        val instaGroup = state.groupedLastSummary.first { it.packageName == "com.instagram.android" }
        assertEquals(2, instaGroup.count)
        assertEquals(1_700_000_025_000L, instaGroup.items.first { it.title == "alice_dev" }.timestampMs)

        val spotifyGroup = state.groupedLastSummary.first { it.packageName == "com.spotify.music" }
        assertEquals(1, spotifyGroup.count)
        assertEquals("100% complete", spotifyGroup.items.first().snippet)
    }

    @Test
    fun notificationBlocker_detectsGroupSummaryFlag() {
        val summaryNotif = Notification.Builder(context, "test")
            .setContentTitle("WhatsApp")
            .setContentText("5 new messages")
            .setGroup("chats")
            .setGroupSummary(true)
            .build()
        val summarySbn = StatusBarNotification(
            "com.whatsapp",
            "com.whatsapp",
            999,
            null,
            1000,
            1000,
            0,
            summaryNotif,
            android.os.Process.myUserHandle(),
            System.currentTimeMillis()
        )
        assertTrue(FocusNotificationBlockerService.isGroupSummary(summarySbn))
    }

    @Test
    fun lockService_clampsMovableBubbleWithinScreenBounds() {
        val (clampedNegX, clampedNegY) = LockService.clampBubblePosition(
            x = -50,
            y = -20,
            viewWidth = 300,
            viewHeight = 120,
            screenWidth = 1080,
            screenHeight = 2400,
            marginPx = 24
        )
        assertEquals(24, clampedNegX)
        assertEquals(24, clampedNegY)

        val (clampedMaxX, clampedMaxY) = LockService.clampBubblePosition(
            x = 2000,
            y = 3000,
            viewWidth = 300,
            viewHeight = 120,
            screenWidth = 1080,
            screenHeight = 2400,
            marginPx = 24
        )
        assertEquals(1080 - 300 - 24, clampedMaxX)
        assertEquals(2400 - 120 - 24, clampedMaxY)
    }

    @Test
    fun powerBlocker_matchesPowerAndRestartLabels() {
        assertTrue(FocusPowerBlockerAccessibilityService.matchesPowerLabel("Power off"))
        assertTrue(FocusPowerBlockerAccessibilityService.matchesPowerLabel("Restart device"))
        assertTrue(FocusPowerBlockerAccessibilityService.matchesPowerLabel("Reboot"))
        assertTrue(FocusPowerBlockerAccessibilityService.matchesPowerLabel("Emergency call"))
        assertFalse(FocusPowerBlockerAccessibilityService.matchesPowerLabel("Volume up"))
        assertFalse(FocusPowerBlockerAccessibilityService.matchesPowerLabel(null))
    }

    @Test
    fun unifiedThemesAndWidgetDefaults_resolveCleanly() {
        assertEquals(4, AppVisualTheme.entries.size)
        AppVisualTheme.entries.forEach { theme ->
            val palette = FocusThemePalette.from(theme)
            assertEquals(theme, palette.theme)
            assertEquals(theme, AppVisualTheme.fromIdOrName(theme.id))
        }
        assertTrue(FocusThemePalette.from(AppVisualTheme.LIQUID_GLASS).isLiquidGlass)
        assertTrue(FocusThemePalette.from(AppVisualTheme.NORDIC_SAGE).useOrganicHorizon)
        // Verify Dark Liquid Glass uses deep obsidian background and opaque surface for crisp dialogs
        val darkGlass = FocusThemePalette.from(AppVisualTheme.LIQUID_GLASS)
        assertTrue(darkGlass.background.red < 0.05f && darkGlass.background.green < 0.05f && darkGlass.background.blue < 0.06f)
        assertEquals(1f, darkGlass.surface.alpha, 0.001f)
        // Verify crisp textPrimary contrast against background across all themes
        AppVisualTheme.entries.forEach { theme ->
            val p = FocusThemePalette.from(theme)
            assertTrue("Expected bright textPrimary for $theme", p.textPrimary.red > 0.85f && p.textPrimary.green > 0.85f)
            assertTrue("Expected dark background for $theme", p.background.red < 0.10f && p.background.green < 0.10f && p.background.blue < 0.10f)
        }
        // Removed legacy themes cleanly fall back to Nothing Crimson
        assertEquals(AppVisualTheme.NOTHING_CRIMSON, AppVisualTheme.fromIdOrName("emerald_terminal"))
        assertEquals(AppVisualTheme.NOTHING_CRIMSON, AppVisualTheme.fromIdOrName("EMERALD_TERMINAL"))
        assertEquals(AppVisualTheme.NOTHING_CRIMSON, AppVisualTheme.fromIdOrName("cyber_ice"))
        assertEquals(AppVisualTheme.NOTHING_CRIMSON, AppVisualTheme.fromIdOrName("ultraviolet_pulse"))

        val settings = FocusSettingsRepository.getCurrent(context)
        assertEquals(240L, settings.dailyGoalMinutes)
        assertEquals(AppVisualTheme.NOTHING_CRIMSON, settings.appTheme)
        assertTrue(settings.widgetShowDottedRing)
        assertTrue(settings.blockPowerMenuDuringLock)
    }

    @Test
    fun widgetDottedRingBitmap_rendersAtHighResolutionArgb8888() {
        val ring = NothingFocusWidget.renderDottedRingBitmap(
            context = context,
            progressFraction = 0.5f,
            studyHoursText = "02H 00M",
            percentText = "50%",
            showStudyHours = true,
            canvasSizePx = 720
        )
        assertEquals(720, ring.width)
        assertEquals(720, ring.height)
        assertEquals(Bitmap.Config.ARGB_8888, ring.config)
    }

    @Test
    fun monochromeAppIconProcessor_extractsSilhouetteAndAppliesThemeAccent() {
        // Construct a multicolored 64x64 legacy icon: dark blue square badge with bright white center square
        val src = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(src)
        val bgPaint = Paint().apply { color = Color.rgb(18, 45, 110) }
        val fgPaint = Paint().apply { color = Color.WHITE }
        canvas.drawRect(4f, 4f, 60f, 60f, bgPaint)
        canvas.drawRect(22f, 22f, 42f, 42f, fgPaint)

        val accentArgb = Color.rgb(0, 229, 255)
        val result = FocusSettingsRepository.toMonochromeTintedBitmap(
            drawable = BitmapDrawable(context.resources, src),
            targetPx = 64,
            accentColorArgb = accentArgb
        )

        val centerPixel = result.getPixel(32, 32)
        val outerBadgePixel = result.getPixel(10, 10)
        assertTrue(((centerPixel ushr 24) and 0xFF) > 200)
        assertEquals(0, (centerPixel ushr 16) and 0xFF)
        assertEquals(229, (centerPixel ushr 8) and 0xFF)
        assertEquals(255, centerPixel and 0xFF)
        assertTrue(((outerBadgePixel ushr 24) and 0xFF) < 30)
    }
}
