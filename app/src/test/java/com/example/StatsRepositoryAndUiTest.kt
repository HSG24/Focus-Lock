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
import androidx.test.core.app.ApplicationProvider
import com.example.data.FocusStatsRepository
import com.example.data.FocusXpRepository
import com.example.data.GitHubReleaseInfo
import com.example.data.StatsTimeframe
import com.example.data.UpdateUiState
import com.example.ui.components.UpdateBottomSheet
import com.example.ui.screens.StatsScreen
import com.example.ui.theme.AppVisualTheme
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Assert.assertEquals
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
    fun periodReport_computesCurrentWeekLabel_passedOnlyBars_and4WeekMonthDistribution() {
        // Wednesday Nov 11, 2026 (mid-week)
        val midWeekAnchor = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 11, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val mondaySession = (midWeekAnchor.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 9)
            set(Calendar.HOUR_OF_DAY, 10)
        }.timeInMillis
        val wednesdaySession = (midWeekAnchor.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 11)
            set(Calendar.HOUR_OF_DAY, 14)
        }.timeInMillis

        val sessions = listOf(
            Session(startTime = mondaySession, duration = 30 * 60_000L),
            Session(startTime = wednesdaySession, duration = 45 * 60_000L)
        )

        // Current week on Wed Nov 11: header must say CURRENT WEEK and show only Mon 9, Tue 10, Wed 11 (3 bars)
        val currentWeekReport = FocusStatsRepository.buildPeriodReport(
            sessions = sessions,
            timeframe = StatsTimeframe.WEEK,
            selectedDate = midWeekAnchor,
            dailyGoalMinutes = 300L,
            nowCal = midWeekAnchor
        )
        assertEquals("CURRENT WEEK", currentWeekReport.headerDateLabel)
        assertEquals(3, currentWeekReport.chartBuckets.size)
        assertEquals(listOf("Mon\n9", "Tue\n10", "Wed\n11"), currentWeekReport.chartBuckets.map { it.axisLabel })

        // Previous completed week (Nov 2 – Nov 8, 2026): header shows date range and all 7 days
        val prevWeekCal = (midWeekAnchor.clone() as Calendar).apply { add(Calendar.DATE, -7) }
        val prevWeekReport = FocusStatsRepository.buildPeriodReport(
            sessions = sessions,
            timeframe = StatsTimeframe.WEEK,
            selectedDate = prevWeekCal,
            dailyGoalMinutes = 300L,
            nowCal = midWeekAnchor
        )
        assertEquals("NOV 2 – NOV 8, 2026", prevWeekReport.headerDateLabel)
        assertEquals(7, prevWeekReport.chartBuckets.size)

        // Current month on Nov 15, 2026: only weeks 1..3 have started (1–7, 8–14, 15–21); week 4 (22–30) hidden until Nov 22
        val nov15 = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 15, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val novMidMonthReport = FocusStatsRepository.buildPeriodReport(
            sessions = sessions,
            timeframe = StatsTimeframe.MONTH,
            selectedDate = nov15,
            dailyGoalMinutes = 300L,
            nowCal = nov15
        )
        assertEquals(3, novMidMonthReport.chartBuckets.size)
        assertEquals(
            listOf("Nov\n1–7", "Nov\n8–14", "Nov\n15–21"),
            novMidMonthReport.chartBuckets.map { it.axisLabel }
        )

        // Past month October 2026 (31 days): 4 weeks starting day 1, remaining 3 days kept in Oct (22–31)
        val oct10 = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 10, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val octReport = FocusStatsRepository.buildPeriodReport(
            sessions = sessions,
            timeframe = StatsTimeframe.MONTH,
            selectedDate = oct10,
            dailyGoalMinutes = 300L,
            nowCal = nov15
        )
        assertEquals(4, octReport.chartBuckets.size)
        assertEquals(
            listOf("Oct\n1–7", "Oct\n8–14", "Oct\n15–21", "Oct\n22–31"),
            octReport.chartBuckets.map { it.axisLabel }
        )
        assertEquals("Oct 22 – Oct 31", octReport.chartBuckets[3].tooltipTitle)

        // Day buckets still cover full 24h with 12AM/12PM labels
        val dayReport = FocusStatsRepository.buildPeriodReport(
            sessions = sessions,
            timeframe = StatsTimeframe.DAY,
            selectedDate = midWeekAnchor,
            dailyGoalMinutes = 300L,
            nowCal = midWeekAnchor
        )
        assertEquals(24, dayReport.chartBuckets.size)
        assertEquals("12AM", dayReport.chartBuckets[0].axisLabel)
        assertEquals("12PM", dayReport.chartBuckets[12].axisLabel)
    }

    @Test
    fun statsScreen_barTapTogglesDeselection_andRendersBestEmojiStreakAndCurrentWeek() {
        val now = System.currentTimeMillis()
        FocusXpRepository.recordCompletedSession(
            context = context,
            sessionStartTimeMs = now - 60 * 60_000L,
            durationMs = 55 * 60_000L,
            dailyGoalMinutes = 300L
        )
        FocusStatsRepository.recordCompletedSession(
            context = context,
            startTimeMs = now - 60 * 60_000L,
            durationMs = 55 * 60_000L
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

        // Day mode UI + Streak with Best label
        composeRule.onNodeWithTag("bento_metrics_grid").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("bento_streak_cell").assertIsDisplayed()
        composeRule.onNodeWithText("Best").assertIsDisplayed()
        composeRule.onNodeWithText("12PM").assertIsDisplayed()

        // Tap a bar -> selects period & swaps Bento grid
        composeRule.onNodeWithTag("stats_bar_chart_canvas").performTouchInput {
            click(Offset(center.x, center.y))
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("selected_period_pill").assertIsDisplayed()
        composeRule.onNodeWithText("TOTAL SESSIONS").assertIsDisplayed()
        composeRule.onNodeWithText("TOTAL TIME").assertIsDisplayed()

        // Tap the SAME bar again -> MUST deselect and restore default Bento metrics
        composeRule.onNodeWithTag("stats_bar_chart_canvas").performTouchInput {
            click(Offset(center.x, center.y))
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("selected_period_pill").assertDoesNotExist()
        composeRule.onNodeWithText("AVG SESSION").assertIsDisplayed()

        // Tap bar again, then tap outside -> also deselects
        composeRule.onNodeWithTag("stats_bar_chart_canvas").performTouchInput {
            click(Offset(center.x, center.y))
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("selected_period_pill").assertIsDisplayed()
        composeRule.onNodeWithTag("bento_metrics_grid").performTouchInput {
            click(Offset(center.x, center.y))
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("selected_period_pill").assertDoesNotExist()

        // Switch to WEEK -> shows CURRENT WEEK label & hides 7-day strip
        composeRule.onNodeWithTag("timeframe_week_tab").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("CURRENT WEEK").assertIsDisplayed()
        composeRule.onNodeWithTag("week_strip_day_0").assertDoesNotExist()

        // Switch to MONTH -> hidden 7-day strip & monthly consistency
        composeRule.onNodeWithTag("timeframe_month_tab").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("MONTHLY CONSISTENCY").assertIsDisplayed()
        composeRule.onNodeWithTag("week_strip_day_0").assertDoesNotExist()

        // Switch back to DAY & verify all themes
        composeRule.onNodeWithTag("timeframe_day_tab").performClick()
        composeRule.waitForIdle()
        for (theme in listOf(
            AppVisualTheme.CYBER_ICE,
            AppVisualTheme.SOLAR_AMBER,
            AppVisualTheme.ULTRAVIOLET_PULSE
        )) {
            activeTheme = theme
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("hero_infographic_card").assertIsDisplayed()
        }

        // Open Rank & XP bottom sheet -> shows CURRENT badge (not ACTIVE)
        composeRule.onNodeWithTag("top_bar_rank_xp_pill").performClick()
        composeRule.onNodeWithTag("rank_xp_bottom_sheet").assertIsDisplayed()
        composeRule.onNodeWithText("CURRENT").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithText("ACTIVE").fetchSemanticsNodes().size)
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
        composeRule.onNodeWithText("Hidden changelog body text").assertDoesNotExist()
    }
}
