package com.example.service

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.LockService
import com.example.data.FocusSettingsRepository
import com.example.data.NotificationBlockerRepository

class FocusNotificationBlockerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn ?: return
        if (LockService.currentSessionStartTimeMs <= 0L) return

        val settings = FocusSettingsRepository.getCurrent(this)
        if (!settings.blockNotificationsDuringLock) return

        val pkg = notification.packageName ?: return
        if (pkg == packageName) return

        if (shouldAllowNotification(notification)) {
            return
        }

        val notif = notification.notification ?: return
        val extras = notif.extras
        val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val bigText = extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim().orEmpty()
        val bodyText = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        val snippet = bigText.ifBlank { bodyText }.ifBlank { "New notification" }

        val appLabel = resolveAppLabel(pkg)

        NotificationBlockerRepository.recordBlockedNotification(
            context = applicationContext,
            packageName = pkg,
            appLabel = appLabel,
            title = title.ifBlank { appLabel },
            snippet = snippet,
            timestampMs = notification.postTime.takeIf { it > 0L } ?: System.currentTimeMillis()
        )

        runCatching {
            cancelNotification(notification.key)
        }
    }

    private fun resolveAppLabel(pkg: String): String {
        return runCatching {
            val appInfo = packageManager.getApplicationInfo(pkg, 0)
            packageManager.getApplicationLabel(appInfo).toString().trim()
        }.getOrNull().orEmpty().ifBlank { pkg }
    }

    companion object {
        private val EXEMPT_CALL_PACKAGES = setOf(
            "com.android.dialer",
            "com.google.android.dialer",
            "com.android.server.telecom",
            "com.nothing.dialer",
            "com.samsung.android.dialer",
            "com.samsung.android.incallui",
            "com.android.incallui",
            "com.android.phone"
        )

        fun isCallOrExemptPackage(packageName: String): Boolean {
            val lower = packageName.lowercase()
            return lower in EXEMPT_CALL_PACKAGES ||
                lower.contains("dialer") ||
                lower.contains("telecom") ||
                lower.contains("incall")
        }

        fun shouldAllowNotification(sbn: StatusBarNotification): Boolean {
            val pkg = sbn.packageName.orEmpty()
            if (isCallOrExemptPackage(pkg)) return true

            val notif = sbn.notification ?: return false
            val category = notif.category
            if (category == Notification.CATEGORY_CALL || category == Notification.CATEGORY_ALARM) {
                return true
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val template = notif.extras?.getString(Notification.EXTRA_TEMPLATE).orEmpty()
                if (template.contains("CallStyle", ignoreCase = true) ||
                    notif.extras?.containsKey(Notification.EXTRA_CALL_PERSON) == true
                ) {
                    return true
                }
            }

            val isOngoing = (notif.flags and Notification.FLAG_ONGOING_EVENT) != 0
            if (isOngoing && notif.fullScreenIntent != null) {
                return true
            }

            return false
        }

        fun isNotificationListenerEnabled(context: Context): Boolean {
            return runCatching {
                val flat = Settings.Secure.getString(
                    context.contentResolver,
                    "enabled_notification_listeners"
                ).orEmpty()
                val expected = ComponentName(context, FocusNotificationBlockerService::class.java)
                    .flattenToString()
                flat.split(":").any { it.equals(expected, ignoreCase = true) }
            }.getOrDefault(false)
        }
    }
}
