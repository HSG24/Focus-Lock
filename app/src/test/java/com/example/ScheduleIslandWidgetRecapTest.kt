package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.data.AllowedApp
import com.example.data.FocusSchedule
import com.example.data.FocusScheduleRepository
import com.example.data.FocusSettingsState
import com.example.data.FocusXpState
import com.example.data.LockAppLayout
import com.example.data.LockModuleSize
import com.example.data.LockSpacingDensity
import com.example.data.NothingWidgetKind
import com.example.data.ScheduleRepeatMode
import com.example.data.ScheduledBottomDockMode
import com.example.data.ScheduledLockLayoutConfig
import com.example.data.ScheduledLockModuleType
import com.example.data.ScheduledTimerStyle
import com.example.data.WidgetCornerStyle
import com.example.data.WidgetStudioRepository
import com.example.data.WidgetSurfaceStyle
import com.example.service.ScheduleAlarmScheduler
import com.example.ui.components.RecapCardOptions
import com.example.ui.components.RecapCardTheme
import com.example.ui.components.RecapTimeframe
import com.example.ui.components.ScheduledLockScreen
import com.example.ui.components.ScheduledSuccessScreen
import com.example.ui.components.ShareRecapCardRenderer
import com.example.ui.screens.ScheduleScreen
import com.example.ui.theme.AppVisualTheme
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class ScheduleIslandWidgetRecapTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("focus_schedules_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("focus_widget_studio_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        LockService.isRunning = false
        LockService.isScheduledSession = false
    }

    @After
    fun tearDown() {
        LockService.isRunning = false
        LockService.isScheduledSession = false
    }

    @Test
    fun focusScheduleRepository_enforcesFixedDesignOrderAndOvernightSkip() {
        val overnight = FocusSchedule(
            id = "sched_night",
            name = "Late Study",
            startHour = 23,
            startMinute = 0,
            endHour = 1,
            endMinute = 30,
            repeatMode = ScheduleRepeatMode.DAILY
        )
        assertEquals(150, overnight.durationMinutes())

        FocusScheduleRepository.addOrUpdateSchedule(context, overnight)
        val saved = FocusScheduleRepository.getCurrent(context).schedules
        assertEquals(1, saved.size)
        assertEquals("Late Study", saved.first().name)

        // Marking skipped for today skips today's occurrence cleanly
        val anchor = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 20, 22, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        FocusScheduleRepository.markSkippedForToday(context, "sched_night", anchor)
        val afterSkip = FocusScheduleRepository.getCurrent(context).schedules.first()
        assertTrue(FocusScheduleRepository.isSkippedToday(afterSkip, anchor))

        val nextWindow = FocusScheduleRepository.computeNextOccurrence(afterSkip, anchor)
        assertNotNull(nextWindow)
        val nextDayCal = Calendar.getInstance().apply { timeInMillis = nextWindow!!.startTimeMs }
        assertEquals(21, nextDayCal.get(Calendar.DAY_OF_MONTH))

        // Verify exact fixed scheduled lock design (Focus Mantra & Session Timeline removed, Goal & Streak Medium)
        val layout = FocusScheduleRepository.getCurrent(context).layoutConfig
        assertEquals(ScheduledTimerStyle.DOT_RING, layout.timerStyle)
        assertEquals(LockSpacingDensity.BALANCED, layout.spacingDensity)
        assertEquals(ScheduledBottomDockMode.COMPACT, layout.bottomDockMode)
        assertEquals(
            listOf(
                ScheduledLockModuleType.CLOCK_DATE,
                ScheduledLockModuleType.SCHEDULE_BADGE,
                ScheduledLockModuleType.COUNTDOWN_TIMER,
                ScheduledLockModuleType.ALLOWED_APPS,
                ScheduledLockModuleType.DAILY_PROGRESS
            ),
            layout.modules.map { it.type }
        )
        val goalModule = layout.modules.first { it.type == ScheduledLockModuleType.DAILY_PROGRESS }
        assertEquals(LockModuleSize.MEDIUM, goalModule.size)
    }

    @Test
    fun scheduleAlarmScheduler_skipsScheduleForTodayWhenManualOtpLockIsRunning() {
        val nowCal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 21, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val schedule = FocusSchedule(
            id = "sched_morning",
            name = "Morning Deep Work",
            startHour = 10,
            startMinute = 0,
            endHour = 11,
            endMinute = 0,
            repeatMode = ScheduleRepeatMode.DAILY
        )
        FocusScheduleRepository.addOrUpdateSchedule(context, schedule)

        // Simulate an active manual OTP session
        LockService.isRunning = true
        LockService.isScheduledSession = false

        val handled = ScheduleAlarmScheduler.triggerActiveScheduleIfDue(context, nowCal.timeInMillis)
        assertTrue(handled)

        val updated = FocusScheduleRepository.getCurrent(context).schedules.first { it.id == "sched_morning" }
        assertTrue(FocusScheduleRepository.isSkippedToday(updated, nowCal.timeInMillis))
    }

    @Test
    fun scheduleScreen_hasNoLockStudioAndMinuteStepperMovesByOneUpTo59() {
        val existing = FocusSchedule(
            id = "sched_exact",
            name = "Exact Minute Test",
            startHour = 9,
            startMinute = 0,
            endHour = 10,
            endMinute = 58,
            repeatMode = ScheduleRepeatMode.DAILY
        )
        FocusScheduleRepository.addOrUpdateSchedule(context, existing)

        composeRule.setContent {
            MyApplicationTheme(appTheme = AppVisualTheme.LIQUID_GLASS) {
                ScheduleScreen(settingsState = FocusSettingsState())
            }
        }

        // Lock Studio tab is completely removed
        composeRule.onNodeWithTag("schedule_tab_studio").assertDoesNotExist()
        composeRule.onNodeWithTag("schedule_card_sched_exact").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        // Start minute 00 -> minus wraps to 59, plus goes back to 00, plus goes to 01
        composeRule.onNodeWithTag("start_minute_minus").performClick()
        composeRule.onNodeWithText("59m").assertIsDisplayed()
        composeRule.onNodeWithTag("start_minute_plus").performClick()
        composeRule.onNodeWithTag("start_minute_plus").performClick()
        composeRule.onNodeWithText("01m").assertIsDisplayed()

        // Stop minute 58 -> plus moves by 1 to 59
        composeRule.onNodeWithTag("stop_minute_plus").performClick()
        composeRule.onNodeWithText("59m").assertIsDisplayed()
    }

    @Test
    fun widgetStudioRepository_persistsPerWidgetNothingMonoVsAppThemeIndependently() {
        WidgetStudioRepository.updateWidgetConfig(context, NothingWidgetKind.DOT_CLOCK) {
            it.copy(
                useNothingMono = false,
                cornerStyle = WidgetCornerStyle.SOFT,
                surfaceStyle = WidgetSurfaceStyle.GLASS,
                use24HourClock = true
            )
        }
        WidgetStudioRepository.updateWidgetConfig(context, NothingWidgetKind.HEATMAP_MATRIX) {
            it.copy(useNothingMono = true, surfaceStyle = WidgetSurfaceStyle.ELEVATED)
        }

        val clockCfg = WidgetStudioRepository.getConfig(context, NothingWidgetKind.DOT_CLOCK)
        val heatmapCfg = WidgetStudioRepository.getConfig(context, NothingWidgetKind.HEATMAP_MATRIX)

        assertFalse(clockCfg.useNothingMono)
        assertEquals(WidgetCornerStyle.SOFT, clockCfg.cornerStyle)
        assertEquals(WidgetSurfaceStyle.GLASS, clockCfg.surfaceStyle)
        assertTrue(clockCfg.use24HourClock)

        assertTrue(heatmapCfg.useNothingMono)
        assertEquals(WidgetSurfaceStyle.ELEVATED, heatmapCfg.surfaceStyle)
    }

    @Test
    fun shareRecapCardRenderer_generatesCrispBitmapAcrossThemesAndCustomDate() {
        val customDayCal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 15, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val customDayMs = customDayCal.timeInMillis
        val now = System.currentTimeMillis()
        val sessions = listOf(
            Session(startTime = now - 45 * 60_000L, duration = 45 * 60_000L),
            Session(startTime = now - 2 * 3_600_000L, duration = 60 * 60_000L),
            Session(startTime = customDayMs, duration = 75 * 60_000L)
        )
        val xp = FocusXpState(totalXp = 1250, todayFocusMs = 105 * 60_000L, currentStreak = 6)
        val data = ShareRecapCardRenderer.buildRecapData(
            sessions = sessions,
            xpState = xp,
            dailyGoalMinutes = 120L,
            timeframe = RecapTimeframe.TODAY
        )
        assertEquals(2, data.sessionCount)
        assertEquals(60 * 60_000L, data.longestSessionMs)
        assertEquals(35, data.miniHeatmapLevels.size)

        val customData = ShareRecapCardRenderer.buildRecapData(
            sessions = sessions,
            xpState = xp,
            dailyGoalMinutes = 120L,
            timeframe = RecapTimeframe.CUSTOM,
            customDateMs = customDayMs
        )
        assertEquals(1, customData.sessionCount)
        assertEquals(75 * 60_000L, customData.longestSessionMs)
        assertEquals("NOV 15", customData.timeframeLabel)

        for (theme in RecapCardTheme.entries) {
            val bmp = ShareRecapCardRenderer.renderCardBitmap(
                context = context,
                data = customData,
                options = RecapCardOptions(theme = theme),
                widthPx = 540,
                heightPx = 675
            )
            assertEquals(540, bmp.width)
            assertEquals(675, bmp.height)
        }
    }

    @Test
    fun scheduledLockScreen_rendersUnifiedMonolithAndIntegratedTelemetry() {
        val startMs = 1_700_000_000_000L
        val endMs = startMs + 60 * 60_000L
        composeRule.setContent {
            MyApplicationTheme(appTheme = AppVisualTheme.LIQUID_GLASS) {
                ScheduledLockScreen(
                    scheduleName = "Deep Work",
                    timeRangeLabel = "9:00 AM → 10:00 AM",
                    startTimeMs = startMs,
                    endTimeMs = endMs,
                    layoutConfig = ScheduledLockLayoutConfig(),
                    dailyGoalPercent = 82,
                    currentStreak = 5,
                    allowedApps = listOf(
                        AllowedApp("preset_phone", "Phone", "com.android.dialer", true)
                    ),
                    appLayout = LockAppLayout.PILL_ROW,
                    isPreview = true
                )
            }
        }

        composeRule.onNodeWithTag("scheduled_lock_header").assertIsDisplayed()
        composeRule.onNodeWithText("Deep Work").assertIsDisplayed()
        composeRule.onNodeWithTag("scheduled_lock_monolith_card").assertIsDisplayed()
        composeRule.onNodeWithTag("scheduled_lock_countdown_text").assertIsDisplayed()
        composeRule.onNodeWithTag("scheduled_lock_window_rail").assertIsDisplayed()
        composeRule.onNodeWithTag("scheduled_tile_goal").assertIsDisplayed()
        composeRule.onNodeWithTag("scheduled_tile_streak").assertIsDisplayed()
        composeRule.onNodeWithTag("scheduled_tile_silenced").assertIsDisplayed()
        composeRule.onNodeWithText("82%").assertIsDisplayed()
        composeRule.onNodeWithText("5d").assertIsDisplayed()
        composeRule.onNodeWithTag("scheduled_lock_apps_tray").assertIsDisplayed()
        composeRule.onNodeWithTag("scheduled_lock_unlock_footer").assertIsDisplayed()
    }

    @Test
    fun scheduledSuccessScreen_hasManualDismissButtonAndNoAutoCountdown() {
        var dismissed = false
        composeRule.setContent {
            MyApplicationTheme(appTheme = AppVisualTheme.NOTHING_CRIMSON) {
                ScheduledSuccessScreen(
                    scheduleName = "Morning Focus",
                    durationMs = 45 * 60_000L,
                    encouragementMessage = "Strong focus session.",
                    dailyGoalPercent = 100,
                    currentStreak = 7,
                    onDismiss = { dismissed = true }
                )
            }
        }

        composeRule.onNodeWithTag("scheduled_success_screen").assertIsDisplayed()
        composeRule.onNodeWithText("SESSION COMPLETE").assertIsDisplayed()
        composeRule.onNodeWithText("Morning Focus").assertIsDisplayed()
        composeRule.onNodeWithText("45m").assertIsDisplayed()
        composeRule.onNodeWithTag("scheduled_success_countdown").assertDoesNotExist()
        composeRule.onNodeWithTag("scheduled_success_dismiss_button").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertTrue(dismissed)
    }
}
