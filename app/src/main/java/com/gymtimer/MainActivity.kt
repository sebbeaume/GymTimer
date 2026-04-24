package com.gymtimer

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gymtimer.service.TimerService
import com.gymtimer.ui.history.HistoryScreen
import com.gymtimer.ui.home.HomeScreen
import com.gymtimer.ui.session.SessionScreen
import com.gymtimer.ui.theme.GymTimerTheme

/**
 * ComponentActivity is the Jetpack Compose-friendly base class.
 * It gives us setContent{} for Compose and handles modern lifecycle events.
 *
 * We override dispatchKeyEvent to intercept hardware key presses BEFORE
 * they reach any composable — this is how we detect the volume-up double press
 * even when the app is visible.
 *
 * When the screen is OFF or the app is in the background, the service itself
 * needs to handle the button — but Android restricts background key interception
 * since API 29. The pattern here works when the screen is on (app visible or
 * recent apps). For fully locked-screen detection, users would need an
 * Accessibility Service (which requires explicit user permission in Settings).
 */
class MainActivity : ComponentActivity() {

    // Volume double-press detection: track last press timestamp
    private var lastVolumeUpPress = 0L
    private val DOUBLE_PRESS_THRESHOLD = 500L // ms

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GymTimerTheme {
                /**
                 * rememberNavController creates a NavController that survives
                 * recomposition. NavHost declares all routes; each composable{}
                 * block is one screen.
                 */
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        HomeScreen(
                            onSessionStart = { navController.navigate("session") },
                            onHistoryClick = { navController.navigate("history") }
                        )
                    }
                    composable("session") {
                        SessionScreen(
                            onSessionEnd = { navController.navigate("home") {
                                popUpTo("home") { inclusive = true }
                            }}
                        )
                    }
                    composable("history") {
                        HistoryScreen(onBack = { navController.popBackStack() })
                    }
                }
            }
        }
    }

    /**
     * dispatchKeyEvent fires for ALL key events before they reach any view.
     * We check for VOLUME_UP and measure time between presses to detect a double-tap.
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP
            && event.action == KeyEvent.ACTION_DOWN) {

            val now = System.currentTimeMillis()
            if (now - lastVolumeUpPress < DOUBLE_PRESS_THRESHOLD) {
                // Double press detected — forward to service
                sendCommandToService(TimerService.ACTION_VOLUME_DOUBLE)
                lastVolumeUpPress = 0L // reset so triple press doesn't re-trigger
                return true // consume the event (don't change system volume)
            }
            lastVolumeUpPress = now
            // Don't consume on first press — let the system handle volume change
        }
        return super.dispatchKeyEvent(event)
    }

    private fun sendCommandToService(action: String) {
        val intent = Intent(this, TimerService::class.java).apply {
            this.action = action
        }
        startService(intent)
    }
}
