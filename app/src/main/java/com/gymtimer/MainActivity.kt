package com.gymtimer

import android.content.Intent
import android.os.Bundle
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

}
