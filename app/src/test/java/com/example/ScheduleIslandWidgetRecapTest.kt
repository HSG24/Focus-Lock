package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.data.DynamicIslandConfig
import com.example.data.DynamicIslandEvent
import com.example.data.DynamicIslandRepository
import com.example.data.FocusSchedule
import com.example.data.FocusScheduleRepository
import com.example.data.FocusXpState
import com.example.data.LockModuleAlignment
import com.example.data.LockModuleSize
import com.example.data.MilestoneKind
import com.example.data.NothingWidgetKind
import com.example.data.ScheduleRepeatMode
import com.example.data.ScheduledBottomDockMode
import com.example.data.ScheduledLockModuleType
import com.example.data.WidgetCornerStyle
import com.example.data.WidgetStudioRepository
import com.example.data.WidgetSurfaceStyle
import com.example.ui.components.NothingDynamicIslandPill
import com.example.ui.components.RecapCardOptions
import com.example.ui.components.RecapCardTheme
import com.example.ui.components.RecapTimeframe
import com.example.ui.components.ShareRecapCardRenderer
import com.example.ui.theme.AppVisualTheme
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
        context.getSharedPreferences("focus_dynamic_island_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("focus_widget_studio_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun focusScheduleRepository_handlesOvernightSkipAndLayoutCustomizations() {
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

        // Lock Studio mutations: Countdown timer stays locked on; Session Timeline & Bottom Dock customize cleanly
        FocusScheduleRepository.toggleModule(context, ScheduledLockModuleType.COUNTDOWN_TIMER, false)
        assertTrue(
            FocusScheduleRepository.getCurrent(context).layoutConfig.modules
                .first { it.type == ScheduledLockModuleType.COUNTDOWN_TIMER }.enabled
        )

        FocusScheduleRepository.setModuleSize(context, ScheduledLockModuleType.SESSION_TIMELINE, LockModuleSize.LARGE)
        FocusScheduleRepository.setModuleAlignment(context, ScheduledLockModuleType.SESSION_TIMELINE, LockModuleAlignment.START)
        FocusScheduleRepository.moveModule(context, ScheduledLockModuleType.SESSION_TIMELINE, -1)
        FocusScheduleRepository.setBottomDockMode(context, ScheduledBottomDockMode.COMPACT)

        val updatedLayout = FocusScheduleRepository.getCurrent(context).layoutConfig
        val timelineMod = updatedLayout.modules.first { it.type == ScheduledLockModuleType.SESSION_TIMELINE }
        assertEquals(LockModuleSize.LARGE, timelineMod.size)
        assertEquals(LockModuleAlignment.START, timelineMod.alignment)
        assertEquals(ScheduledBottomDockMode.COMPACT, updatedLayout.bottomDockMode)
    }

    @Test
    fun dynamicIsland_skipTodayAppearsOnlyWhenScheduleIslandIsExpanded() {
        val event = DynamicIslandEvent.UpcomingSchedule(
            id = "warn_1",
            scheduleId = "sched_1",
            scheduleName = "Morning Focus",
            timeRangeLabel = "9:00 AM → 10:30 AM",
            startsAtMs = System.currentTimeMillis() + 10 * 60_000L
        )
        var skipped = false

        composeRule.setContent {
            val expandedState = androidx.compose.runtime.remember {
                androidx.compose.runtime.mutableStateOf(false)
            }
            MyApplicationTheme(appTheme = AppVisualTheme.LIQUID_GLASS) {
                NothingDynamicIslandPill(
                    event = event,
                    config = DynamicIslandConfig(),
                    isExpanded = expandedState.value,
                    onToggleExpand = { expandedState.value = !expandedState.value },
                    onDismiss = {},
                    onSkipScheduleToday = { skipped = true },
                    onClearNotifications = {},
                    isInteractivePreview = true
                )
            }
        }

        // Compact pill MUST NOT show "Skip Today"
        composeRule.onNodeWithTag("island_skip_today_button").assertDoesNotExist()
        composeRule.onNodeWithText("MORNING FOCUS").assertIsDisplayed()

        // Expand via click -> "Skip Today" appears and triggers callback
        composeRule.onNodeWithTag("dynamic_island_compact").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("island_skip_today_button").assertIsDisplayed().performClick()
        assertTrue(skipped)
    }

    @Test
    fun dynamicIsland_evaluatesGoalAndStreakMilestones() {
        val prev = FocusXpState(totalXp = 100, todayFocusMs = 10 * 60_000L, currentStreak = 1)
        val updated = FocusXpState(totalXp = 500, todayFocusMs = 60 * 60_000L, currentStreak = 2)
        DynamicIslandRepository.evaluatePostSessionMilestones(
            context = context,
            previousXpState = prev,
            updatedXpState = updated,
            dailyGoalMinutes = 60L
        )
        val active = DynamicIslandRepository.observe(context).value.activeEvent
        assertTrue(active is DynamicIslandEvent.Milestone)
        assertEquals(MilestoneKind.GOAL_REACHED, (active as DynamicIslandEvent.Milestone).kind)

        // Live preview trigger sets timed auto-hide & increments pulse sequence
        val previewEvent = DynamicIslandEvent.Milestone(
            id = "preview_test",
            kind = MilestoneKind.GOAL_REACHED,
            title = "Preview",
            subtitle = "Test",
            badgeText = "100%"
        )
        DynamicIslandRepository.showLivePreview(context, previewEvent, durationMs = 3500L)
        val previewState = DynamicIslandRepository.observe(context).value
        assertEquals(3500L, previewState.previewAutoHideMs)
        assertTrue(previewState.previewPulseSeq >= 1L)

        DynamicIslandRepository.dismissActive(context)
        assertNull(DynamicIslandRepository.observe(context).value.activeEvent)
        assertNull(DynamicIslandRepository.observe(context).value.previewAutoHideMs)
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
}
