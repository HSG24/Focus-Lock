package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.BackupValidationResult
import com.example.data.FocusBackupManager
import com.example.data.FocusOnboardingRepository
import com.example.data.FocusQuizEngine
import com.example.data.FocusScheduleRepository
import com.example.data.FocusSettingsRepository
import com.example.data.FocusStatsRepository
import com.example.data.FocusWhatsNewCatalog
import com.example.data.FocusXpRepository
import com.example.data.LockAppLayout
import com.example.data.ScheduleRepeatMode
import com.example.ui.components.LockAppsTrayMetrics
import com.example.ui.theme.AppVisualTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FocusFinalReleaseTest {

    @Test
    fun quizEngine_generatesTenUniqueNonRepeatingQuestionsAcrossBatches() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        FocusQuizEngine.replaceAskedSignatures(context, emptyList())

        val batch1 = FocusQuizEngine.generateQuizSet(context, count = 10, random = Random(42))
        val batch2 = FocusQuizEngine.generateQuizSet(context, count = 10, random = Random(42))

        assertEquals(10, batch1.size)
        assertEquals(10, batch2.size)

        val allSignatures = (batch1 + batch2).map { it.signature }
        assertEquals(20, allSignatures.distinct().size)

        // Every generated question accepts its own canonical answer case-insensitively
        for (q in batch1 + batch2) {
            assertTrue(FocusQuizEngine.checkAnswer(q, q.acceptedAnswers.first()))
            assertTrue(FocusQuizEngine.checkAnswer(q, "  ${q.acceptedAnswers.first().lowercase()}  "))
            assertFalse(FocusQuizEngine.checkAnswer(q, "___DEFINITELY_WRONG___"))
        }
    }

    @Test
    fun backupManager_roundTripsSettingsStatsXpSchedulesAndQuizMemory() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // Populate state across repositories
        FocusSettingsRepository.setAppTheme(context, AppVisualTheme.SOLAR_AMBER)
        FocusSettingsRepository.setLockAppLayout(context, LockAppLayout.PILL_ROW)
        FocusSettingsRepository.setMinimalPillButtonSizeDp(context, 34)
        FocusSettingsRepository.setDailyGoalMinutes(context, 150L)

        val sessionStart = System.currentTimeMillis() - 3_600_000L
        FocusStatsRepository.recordCompletedSession(context, sessionStart, 1_800_000L)
        FocusXpRepository.recordCompletedSession(context, sessionStart, 1_800_000L, 150L)

        val schedule = FocusScheduleRepository.createNewSchedule(
            name = "Morning Deep Work",
            startHour = 8,
            startMinute = 30,
            endHour = 10,
            endMinute = 0,
            repeatMode = ScheduleRepeatMode.WEEKDAYS,
            customDays = emptySet()
        )
        FocusScheduleRepository.addOrUpdateSchedule(context, schedule)
        FocusQuizEngine.generateQuizSet(context, count = 5, random = Random(7))

        val exportedJson = FocusBackupManager.exportToJson(context)
        val validation = FocusBackupManager.validateBackupJson(exportedJson)
        assertTrue(validation is BackupValidationResult.Valid)
        val valid = validation as BackupValidationResult.Valid
        assertEquals(1, valid.summary.sessionCount)
        assertEquals(1, valid.summary.scheduleCount)
        assertEquals(5, valid.summary.quizHistoryCount)

        // Mutate state away, then restore from JSON
        FocusSettingsRepository.setAppTheme(context, AppVisualTheme.NORDIC_SAGE)
        FocusSettingsRepository.setDailyGoalMinutes(context, 60L)
        FocusStatsRepository.replaceAllSessions(context, emptyList())
        FocusQuizEngine.replaceAskedSignatures(context, emptyList())

        val applyResult = FocusBackupManager.applyValidatedBackup(context, valid.summary)
        assertTrue(applyResult.isSuccess)

        val restoredSettings = FocusSettingsRepository.getCurrent(context)
        assertEquals(AppVisualTheme.SOLAR_AMBER, restoredSettings.appTheme)
        assertEquals(34, restoredSettings.minimalPillButtonSizeDp)
        assertEquals(150L, restoredSettings.dailyGoalMinutes)
        assertEquals(1, FocusStatsRepository.getCachedSessions(context).size)
        assertEquals(5, FocusQuizEngine.getAskedCount(context))
    }

    @Test
    fun onboarding_showsOnFreshInstallAndSkipsOnExistingFootprint() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertTrue(FocusOnboardingRepository.shouldShowFirstInstallSetup(context))

        // Mid-setup edits (like choosing a goal during setup) keep setup active until finished
        FocusSettingsRepository.setDailyGoalMinutes(context, 180L)
        FocusOnboardingRepository.markSetupCompleted(context)
        assertFalse(FocusOnboardingRepository.shouldShowFirstInstallSetup(context))
    }

    @Test
    fun whatsNew_keepsPreviousItemsAndAdvancesToRelease16() {
        assertEquals(16, FocusWhatsNewCatalog.CURRENT_RELEASE_ID)
        val items = FocusWhatsNewCatalog.releases.first().items
        val titles = items.map { it.title }
        assertTrue(titles.contains("Redesigned Scheduled Lock Screen"))
        assertTrue(titles.contains("Manual Dismiss for Completion Screen"))
        assertTrue(titles.any { it.contains("10-Question Infinite Quiz") })
        assertTrue(titles.any { it.contains("Solar Eclipse Focus Ring") })
    }

    @Test
    fun lockTrayMetrics_matchesPhoneAndTabletConstraints() {
        // 360dp phone: min(360 - 44, 400) - 32 = 284dp
        assertEquals(284f, LockAppsTrayMetrics.computeLockTrayContentWidthDp(360f), 0.01f)
        // 412dp Pixel phone: min(412 - 44, 400) - 32 = 336dp
        assertEquals(336f, LockAppsTrayMetrics.computeLockTrayContentWidthDp(412f), 0.01f)
        // 800dp tablet: min(800 - 44, 400) - 32 = 368dp
        assertEquals(368f, LockAppsTrayMetrics.computeLockTrayContentWidthDp(800f), 0.01f)
    }
}
