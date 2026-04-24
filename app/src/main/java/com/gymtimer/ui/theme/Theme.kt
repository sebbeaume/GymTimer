package com.gymtimer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary        = Color(0xFFE8F44D),   // neon yellow-green — gym energy
    onPrimary      = Color(0xFF1A1A1A),
    secondary      = Color(0xFF4DBBF4),   // electric blue accent
    background     = Color(0xFF0F0F0F),
    surface        = Color(0xFF1C1C1C),
    onBackground   = Color(0xFFF0F0F0),
    onSurface      = Color(0xFFE0E0E0),
    error          = Color(0xFFFF5370),
)

@Composable
fun GymTimerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content
    )
}
