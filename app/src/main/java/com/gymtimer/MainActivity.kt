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
 * Rest timer control is handled by GymTimerAccessibilityService via the
 * system accessibility button — see that class for details.
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
