package com.example.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.example.LockService
import com.example.data.DynamicIslandRepository
import com.example.data.FocusScheduleRepository

object ScheduleAlarmScheduler {
    const val ACTION_SCHEDULE_START = "com.example.action.SCHEDULE_LOCK_START"
    const val ACTION_SCHEDULE_WARNING = "com.example.action.SCHEDULE_LOCK_WARNING"
    const val EXTRA_SCHEDULE_ID = "extra_schedule_id"
    const val EXTRA_SCHEDULE_NAME = "extra_schedule_name"
    const val EXTRA_START_TIME_MS = "extra_start_time_ms"
    const val EXTRA_END_TIME_MS = "extra_end_time_ms"

    private const val REQ_NEXT_START = 4101
    private const val REQ_NEXT_WARNING = 4102

    fun syncAllAlarms(context: Context, nowMs: Long = System.currentTimeMillis()) {
        val appCtx = context.applicationContext
        val am = appCtx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        cancelAlarm(appCtx, am, ACTION_SCHEDULE_START, REQ_NEXT_START)
        cancelAlarm(appCtx, am, ACTION_SCHEDULE_WARNING, REQ_NEXT_WARNING)

        val state = FocusScheduleRepository.getCurrent(appCtx)
        val islandCfg = DynamicIslandRepository.getConfig(appCtx)
        val leadMs = islandCfg.scheduleLeadMinutes.coerceIn(1, 30) * 60_000L

        val upcomingWindows = state.schedules
            .mapNotNull { FocusScheduleRepository.computeNextOccurrence(it, nowMs) }
            .filter { !it.isCurrentlyActive }
            .sortedBy { it.startTimeMs }

        val nextWindow = upcomingWindows.firstOrNull() ?: return

        // Schedule start trigger
        val startIntent = Intent(appCtx, ScheduleAlarmReceiver::class.java).apply {
            action = ACTION_SCHEDULE_START
            putExtra(EXTRA_SCHEDULE_ID, nextWindow.schedule.id)
            putExtra(EXTRA_SCHEDULE_NAME, nextWindow.schedule.name)
            putExtra(EXTRA_START_TIME_MS, nextWindow.startTimeMs)
            putExtra(EXTRA_END_TIME_MS, nextWindow.endTimeMs)
        }
        scheduleOneShot(appCtx, am, nextWindow.startTimeMs, startIntent, REQ_NEXT_START)

        // Schedule pre-lock Dynamic Island alert
        val warningAtMs = nextWindow.startTimeMs - leadMs
        if (islandCfg.enabled && islandCfg.enableScheduleAlert && warningAtMs > nowMs + 5_000L) {
            val warnIntent = Intent(appCtx, ScheduleAlarmReceiver::class.java).apply {
                action = ACTION_SCHEDULE_WARNING
                putExtra(EXTRA_SCHEDULE_ID, nextWindow.schedule.id)
                putExtra(EXTRA_SCHEDULE_NAME, nextWindow.schedule.name)
                putExtra(EXTRA_START_TIME_MS, nextWindow.startTimeMs)
                putExtra(EXTRA_END_TIME_MS, nextWindow.endTimeMs)
            }
            scheduleOneShot(appCtx, am, warningAtMs, warnIntent, REQ_NEXT_WARNING)
        }
    }

    private fun scheduleOneShot(
        context: Context,
        alarmManager: AlarmManager,
        triggerAtMs: Long,
        intent: Intent,
        requestCode: Int
    ) {
        val pi = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        runCatching {
            val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.canScheduleExactAlarms()
            } else {
                true
            }
            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pi)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pi)
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
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                ScheduleAlarmScheduler.syncAllAlarms(appCtx)
            }

            ScheduleAlarmScheduler.ACTION_SCHEDULE_WARNING -> {
                val scheduleId = intent.getStringExtra(ScheduleAlarmScheduler.EXTRA_SCHEDULE_ID) ?: return
                val startMs = intent.getLongExtra(ScheduleAlarmScheduler.EXTRA_START_TIME_MS, 0L)
                val schedule = FocusScheduleRepository.getCurrent(appCtx).schedules
                    .firstOrNull { it.id == scheduleId } ?: return

                if (!schedule.enabled || FocusScheduleRepository.isSkippedToday(schedule)) return
                if (LockService.isRunning) return

                DynamicIslandRepository.showUpcomingSchedule(
                    context = appCtx,
                    schedule = schedule,
                    startsAtMs = startMs
                )
            }

            ScheduleAlarmScheduler.ACTION_SCHEDULE_START -> {
                val scheduleId = intent.getStringExtra(ScheduleAlarmScheduler.EXTRA_SCHEDULE_ID) ?: return
                val endMs = intent.getLongExtra(ScheduleAlarmScheduler.EXTRA_END_TIME_MS, 0L)
                val schedule = FocusScheduleRepository.getCurrent(appCtx).schedules
                    .firstOrNull { it.id == scheduleId }

                if (schedule == null || !schedule.enabled || FocusScheduleRepository.isSkippedToday(schedule)) {
                    ScheduleAlarmScheduler.syncAllAlarms(appCtx)
                    return
                }

                // Conflict rule: If a manual OTP focus session is already running when the
                // scheduled lock time arrives, automatically cancel/skip this schedule for today.
                if (LockService.isRunning && !LockService.isScheduledSession) {
                    FocusScheduleRepository.markSkippedForToday(appCtx, schedule.id)
                    DynamicIslandRepository.dismissActive(appCtx)
                    return
                }

                if (LockService.isRunning) {
                    ScheduleAlarmScheduler.syncAllAlarms(appCtx)
                    return
                }

                val nowMs = System.currentTimeMillis()
                val safeEndMs = if (endMs > nowMs + 10_000L) {
                    endMs
                } else {
                    nowMs + schedule.durationMinutes().coerceAtLeast(1) * 60_000L
                }

                if (runCatching { Settings.canDrawOverlays(appCtx) }.getOrDefault(false)) {
                    DynamicIslandRepository.dismissActive(appCtx)
                    val serviceIntent = Intent(appCtx, LockService::class.java).apply {
                        putExtra(LockService.EXTRA_IS_SCHEDULED, true)
                        putExtra(LockService.EXTRA_SCHEDULE_ID, schedule.id)
                        putExtra(LockService.EXTRA_SCHEDULE_NAME, schedule.name)
                        putExtra(LockService.EXTRA_SCHEDULE_TIME_RANGE, schedule.formattedTimeRange())
                        putExtra(LockService.EXTRA_SCHEDULE_END_TIME_MS, safeEndMs)
                    }
                    runCatching {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            appCtx.startForegroundService(serviceIntent)
                        } else {
                            appCtx.startService(serviceIntent)
                        }
                    }
                }
                ScheduleAlarmScheduler.syncAllAlarms(appCtx, nowMs + 1_500L)
            }
        }
    }
}
