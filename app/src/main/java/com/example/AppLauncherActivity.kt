package com.example

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.Toast

class AppLauncherActivity : Activity() {
    companion object {
        const val EXTRA_PACKAGE_NAME = "EXTRA_PACKAGE_NAME"
        const val EXTRA_APP_LABEL = "EXTRA_APP_LABEL"
    }

    private var hasPaused = false
    private var launchStartedAtMs = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startService(Intent(this, LockService::class.java).apply { action = "MINIMIZE" })
        hasPaused = false
        launchTarget(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        startService(Intent(this, LockService::class.java).apply { action = "MINIMIZE" })
        hasPaused = false
        launchTarget(intent)
    }

    private fun launchTarget(intent: Intent?) {
        val packageName = intent?.getStringExtra(EXTRA_PACKAGE_NAME).orEmpty()
        val label = intent?.getStringExtra(EXTRA_APP_LABEL).orEmpty().ifBlank { "App" }

        var launched = false
        if (packageName.isNotBlank()) {
            val launchIntent = resolveAppLaunchIntent(packageName)
            if (launchIntent != null) {
                try {
                    launchIntent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                    )
                    launchStartedAtMs = System.currentTimeMillis()
                    startActivity(launchIntent)
                    launched = true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        if (!launched) {
            Toast.makeText(this, "$label could not be launched", Toast.LENGTH_SHORT).show()
            startService(Intent(this, LockService::class.java).apply { action = "MAXIMIZE" })
            finish()
        }
    }

    private fun resolveAppLaunchIntent(packageName: String): Intent? {
        packageManager.getLaunchIntentForPackage(packageName)?.let { return it }

        return try {
            val mainIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                setPackage(packageName)
            }
            val resolved = packageManager.queryIntentActivities(mainIntent, 0).firstOrNull()
            val activityInfo = resolved?.activityInfo
            if (activityInfo != null) {
                Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    component = ComponentName(activityInfo.packageName, activityInfo.name)
                }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    override fun onPause() {
        super.onPause()
        hasPaused = true
    }

    override fun onResume() {
        super.onResume()
        if (hasPaused && System.currentTimeMillis() - launchStartedAtMs > 500L) {
            startService(Intent(this, LockService::class.java).apply { action = "MAXIMIZE" })
            finish()
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }
}
