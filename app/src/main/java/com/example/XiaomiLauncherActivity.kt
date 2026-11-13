package com.example

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast

class XiaomiLauncherActivity : Activity() {
    private var hasPaused = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Minimize the lock screen overlay immediately
        startService(Intent(this, LockService::class.java).apply { action = "MINIMIZE" })
        
        hasPaused = false
        launchXiaomiHome()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        startService(Intent(this, LockService::class.java).apply { action = "MINIMIZE" })
        hasPaused = false
        launchXiaomiHome()
    }

    private fun launchXiaomiHome() {
        val xiaomiPackages = listOf(
            "com.xiaomi.smarthome",
            "com.xiaomi.mihome"
        )
        
        var launched = false
        for (pkg in xiaomiPackages) {
            try {
                val intent = packageManager.getLaunchIntentForPackage(pkg)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                    startActivity(intent)
                    launched = true
                    break
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (!launched) {
            Toast.makeText(this, "Xiaomi Home app not found", Toast.LENGTH_SHORT).show()
            // If failed, restore the lock screen right away
            startService(Intent(this, LockService::class.java).apply { action = "MAXIMIZE" })
            finish()
        }
    }

    override fun onPause() {
        super.onPause()
        hasPaused = true
    }

    override fun onResume() {
        super.onResume()
        if (hasPaused) {
            // Returned from Xiaomi Home app! Maximize lock screen and finish
            startService(Intent(this, LockService::class.java).apply { action = "MAXIMIZE" })
            finish()
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }
}
