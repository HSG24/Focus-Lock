package com.example.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import com.example.LockService
import com.example.MainActivity
import com.example.data.FocusOnboardingRepository
import com.example.data.FocusSchedule
import com.example.data.FocusScheduleRepository
import com.example.data.UpcomingScheduleWindow

object ScheduleAlarmScheduler {
    const val ACTION_SCHEDULE_START = "com.example.action.SCHEDULE_LOCK_START"
    const val EXTRA_SCHEDULE_ID = "extra_schedule_id"
    const val EXTRA_SCHEDULE_NAME = "extra_schedule_name"
    const val EXTRA_START_TIME_MS = "extra_start_time_ms"
    const val EXTRA_END_TIME_MS = "extra_end_time_ms"

    private const val REQ_NEXT_START = 4101
    private const val REQ_SHOW_APP = 4103
    /** Small tolerance so alarms firing slightly early (or right on the second) still launch cleanly. */
    private const val EARLY_TRIGGER_TOLERANCE_MS = 15_000L

    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val am = context.applicationContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            ?: return false
        return runCatching { am.canScheduleExactAlarms() }.getOrDefault(false)
    }

    /**
     * Re-arms the next upcoming scheduled lock using [AlarmManager.setAlarmClock] when exact alarms
     * are allowed (highest-priority stock Android alarm: wakes Doze on the exact second and grants
     * Android 12+ background foreground-service start exemption without root).
     *
     * Also checks if any enabled schedule is active right now and starts it immediately.
     */
    fun syncAllAlarms(
        context: Context,
        nowMs: Long = System.currentTimeMillis(),
        triggerIfActiveNow: Boolean = true
    ) {
        val appCtx = context.applicationContext
        val am = appCtx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        cancelAlarm(appCtx, am, ACTION_SCHEDULE_START, REQ_NEXT_START)

        val state = FocusScheduleRepository.getCurrent(appCtx)
        val occurrences = state.schedules
            .mapNotNull { FocusScheduleRepository.computeNextOccurrence(it, nowMs) }
            .sortedBy { it.startTimeMs }

        if (triggerIfActiveNow) {
            val activeNow = occurrences.firstOrNull { it.isCurrentlyActive }
            if (activeNow != null) {
                startScheduledLockForWindow(appCtx, activeNow, nowMs)
            }
        }

        val nextFuture = occurrences
            .filter { !it.isCurrentlyActive && it.startTimeMs > nowMs }
            .minByOrNull { it.startTimeMs }
            ?: return

        val startIntent = Intent(appCtx, ScheduleAlarmReceiver::class.java).apply {
            action = ACTION_SCHEDULE_START
            putExtra(EXTRA_SCHEDULE_ID, nextFuture.schedule.id)
            putExtra(EXTRA_SCHEDULE_NAME, nextFuture.schedule.name)
            putExtra(EXTRA_START_TIME_MS, nextFuture.startTimeMs)
            putExtra(EXTRA_END_TIME_MS, nextFuture.endTimeMs)
        }
        scheduleReliableAlarm(appCtx, am, nextFuture.startTimeMs, startIntent, REQ_NEXT_START)
    }

    /**
     * Called by foreground ticker (when app is open) or AlarmReceiver to start any schedule
     * whose window is active right now or starts within [EARLY_TRIGGER_TOLERANCE_MS].
     * Returns true if a scheduled lock was triggered or conflict-skipped.
     */
    fun triggerActiveScheduleIfDue(
        context: Context,
        nowMs: Long = System.currentTimeMillis(),
        preferredScheduleId: String? = null
    ): Boolean {
        val appCtx = context.applicationContext
        val schedules = FocusScheduleRepository.getCurrent(appCtx).schedules

        val candidates = schedules
            .mapNotNull { FocusScheduleRepository.computeNextOccurrence(it, nowMs) }
            .filter { window ->
                window.isCurrentlyActive || (window.startTimeMs - nowMs) in 0L..EARLY_TRIGGER_TOLERANCE_MS
            }
            .sortedWith(
                compareByDescending<UpcomingScheduleWindow> { it.schedule.id == preferredScheduleId }
                    .thenBy { it.startTimeMs }
            )

        val due = candidates.firstOrNull() ?: return false
        return startScheduledLockForWindow(appCtx, due, nowMs)
    }

    internal fun startScheduledLockForWindow(
        context: Context,
        window: UpcomingScheduleWindow,
        nowMs: Long = System.currentTimeMillis()
    ): Boolean {
        val appCtx = context.applicationContext
        val schedule = window.schedule
        if (!schedule.enabled || FocusScheduleRepository.isSkippedToday(schedule, nowMs)) {
            return false
        }

        // Conflict rule: If a manual OTP focus session is already running when the
        // scheduled lock time arrives, automatically cancel/skip this schedule for today.
        if (LockService.isRunning && !LockService.isScheduledSession) {
            FocusScheduleRepository.markSkippedForToday(appCtx, schedule.id, nowMs)
            return true
        }

        // Already locked by a scheduled session
        if (LockService.isRunning) {
            return false
        }

        val safeEndMs = computeSafeEndTimeMs(schedule, window.endTimeMs, nowMs)
        if (!runCatching { Settings.canDrawOverlays(appCtx) }.getOrDefault(false)) {
            return false
        }

        val serviceIntent = Intent(appCtx, LockService::class.java).apply {
            putExtra(LockService.EXTRA_IS_SCHEDULED, true)
            putExtra(LockService.EXTRA_SCHEDULE_ID, schedule.id)
            putExtra(LockService.EXTRA_SCHEDULE_NAME, schedule.name)
            putExtra(LockService.EXTRA_SCHEDULE_TIME_RANGE, schedule.formattedTimeRange())
            putExtra(LockService.EXTRA_SCHEDULE_END_TIME_MS, safeEndMs)
        }

        val started = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                appCtx.startForegroundService(serviceIntent)
            } else {
                appCtx.startService(serviceIntent)
            }
            true
        }.getOrElse {
            runCatching {
                appCtx.startService(serviceIntent)
                true
            }.getOrDefault(false)
        }

        return started
    }

    private fun computeSafeEndTimeMs(
        schedule: FocusSchedule,
        windowEndMs: Long,
        nowMs: Long
    ): Long {
        return if (windowEndMs > nowMs + 10_000L) {
            windowEndMs
        } else {
            nowMs + schedule.durationMinutes().coerceAtLeast(1) * 60_000L
        }
    }

    private fun scheduleReliableAlarm(
        context: Context,
        alarmManager: AlarmManager,
        triggerAtMs: Long,
        intent: Intent,
        requestCode: Int
    ) {
        val operationPi = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showIntent = PendingIntent.getActivity(
            context,
            REQ_SHOW_APP,
            Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val canExact = canScheduleExactAlarms(context)
        if (canExact) {
            // setAlarmClock is Android's highest-reliability exact alarm:
            // wakes Doze on the exact second and permits background foreground service starts.
            val alarmClockDone = runCatching {
                val info = AlarmManager.AlarmClockInfo(triggerAtMs, showIntent)
                alarmManager.setAlarmClock(info, operationPi)
                true
            }.getOrDefault(false)

            if (!alarmClockDone) {
                runCatching {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, operationPi)
                }
            }
        } else {
            runCatching {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, operationPi)
            }
        }
    }

    private fun cancelAlarm(
        context: Context,
        alarmManager: AlarmManager,
        action: String,
        requestCode: Int
    ) {
        val pi = PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, ScheduleAlarmReceiver::class.java).apply { this.action = action },
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        runCatching { alarmManager.cancel(pi) }
        pi.cancel()
    }
}

class ScheduleAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val appCtx = context.applicationContext
        val action = intent.action ?: return

        when (action) {
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                // In-place update over an existing install: never show first-install welcome setup
                FocusOnboardingRepository.markSetupCompleted(appCtx)
                ScheduleAlarmScheduler.syncAllAlarms(appCtx)
            }

            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED -> {
                ScheduleAlarmScheduler.syncAllAlarms(appCtx)
            }

            ScheduleAlarmScheduler.ACTION_SCHEDULE_START -> {
                val pm = appCtx.getSystemService(Context.POWER_SERVICE) as? PowerManager
                val wakeLock = runCatching {
                    pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FocusLock:ScheduleTrigger")?.apply {
                        setReferenceCounted(false)
                        acquire(10_000L)
                    }
                }.getOrNull()

                try {
                    val scheduleId = intent.getStringExtra(ScheduleAlarmScheduler.EXTRA_SCHEDULE_ID)
                    val endMs = intent.getLongExtra(ScheduleAlarmScheduler.EXTRA_END_TIME_MS, 0L)
                    val nowMs = System.currentTimeMillis()

                    val schedule = scheduleId?.let { id ->
                        FocusScheduleRepository.getCurrent(appCtx).schedules.firstOrNull { it.id == id }
                    }

                    if (schedule != null && schedule.enabled && !FocusScheduleRepository.isSkippedToday(schedule, nowMs)) {
                        val safeEnd = if (endMs > nowMs + 10_000L) {
                            endMs
                        } else {
                            nowMs + schedule.durationMinutes().coerceAtLeast(1) * 60_000L
                        }
                        val window = UpcomingScheduleWindow(
                            schedule = schedule,
                            startTimeMs = nowMs,
                            endTimeMs = safeEnd,
                            isCurrentlyActive = true
                        )
                        ScheduleAlarmScheduler.startScheduledLockForWindow(appCtx, window, nowMs)
                    } else {
                        ScheduleAlarmScheduler.triggerActiveScheduleIfDue(appCtx, nowMs, scheduleId)
                    }

                    // Arm the next upcoming schedule (without re-triggering the one we just launched)
                    ScheduleAlarmScheduler.syncAllAlarms(
                        context = appCtx,
                        nowMs = nowMs + 2_000L,
                        triggerIfActiveNow = false
                    )
                } finally {
                    runCatching {
                        if (wakeLock?.isHeld == true) wakeLock.release()
                    }
                }
            }
        }
    }
}
