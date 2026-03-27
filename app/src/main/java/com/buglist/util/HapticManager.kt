package com.buglist.util

import android.content.Context
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * BugList v2.0.0 Haptic Feedback System.
 *
 * Centralizes all haptic feedback calls so every interaction feels deliberate.
 * Uses [HapticFeedbackConstants] — no custom vibration patterns.
 *
 * Usage: Call the appropriate method from a Composable via [View.performHapticFeedback],
 * or pass this manager to a composable that owns a [View] reference.
 */
@Singleton
class HapticManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Haptic for numpad key taps (KEYBOARD_TAP).
     * Light, crisp response for every digit / comma / backspace press.
     */
    fun onKeyboardTap(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    /**
     * Haptic for successful actions: adding a debt, completing a payment.
     * CONFIRM feedback (API 30+), falls back to VIRTUAL_KEY on older APIs.
     */
    fun onConfirm(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }

    /**
     * Haptic for destructive / rejection actions: delete, cancel debt.
     * REJECT feedback (API 30+), falls back to LONG_PRESS on older APIs.
     */
    fun onReject(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }

    /**
     * Haptic when a swipe threshold is reached: reveals action button.
     * CLOCK_TICK — light tick to confirm the action is available.
     */
    fun onSwipeThreshold(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    /**
     * Haptic for long-press: context menu trigger, drag handle activation.
     * LONG_PRESS for reliable feedback across all API levels.
     */
    fun onLongPress(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }
}
