package com.example.ui.components

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/**
 * Centralized tactile haptic feedback engine for Focus Lock.
 * Maps modern Android haptic constants (API 30 / 34+) with clean fallbacks on older versions
 * so every tap, toggle, swipe detent, scrub tick, and confirmation feels deliberate and refined.
 */
class FocusHaptics(private val view: View) {

    /** Crisp micro-tap for standard buttons, keypad digits, and pills. */
    fun tap() {
        performSafe(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    /** Ultra-light detent tick for tab switches, slider steps, and chart scrubbing. */
    fun tick() {
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            HapticFeedbackConstants.SEGMENT_FREQUENT_TICK
        } else {
            HapticFeedbackConstants.CLOCK_TICK
        }
        performSafe(constant)
    }

    /** Slightly firmer step tick for switching periods, themes, or discrete options. */
    fun segment() {
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            HapticFeedbackConstants.SEGMENT_TICK
        } else {
            HapticFeedbackConstants.CLOCK_TICK
        }
        performSafe(constant)
    }

    /** Tactile switch toggle feedback distinguishing ON vs OFF on Android 14+. */
    fun toggle(enabled: Boolean) {
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (enabled) HapticFeedbackConstants.TOGGLE_ON else HapticFeedbackConstants.TOGGLE_OFF
        } else {
            HapticFeedbackConstants.CLOCK_TICK
        }
        performSafe(constant)
    }

    /** Positive confirmation (lock engaged, unlock success, link copied, refresh done). */
    fun confirm() {
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.CONFIRM
        } else {
            HapticFeedbackConstants.VIRTUAL_KEY
        }
        performSafe(constant)
    }

    /** Distinct rejection / warning pulse (wrong PIN, destructive threshold, blocked). */
    fun reject() {
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.REJECT
        } else {
            HapticFeedbackConstants.LONG_PRESS
        }
        performSafe(constant)
    }

    /** Subtle lift feedback when initiating a swipe or drag gesture. */
    fun gestureStart() {
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.GESTURE_START
        } else {
            HapticFeedbackConstants.CLOCK_TICK
        }
        performSafe(constant)
    }

    /** Subtle settle feedback when completing a swipe or snapping to an edge. */
    fun gestureEnd() {
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.GESTURE_END
        } else {
            HapticFeedbackConstants.CLOCK_TICK
        }
        performSafe(constant)
    }

    private fun performSafe(constant: Int) {
        runCatching {
            view.performHapticFeedback(constant)
        }
    }
}

@Composable
fun rememberFocusHaptics(): FocusHaptics {
    val view = LocalView.current
    return remember(view) { FocusHaptics(view) }
}

/** Convenience extensions for Views outside Compose (e.g., LockService WindowManager touch listeners). */
fun View.performFocusTap() = FocusHaptics(this).tap()
fun View.performFocusTick() = FocusHaptics(this).tick()
fun View.performFocusSegment() = FocusHaptics(this).segment()
fun View.performFocusToggle(enabled: Boolean) = FocusHaptics(this).toggle(enabled)
fun View.performFocusConfirm() = FocusHaptics(this).confirm()
fun View.performFocusReject() = FocusHaptics(this).reject()
fun View.performFocusGestureStart() = FocusHaptics(this).gestureStart()
fun View.performFocusGestureEnd() = FocusHaptics(this).gestureEnd()
