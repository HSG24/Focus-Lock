package com.example

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast

class LensLauncherActivity : Activity() {
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
        launchLens()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        sendLockAction("MINIMIZE")
        hasPaused = false
        launchLens()
    }

    private fun launchLens() {
        val lensPackages = listOf(
            "com.google.android.googlequicksearchbox",
            "com.google.ar.lens",
            "com.google.lens"
        )

        var launched = false
        for (pkg in lensPackages) {
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

        if (!launched) {
            try {
                val searchIntent = Intent(Intent.ACTION_WEB_SEARCH)
                searchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                launchStartedAtMs = System.currentTimeMillis()
                startActivity(searchIntent)
                launched = true
            } catch (_: Exception) {
            }
        }

        if (!launched) {
            Toast.makeText(this, "Google App/Lens not found", Toast.LENGTH_SHORT).show()
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
