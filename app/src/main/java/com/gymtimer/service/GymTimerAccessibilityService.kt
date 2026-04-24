package com.gymtimer.service

import android.accessibilityservice.AccessibilityButtonController
import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

/**
 * Uses Android's built-in accessibility button (the icon in the nav bar) to
 * trigger the rest timer start/stop from any screen.
 *
 * flagRequestAccessibilityButton tells Android to show the accessibility button
 * whenever this service is active. Tapping it calls our registered callback.
 */
class GymTimerAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        accessibilityButtonController.registerAccessibilityButtonCallback(
            object : AccessibilityButtonController.AccessibilityButtonCallback() {
                override fun onClicked(controller: AccessibilityButtonController) {
                    if (TimerService.isSessionActive.value) {
                        startService(Intent(this@GymTimerAccessibilityService, TimerService::class.java).apply {
                            action = TimerService.ACTION_VOLUME_DOUBLE
                        })
                    }
                }
                override fun onAvailabilityChanged(controller: AccessibilityButtonController, available: Boolean) {}
            }
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
}
