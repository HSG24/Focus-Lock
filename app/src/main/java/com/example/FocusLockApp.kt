package com.example

import android.app.Application
import com.example.data.FocusStatsRepository

class FocusLockApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FocusStatsRepository.initialize(this)
    }
}
