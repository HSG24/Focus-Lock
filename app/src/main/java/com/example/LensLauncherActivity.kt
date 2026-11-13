package com.example

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast

class LensLauncherActivity : Activity() {
    private var hasPaused = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Minimize the lock screen overlay immediately
        startService(Intent(this, LockService::class.java).apply { action = "MINIMIZE" })
        
        hasPaused = false
        launchLens()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        startService(Intent(this, LockService::class.java).apply { action = "MINIMIZE" })
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
        // 1. Try launching dedicated Lens package if installed
        for (pkg in lensPackages) {
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
        
        // 2. Fallback to Google App
        if (!launched) {
            try {
                val searchIntent = Intent(Intent.ACTION_WEB_SEARCH)
                searchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                startActivity(searchIntent)
                launched = true
            } catch (e: Exception) {
                // Ignore fallback error
            }
        }
        
        if (!launched) {
            Toast.makeText(this, "Google App/Lens not found", Toast.LENGTH_SHORT).show()
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
            // Returned from Google Lens/App! Maximize lock screen and finish
            startService(Intent(this, LockService::class.java).apply { action = "MAXIMIZE" })
            finish()
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }
}
