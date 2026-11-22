package com.example

import android.app.Application
import com.example.data.FocusStatsRepository
import com.example.service.ScheduleAlarmScheduler

class FocusLockApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FocusStatsRepository.initialize(this)
        ScheduleAlarmScheduler.syncAllAlarms(this)
    }
}
