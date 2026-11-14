package com.example.widget

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import com.example.LockService
import com.example.MainActivity
import com.example.uploadPinToFirebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.random.Random

/**
 * Translucent launcher-trampoline activity invoked by tapping "TAP TO LOCK" on the 2x2 widget.
 * Generates an OTP PIN, syncs it to Firebase RTDB, and starts LockService cleanly in foreground
 * without hitting Android 12+ background foreground-service restrictions.
 */
class QuickLockActivity : ComponentActivity() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (LockService.currentSessionStartTimeMs > 0L) {
            val maximizeIntent = Intent(this, LockService::class.java).apply {
                action = "MAXIMIZE"
            }
            runCatching { startService(maximizeIntent) }
            finish()
            return
        }

        if (!Settings.canDrawOverlays(this) || !hasUsageStatsPermission(this)) {
            Toast.makeText(this, "Grant overlay & usage permissions to quick lock", Toast.LENGTH_SHORT).show()
            startActivity(
                Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
            )
            finish()
            return
        }

        val pin = String.format(Locale.US, "%06d", Random.nextInt(1_000_000))
        scope.launch {
            val uploaded = withContext(Dispatchers.IO) {
                uploadPinToFirebase(pin)
            }
            if (uploaded) {
                val serviceIntent = Intent(this@QuickLockActivity, LockService::class.java).apply {
                    putExtra("PIN", pin)
                }
                ContextCompat.startForegroundService(this@QuickLockActivity, serviceIntent)
                withContext(Dispatchers.IO) {
                    NothingFocusWidgetUpdater.updateAllWidgets(applicationContext)
                }
            } else {
                Toast.makeText(
                    this@QuickLockActivity,
                    "Could not sync unlock PIN. Check your connection.",
                    Toast.LENGTH_SHORT
                ).show()
                startActivity(
                    Intent(this@QuickLockActivity, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    }
                )
            }
            finish()
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }
}
