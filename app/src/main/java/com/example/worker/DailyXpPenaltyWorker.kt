package com.example.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.FocusSettingsRepository
import com.example.data.FocusXpRepository
import com.example.fetchAllSessions
import com.example.widget.NothingFocusWidgetUpdater
import java.util.Calendar
import java.util.concurrent.TimeUnit

class DailyXpPenaltyWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val settings = FocusSettingsRepository.getCurrent(applicationContext)
        val sessions = runCatching { fetchAllSessions() }.getOrDefault(emptyList())
        if (sessions.isNotEmpty()) {
            FocusXpRepository.syncWithRemoteSessions(
                context = applicationContext,
                sessions = sessions,
                dailyGoalMinutes = settings.dailyGoalMinutes
            )
        } else {
            FocusXpRepository.evaluateMissedDailyGoals(
                context = applicationContext,
                dailyGoalMinutes = settings.dailyGoalMinutes
            )
        }
        NothingFocusWidgetUpdater.updateAllWidgets(applicationContext)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "focus_lock_daily_xp_penalty_check"

        fun scheduleMidnightCheck(context: Context) {
            runCatching {
                val now = Calendar.getInstance()
                val nextMidnight = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 5)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val initialDelayMs = (nextMidnight.timeInMillis - now.timeInMillis)
                    .coerceAtLeast(TimeUnit.MINUTES.toMillis(5))

                val request = PeriodicWorkRequestBuilder<DailyXpPenaltyWorker>(
                    24,
                    TimeUnit.HOURS
                )
                    .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
                    .build()

                WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
            }
        }
    }
}
