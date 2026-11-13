package com.example

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast

class PhoneLauncherActivity : Activity() {
    private var hasPaused = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Minimize the lock screen overlay immediately
        startService(Intent(this, LockService::class.java).apply { action = "MINIMIZE" })
        
        hasPaused = false
        launchPhone()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        startService(Intent(this, LockService::class.java).apply { action = "MINIMIZE" })
        hasPaused = false
        launchPhone()
    }

    private fun launchPhone() {
        var launched = false
        
        // 1. Try launching dialer via ACTION_DIAL
        try {
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            }
            if (dialIntent.resolveActivity(packageManager) != null) {
                startActivity(dialIntent)
                launched = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Fallback to ACTION_CALL_BUTTON
        if (!launched) {
            try {
                val callButtonIntent = Intent(Intent.ACTION_CALL_BUTTON).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                }
                if (callButtonIntent.resolveActivity(packageManager) != null) {
                    startActivity(callButtonIntent)
                    launched = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 3. Fallback to specific phone dialer packages
        if (!launched) {
            val dialerPackages = listOf(
                "com.google.android.dialer",
                "com.samsung.android.dialer",
                "com.android.dialer",
                "com.android.phone",
                "com.android.contacts"
            )
            for (pkg in dialerPackages) {
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
        }

        if (!launched) {
            Toast.makeText(this, "Phone app not found", Toast.LENGTH_SHORT).show()
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
            // Returned from Phone app! Maximize lock screen and finish
            startService(Intent(this, LockService::class.java).apply { action = "MAXIMIZE" })
            finish()
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }
}
