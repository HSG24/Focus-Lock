package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.LockService
import com.example.data.FocusSettingsRepository

class FocusPowerBlockerAccessibilityService : AccessibilityService() {

    @Volatile
    private var lastBlockedTimestampMs: Long = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (LockService.currentSessionStartTimeMs <= 0L) return

        val settings = FocusSettingsRepository.getCurrent(this)
        if (!settings.blockPowerMenuDuringLock) return

        val eventType = event.eventType
        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) {
            return
        }

        val pkg = event.packageName?.toString().orEmpty()
        if (pkg != "com.android.systemui" && pkg != "android") {
            return
        }

        val className = event.className?.toString().orEmpty()
        val classMatchesPowerMenu = POWER_CLASS_KEYWORDS.any { keyword ->
            className.contains(keyword, ignoreCase = true)
        }

        val eventTextMatches = event.text.any { cs ->
            matchesPowerLabel(cs?.toString())
        }

        val nodeMatches = if (classMatchesPowerMenu || eventTextMatches) {
            true
        } else {
            val root = runCatching { rootInActiveWindow }.getOrNull()
            try {
                root != null && containsPowerMenuNode(root, depth = 0)
            } finally {
                @Suppress("DEPRECATION")
                runCatching { root?.recycle() }
            }
        }

        if (nodeMatches) {
            val now = System.currentTimeMillis()
            if (now - lastBlockedTimestampMs < 250L) return
            lastBlockedTimestampMs = now

            performGlobalAction(GLOBAL_ACTION_BACK)
            performGlobalAction(GLOBAL_ACTION_HOME)

            runCatching {
                val maximizeIntent = Intent(this, LockService::class.java).apply {
                    action = "MAXIMIZE"
                }
                startService(maximizeIntent)
            }
        }
    }

    override fun onInterrupt() {
        // No-op
    }

    private fun containsPowerMenuNode(node: AccessibilityNodeInfo, depth: Int): Boolean {
        if (depth > 8) return false

        val text = node.text?.toString()
        val contentDesc = node.contentDescription?.toString()
        if (matchesPowerLabel(text) || matchesPowerLabel(contentDesc)) {
            return true
        }

        val childCount = node.childCount.coerceAtMost(24)
        for (i in 0 until childCount) {
            val child = node.getChild(i) ?: continue
            val matched = try {
                containsPowerMenuNode(child, depth + 1)
            } finally {
                @Suppress("DEPRECATION")
                runCatching { child.recycle() }
            }
            if (matched) return true
        }
        return false
    }

    companion object {
        private val POWER_CLASS_KEYWORDS = listOf(
            "GlobalActionsDialog",
            "ActionsDialog",
            "GlobalActions",
            "Shutdown",
            "PowerMenu"
        )

        private val POWER_NODE_LABELS = listOf(
            "Power off",
            "Restart",
            "Reboot",
            "Emergency"
        )

        fun matchesPowerLabel(raw: String?): Boolean {
            if (raw.isNullOrBlank()) return false
            return POWER_NODE_LABELS.any { label ->
                raw.contains(label, ignoreCase = true)
            }
        }

        fun isPowerBlockerAccessibilityEnabled(context: Context): Boolean {
            return runCatching {
                val enabledServices = Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                ).orEmpty()
                val expected = ComponentName(context, FocusPowerBlockerAccessibilityService::class.java)
                    .flattenToString()
                val shortExpected = ComponentName(context, FocusPowerBlockerAccessibilityService::class.java)
                    .flattenToShortString()
                enabledServices.split(":").any { entry ->
                    entry.equals(expected, ignoreCase = true) ||
                        entry.equals(shortExpected, ignoreCase = true)
                }
            }.getOrDefault(false)
        }
    }
}
