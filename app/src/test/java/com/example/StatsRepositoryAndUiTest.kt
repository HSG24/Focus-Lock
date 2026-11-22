package com.example

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.core.app.ApplicationProvider
import com.example.data.FocusSettingsRepository
import com.example.data.FocusStatsRepository
import com.example.data.FocusWhatsNewRepository
import com.example.data.FocusXpRepository
import com.example.data.GitHubReleaseInfo
import com.example.data.LockAppLayout
import com.example.data.StatsTimeframe
import com.example.data.UpdateUiState
import com.example.ui.components.UpdateBottomSheet
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.theme.AppVisualTheme
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class StatsRepositoryAndUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("focus_stats_cache", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("focus_xp_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("focus_settings", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("focus_whats_new_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun offlineSessionRecording_persistsLocallyWithoutZeroReset() {
        val now = System.currentTimeMillis()
        FocusStatsRepository.recordCompletedSession(
            context = context,
            startTimeMs = now - 25 * 60_000L,
            durationMs = 25 * 60_000L
        )
        val cached = FocusStatsRepository.getCachedSessions(context)
        assertTrue(cached.any { it.duration == 25 * 60_000L })

        val state = FocusStatsRepository.observe(context).value
        assertTrue(state.sessions.any { it.duration == 25 * 60_000L })
    }

    @Test
    fun periodReport_unifiesMonthClippedWeeksAcrossWeekAndMonthTabs() {
        val nov15Anchor = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 15, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val nov1Session = (nov15Anchor.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 10)
        }.timeInMillis
        val nov11Session = (nov15Anchor.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 11)
            set(Calendar.HOUR_OF_DAY, 14)
        }.timeInMillis
        val oct31Session = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 31, 16, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val sessions = listOf(
            Session(startTime = oct31Session, duration = 40 * 60_000L),
            Session(startTime = nov1Session, duration = 25 * 60_000L),
            Session(startTime = nov11Session, duration = 45 * 60_000L)
        )

        // MONTH report for Nov 2026 anchored on Nov 15 renders ONLY passed & current days ("1".."15")
        // and hides upcoming days ("16".."30") until they start.
        val novMonthReport = FocusStatsRepository.buildPeriodReport(
            sessions = sessions,
            timeframe = StatsTimeframe.MONTH,
            selectedDate = nov15Anchor,
            dailyGoalMinutes = 300L,
            nowCal = nov15Anchor
        )
        assertEquals(15, novMonthReport.chartBuckets.size)
        assertEquals((1..15).map { "$it" }, novMonthReport.chartBuckets.map { it.axisLabel })
        assertEquals(25 * 60_000L, novMonthReport.chartBuckets[0].durationMs)
        assertEquals(45 * 60_000L, novMonthReport.chartBuckets[10].durationMs)
        assertTrue(novMonthReport.chartBuckets[14].isCurrentPeriod)

        // Mid-week check (Wed Nov 11): WEEK shows only Mon 9, Tue 10, Wed 11 (hides Thu 12..Sun 15)
        val nov11Anchor = (nov15Anchor.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 11) }
        val midWeekReport = FocusStatsRepository.buildPeriodReport(
            sessions = sessions,
            timeframe = StatsTimeframe.WEEK,
            selectedDate = nov11Anchor,
            dailyGoalMinutes = 300L,
            nowCal = nov11Anchor
        )
        assertEquals("CURRENT WEEK", midWeekReport.headerDateLabel)
        assertEquals(3, midWeekReport.chartBuckets.size)

        // WEEK tab steps through the exact month-clipped weeks:
        // Nov 9–15 -> Nov 2–8 -> Nov 1 -> Oct 26–31
        val currentWeek = FocusStatsRepository.buildPeriodReport(
            sessions = sessions,
            timeframe = StatsTimeframe.WEEK,
            selectedDate = nov15Anchor,
            dailyGoalMinutes = 300L,
            nowCal = nov15Anchor
        )
        assertEquals("CURRENT WEEK", currentWeek.headerDateLabel)
        assertEquals(novMonthReport.chartBuckets[10].durationMs, currentWeek.totalFocusMs)

        val step1Cal = FocusStatsRepository.stepMonthClippedWeekBackward(nov15Anchor)
        val nov2to8 = FocusStatsRepository.buildPeriodReport(
            sessions = sessions,
            timeframe = StatsTimeframe.WEEK,
            selectedDate = step1Cal,
            dailyGoalMinutes = 300L,
            nowCal = nov15Anchor
        )
        assertEquals("NOV 2 – NOV 8, 2026", nov2to8.headerDateLabel)
        assertEquals(7, nov2to8.chartBuckets.size)
        assertEquals(0L, nov2to8.totalFocusMs)

        val step2Cal = FocusStatsRepository.stepMonthClippedWeekBackward(step1Cal)
        val nov1Week = FocusStatsRepository.buildPeriodReport(
            sessions = sessions,
            timeframe = StatsTimeframe.WEEK,
            selectedDate = step2Cal,
            dailyGoalMinutes = 300L,
            nowCal = nov15Anchor
        )
        assertEquals("NOV 1, 2026", nov1Week.headerDateLabel)
        assertEquals(1, nov1Week.chartBuckets.size)
        assertEquals(novMonthReport.chartBuckets[0].durationMs, nov1Week.totalFocusMs)

        val step3Cal = FocusStatsRepository.stepMonthClippedWeekBackward(step2Cal)
        val octLastWeek = FocusStatsRepository.buildPeriodReport(
            sessions = sessions,
            timeframe = StatsTimeframe.WEEK,
            selectedDate = step3Cal,
            dailyGoalMinutes = 300L,
            nowCal = nov15Anchor
        )
        assertEquals("OCT 26 – OCT 31, 2026", octLastWeek.headerDateLabel)
        assertEquals(6, octLastWeek.chartBuckets.size)
        assertEquals(40 * 60_000L, octLastWeek.totalFocusMs)

        // Forward step from Oct 26–31 lands back on Nov 1 (never skipping partial month-start week)
        val fwdFromOct = FocusStatsRepository.stepMonthClippedWeekForward(step3Cal, nov15Anchor)
        val nov1Again = FocusStatsRepository.buildPeriodReport(
            sessions = sessions,
            timeframe = StatsTimeframe.WEEK,
            selectedDate = fwdFromOct,
            dailyGoalMinutes = 300L,
            nowCal = nov15Anchor
        )
        assertEquals("NOV 1, 2026", nov1Again.headerDateLabel)

        // Completed past MONTH report for Oct 2026 renders all 31 days ("1".."31")
        val octReport = FocusStatsRepository.buildPeriodReport(
            sessions = sessions,
            timeframe = StatsTimeframe.MONTH,
            selectedDate = step3Cal,
            dailyGoalMinutes = 300L,
            nowCal = nov15Anchor
        )
        assertEquals(31, octReport.chartBuckets.size)
        assertEquals((1..31).map { "$it" }, octReport.chartBuckets.map { it.axisLabel })
        assertEquals(40 * 60_000L, octReport.chartBuckets[30].durationMs)
    }

    @Test
    fun statsScreen_lowFocusHidesGoalLine_rendersGoalCaption_andTodayVsCurrentJumpButtons() {
        val now = System.currentTimeMillis()
        // Place the 35-minute session firmly inside today's local hour 10 so both empty & populated bars are deterministic
        val sessionStartCal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 10)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val sessionStartMs = minOf(sessionStartCal.timeInMillis, now - 40 * 60_000L)
        val populatedHour = Calendar.getInstance().apply { timeInMillis = sessionStartMs }.get(Calendar.HOUR_OF_DAY)
        val emptyHour = (populatedHour + 6) % 24

        // 35 minutes (< 50% of 300m daily goal = 150m) -> dashed goal line stays hidden, caption shown above chart
        FocusXpRepository.recordCompletedSession(
            context = context,
            sessionStartTimeMs = sessionStartMs,
            durationMs = 35 * 60_000L,
            dailyGoalMinutes = 300L
        )
        FocusStatsRepository.recordCompletedSession(
            context = context,
            startTimeMs = sessionStartMs,
            durationMs = 35 * 60_000L
        )

        var activeTheme by mutableStateOf(AppVisualTheme.NOTHING_CRIMSON)
        composeRule.setContent {
            MyApplicationTheme(appTheme = activeTheme) {
                StatsScreen(
                    dailyGoalMinutes = 300L,
                    onOpenShare = {}
                )
            }
        }

        // DAY tab: stepping back shows TODAY pill
        composeRule.onNodeWithTag("prev_day_button").performClick()
        composeRule.onNodeWithTag("jump_today_button").assertIsDisplayed()
        composeRule.onNodeWithText("TODAY").assertIsDisplayed()
        composeRule.onNodeWithTag("jump_today_button").performClick()

        // Tap an empty (0s) bar -> MUST NOT select or show selected_period_pill
        composeRule.onNodeWithTag("stats_bar_chart_canvas").performTouchInput {
            val emptyX = width * ((emptyHour + 0.5f) / 24f)
            click(Offset(emptyX, center.y))
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("selected_period_pill").assertDoesNotExist()

        // Tap the bar WITH focus data -> selects, tap same bar again -> deselects
        composeRule.onNodeWithTag("stats_bar_chart_canvas").performTouchInput {
            val dataX = width * ((populatedHour + 0.5f) / 24f)
            click(Offset(dataX, center.y))
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("selected_period_pill").assertIsDisplayed()
        composeRule.onNodeWithTag("stats_bar_chart_canvas").performTouchInput {
            val dataX = width * ((populatedHour + 0.5f) / 24f)
            click(Offset(dataX, center.y))
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("selected_period_pill").assertDoesNotExist()

        // WEEK tab: 35m < 150m -> caption "5h/day goal" above chart, no dashed goal line badge
        composeRule.onNodeWithTag("timeframe_week_tab").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("chart_goal_caption").assertIsDisplayed()
        composeRule.onNodeWithText("5h/day goal").assertIsDisplayed()
        composeRule.onNodeWithTag("chart_goal_line_badge").assertDoesNotExist()

        // Stepping back in WEEK mode shows CURRENT pill (not TODAY)
        composeRule.onNodeWithTag("prev_day_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("CURRENT").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithText("TODAY").fetchSemanticsNodes().size)
        composeRule.onNodeWithTag("jump_today_button").performClick()

        // MONTH tab: daily bars & every single bar has its date underneath + 5h/day goal caption
        composeRule.onNodeWithTag("timeframe_month_tab").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("chart_goal_caption").assertIsDisplayed()
        composeRule.onNodeWithText("5h/day goal").assertIsDisplayed()
        composeRule.onNodeWithTag("chart_goal_line_badge").assertDoesNotExist()
        val todayDom = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        for (day in 1..todayDom) {
            composeRule.onNodeWithTag("month_axis_label_$day").assertExists()
        }
        composeRule.onNodeWithTag("prev_day_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("CURRENT").assertIsDisplayed()
    }

    @Test
    fun statsScreen_highFocusDrawsDashedGoalLineWhenHalfGoalMet() {
        val now = System.currentTimeMillis()
        // 180 minutes (>= 50% of 300m daily goal = 150m) -> dashed goal line & badge shown inside chart
        FocusStatsRepository.recordCompletedSession(
            context = context,
            startTimeMs = now - 200 * 60_000L,
            durationMs = 180 * 60_000L
        )

        composeRule.setContent {
            MyApplicationTheme(appTheme = AppVisualTheme.LIQUID_GLASS) {
                StatsScreen(
                    dailyGoalMinutes = 300L,
                    onOpenShare = {}
                )
            }
        }

        composeRule.onNodeWithTag("timeframe_week_tab").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("chart_goal_line_badge").assertIsDisplayed()
        composeRule.onNodeWithTag("chart_goal_caption").assertDoesNotExist()
    }

    @Test
    fun lockScreenLayoutSettings_removesBentoGrid_andAdjustsMinimalPillButtonSize() {
        // Legacy "GLASS_GRID" stored preference falls back cleanly to PILL_ROW
        context.getSharedPreferences("focus_settings", Context.MODE_PRIVATE)
            .edit()
            .putString("lock_app_layout", "GLASS_GRID")
            .commit()
        val migrated = FocusSettingsRepository.getCurrent(context)
        assertEquals(LockAppLayout.PILL_ROW, migrated.lockAppLayout)
        assertFalse(LockAppLayout.entries.any { it.title == "Bento Grid" })

        composeRule.setContent {
            val observedSettings by FocusSettingsRepository.observe(context).collectAsStateWithLifecycle()
            MyApplicationTheme(appTheme = AppVisualTheme.NOTHING_CRIMSON) {
                SettingsScreen(
                    settingsState = observedSettings,
                    installedVersion = "1.0.0",
                    updateUiState = UpdateUiState.Idle,
                    onCheckForUpdates = {},
                    onOpenUpdateSheet = {}
                )
            }
        }

        // Bento Grid option is absent; Minimal Pills + Pill Button Size slider are displayed
        composeRule.onNodeWithText("Bento Grid").assertDoesNotExist()
        composeRule.onNodeWithTag("pill_size_control_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("pill_size_slider").assertIsDisplayed()

        // Persisting 28dp updates live size readout
        FocusSettingsRepository.setMinimalPillButtonSizeDp(context, 28)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("28dp").assertIsDisplayed()

        // Switching to Floating Dock hides Minimal Pill size control
        composeRule.onNodeWithTag("layout_option_icon_dock").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("pill_size_control_card").assertDoesNotExist()
    }

    @Test
    fun updateBottomSheet_hidesReleaseNotesSection() {
        val release = GitHubReleaseInfo(
            tagName = "v2.0.0",
            releaseName = "Release 2.0",
            releaseNotes = "Hidden changelog body text",
            apkAssetUrl = "https://example.com/app.apk",
            apkFileName = "app-release.apk",
            apkSizeBytes = 12_000_000L
        )
        composeRule.setContent {
            MyApplicationTheme(appTheme = AppVisualTheme.NOTHING_CRIMSON) {
                UpdateBottomSheet(
                    installedVersion = "1.0.0",
                    updateState = UpdateUiState.UpdateAvailable(release),
                    release = release,
                    onUpdateNow = {},
                    onOpenInstallPermission = {},
                    onInstallDownloaded = {},
                    onDismiss = {}
                )
            }
        }

        composeRule.onNodeWithText("Update Available").assertIsDisplayed()
        composeRule.onNodeWithTag("update_now_button").assertIsDisplayed()
        composeRule.onNodeWithText("RELEASE NOTES").assertDoesNotExist()
    }

    @Test
    fun whatsNewRepository_triggersOnVersionUpdateAndClearsWhenSeen() {
        assertTrue(
            FocusWhatsNewRepository.shouldShowAutoWhatsNew(
                context = context,
                installedVersionName = "1.0.5",
                installedVersionCode = 5L,
                currentReleaseId = 2
            )
        )

        FocusWhatsNewRepository.markWhatsNewSeen(
            context = context,
            installedVersionName = "1.0.5",
            installedVersionCode = 5L,
            currentReleaseId = 2
        )

        assertFalse(
            FocusWhatsNewRepository.shouldShowAutoWhatsNew(
                context = context,
                installedVersionName = "1.0.5",
                installedVersionCode = 5L,
                currentReleaseId = 2
            )
        )

        // Updating to a newer app version automatically triggers What's New again
        assertTrue(
            FocusWhatsNewRepository.shouldShowAutoWhatsNew(
                context = context,
                installedVersionName = "1.0.6",
                installedVersionCode = 6L,
                currentReleaseId = 2
            )
        )

        // Bumping releaseId from 4 to 5 on same version also triggers What's New
        assertTrue(
            FocusWhatsNewRepository.shouldShowAutoWhatsNew(
                context = context,
                installedVersionName = "1.0.5",
                installedVersionCode = 5L,
                currentReleaseId = 5
            )
        )
    }

    @Test
    fun settingsScreen_whatsNewButtonRemainsAccessible_andOpensBottomSheet() {
        composeRule.setContent {
            val observedSettings by FocusSettingsRepository.observe(context).collectAsStateWithLifecycle()
            MyApplicationTheme(appTheme = AppVisualTheme.NOTHING_CRIMSON) {
                SettingsScreen(
                    settingsState = observedSettings,
                    installedVersion = "1.0.5",
                    updateUiState = UpdateUiState.Idle,
                    onCheckForUpdates = {},
                    onOpenUpdateSheet = {}
                )
            }
        }

        composeRule.onNodeWithTag("whats_new_settings_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("whats_new_settings_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("whats_new_bottom_sheet").assertIsDisplayed()
        composeRule.onNodeWithText("v1.0.5").assertIsDisplayed()
        composeRule.onNodeWithTag("whats_new_section_new").assertIsDisplayed()
        composeRule.onNodeWithText("NEW").assertIsDisplayed()
        composeRule.onNodeWithText("3-Second Scheduled Completion Screen").assertIsDisplayed()
        composeRule.onNodeWithText("Cleaner Scheduled Lock & Medium Goal Pill").assertIsDisplayed()
        composeRule.onNodeWithText("1-Minute Schedule Time Picker").assertIsDisplayed()
        composeRule.onNodeWithText("Streamlined Scheduled Lock Screen").assertIsDisplayed()
        composeRule.onNodeWithTag("whats_new_section_fixes").assertIsDisplayed()
        composeRule.onNodeWithText("Reliable Scheduled Focus Triggers").assertIsDisplayed()
        composeRule.onNodeWithText("Removed Nothing Island").assertIsDisplayed()
        composeRule.onNodeWithTag("whats_new_previous_toggle").assertDoesNotExist()

        composeRule.onNodeWithTag("whats_new_got_it_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("whats_new_bottom_sheet").assertDoesNotExist()
    }

    @Test
    fun focusHeatmapBuilder_computesIntensityLevelsAndActiveCountsAccurately() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 19, 14, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayMs = now.timeInMillis
        val yesterdayMs = (now.clone() as Calendar).apply { add(Calendar.DATE, -1) }.timeInMillis
        val twoDaysAgoMs = (now.clone() as Calendar).apply { add(Calendar.DATE, -2) }.timeInMillis

        val sessions = listOf(
            Session(startTime = todayMs, duration = 60L * 60_000L),        // 100% of 60m goal -> level 4
            Session(startTime = yesterdayMs, duration = 45L * 60_000L),    // 75% -> level 3
            Session(startTime = twoDaysAgoMs, duration = 15L * 60_000L)    // 25% -> level 1
        )

        val heatmap = FocusStatsRepository.buildHeatmapData(
            sessions = sessions,
            dailyGoalMinutes = 60L,
            weeksCount = 14,
            nowCal = now
        )

        assertEquals(14, heatmap.weeks.size)
        assertTrue(heatmap.weeks.all { it.days.size == 7 })
        assertEquals(3, heatmap.activeDaysCount)
        assertEquals(1, heatmap.goalMetDaysCount)

        val allCells = heatmap.weeks.flatMap { it.days }
        val todayCell = allCells.first { it.isToday }
        assertEquals(4, todayCell.intensityLevel)
        assertTrue(todayCell.isGoalMet)
        assertEquals(1, todayCell.sessionCount)
    }

    @Test
    fun blockedSummaryBottomSheet_expandsAndCollapsesExtraNotifications() {
        val baseTime = 1_700_000_000_000L
        com.example.data.NotificationBlockerRepository.startFreshSession(context)
        // Log 6 distinct notifications for WhatsApp (newest 4 shown by default, 2 hidden)
        for (i in 1..6) {
            com.example.data.NotificationBlockerRepository.recordBlockedNotification(
                context = context,
                packageName = "com.whatsapp",
                appLabel = "WhatsApp",
                title = "Sender $i",
                snippet = "Unique message #$i",
                timestampMs = baseTime + i * 1000L
            )
        }
        assertTrue(com.example.data.NotificationBlockerRepository.finalizeSessionOnUnlock(context))
        val blockerState = com.example.data.NotificationBlockerRepository.getCurrent(context)

        composeRule.setContent {
            MyApplicationTheme(appTheme = AppVisualTheme.NOTHING_CRIMSON) {
                com.example.ui.components.BlockedSummaryBottomSheet(
                    blockerState = blockerState,
                    onDoneClearLog = {},
                    onDismiss = {}
                )
            }
        }

        // Top 4 (Sender 6..3) are visible; Sender 1 & 2 are collapsed under "+2 more notifications"
        composeRule.onNodeWithText("Sender 6").assertIsDisplayed()
        composeRule.onNodeWithText("Sender 1").assertDoesNotExist()
        composeRule.onNodeWithText("+2 more notifications").assertIsDisplayed()

        // Expand
        composeRule.onNodeWithTag("expand_notifications_toggle_com.whatsapp").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Sender 1").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Show less").assertIsDisplayed()

        // Collapse (reduce back down)
        composeRule.onNodeWithTag("expand_notifications_toggle_com.whatsapp").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Sender 1").assertDoesNotExist()
        composeRule.onNodeWithText("+2 more notifications").assertIsDisplayed()
    }
}
