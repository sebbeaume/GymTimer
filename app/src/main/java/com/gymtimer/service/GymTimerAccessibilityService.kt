package com.gymtimer.service

import android.accessibilityservice.AccessibilityButtonController
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class GymTimerAccessibilityService : AccessibilityService() {

    // Manual coroutine scope — AccessibilityService has no built-in lifecycleScope
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onServiceConnected() {
        super.onServiceConnected()

        // Register the tap callback
        accessibilityButtonController.registerAccessibilityButtonCallback(
            object : AccessibilityButtonController.AccessibilityButtonCallback() {
                override fun onClicked(controller: AccessibilityButtonController) {
                    startService(Intent(this@GymTimerAccessibilityService, TimerService::class.java).apply {
                        action = TimerService.ACTION_REST_TOGGLE
                    })
                }
                override fun onAvailabilityChanged(controller: AccessibilityButtonController, available: Boolean) {}
            }
        )

        // Observe session state and show/hide the button accordingly
        scope.launch {
            TimerService.isSessionActive.collect { active ->
                setButtonVisible(active)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private fun setButtonVisible(visible: Boolean) {
        val info = serviceInfo ?: return
        info.flags = if (visible) {
            info.flags or AccessibilityServiceInfo.FLAG_REQUEST_ACCESSIBILITY_BUTTON
        } else {
            info.flags and AccessibilityServiceInfo.FLAG_REQUEST_ACCESSIBILITY_BUTTON.inv()
        }
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
}
