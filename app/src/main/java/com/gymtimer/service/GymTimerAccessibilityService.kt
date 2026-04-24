package com.gymtimer.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

/**
 * Intercepts hardware key events globally — even when GymTimer is in the background.
 * This is the only Android API that allows intercepting volume keys from another app's foreground.
 *
 * The user must enable this service once via Settings → Accessibility → GymTimer.
 *
 * onKeyEvent returning true consumes the event (volume does not change).
 * Returning false passes it through to the system (volume changes normally).
 * We only consume on a confirmed double-press.
 */
class GymTimerAccessibilityService : AccessibilityService() {

    private var lastVolumeUpPress = 0L
    private val DOUBLE_PRESS_THRESHOLD = 500L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (!TimerService.isSessionActive.value) return false

        if (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP
            && event.action == KeyEvent.ACTION_DOWN) {

            val now = System.currentTimeMillis()
            if (now - lastVolumeUpPress < DOUBLE_PRESS_THRESHOLD) {
                startService(Intent(this, TimerService::class.java).apply {
                    action = TimerService.ACTION_VOLUME_DOUBLE
                })
                lastVolumeUpPress = 0L
                return true // consume — don't change volume on double-press
            }
            lastVolumeUpPress = now
        }
        return false
    }
}
