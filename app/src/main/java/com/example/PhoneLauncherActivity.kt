package com.example

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast

class PhoneLauncherActivity : Activity() {
    private var hasPaused = false
    private var launchStartedAtMs = 0L

    private fun sendLockAction(actionName: String) {
        if (!LockService.isRunning) return
        runCatching {
            startService(Intent(this, LockService::class.java).apply { action = actionName })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sendLockAction("MINIMIZE")
        hasPaused = false
        launchPhone()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        sendLockAction("MINIMIZE")
        hasPaused = false
        launchPhone()
    }

    private fun launchPhone() {
        var launched = false

        try {
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            }
            if (dialIntent.resolveActivity(packageManager) != null) {
                launchStartedAtMs = System.currentTimeMillis()
                startActivity(dialIntent)
                launched = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (!launched) {
            try {
                val callButtonIntent = Intent(Intent.ACTION_CALL_BUTTON).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                }
                if (callButtonIntent.resolveActivity(packageManager) != null) {
                    launchStartedAtMs = System.currentTimeMillis()
                    startActivity(callButtonIntent)
                    launched = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

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
                        launchStartedAtMs = System.currentTimeMillis()
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
            sendLockAction("MAXIMIZE")
            finish()
        }
    }

    override fun onPause() {
        super.onPause()
        hasPaused = true
    }

    override fun onResume() {
        super.onResume()
        if (hasPaused && System.currentTimeMillis() - launchStartedAtMs > 450L) {
            sendLockAction("MAXIMIZE")
            finish()
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }
}
