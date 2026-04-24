package com.gymtimer.ui.session

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * animateColorAsState and animateFloatAsState are Compose animation APIs.
 * They smoothly interpolate between two values whenever the target changes,
 * without any manual animation code. Compose handles the frame-by-frame updates.
 *
 * LaunchedEffect(key) runs a coroutine side-effect when the key changes.
 * Here we use it to navigate back automatically once the service reports
 * the session is no longer active (i.e. after stopSession() is called).
 * LaunchedEffect is cancelled and re-launched whenever the key changes.
 */
@Composable
fun SessionScreen(
    onSessionEnd: () -> Unit,
    vm: SessionViewModel = viewModel()
) {
    val timerSeconds   by vm.timerSeconds.collectAsState()
    val isResting      by vm.isResting.collectAsState()
    val isVibrating    by vm.isVibrating.collectAsState()
    val sessionSeconds by vm.sessionSeconds.collectAsState()
    val isActive       by vm.isSessionActive.collectAsState()

    // Block the system back button — use "End Session" to exit
    BackHandler {}

    // Navigate home only after the session was active and then stopped
    var sessionStarted by remember { mutableStateOf(false) }
    LaunchedEffect(isActive) {
        if (isActive) sessionStarted = true
        if (sessionStarted && !isActive) onSessionEnd()
    }

    // Background pulses to a dark red tint while resting
    val bgColor by animateColorAsState(
        targetValue = if (isResting)
            Color(0xFF1A0A0A)
        else
            MaterialTheme.colorScheme.background,
        animationSpec = tween(600),
        label = "bg"
    )

    val timerScale by animateFloatAsState(
        targetValue = if (isResting) 1.05f else 1f,
        animationSpec = tween(300),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {

            // ── Session duration ──────────────────────────────────────────────
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Session",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
                Text(
                    text = formatTime(sessionSeconds),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Divider(
                modifier = Modifier.width(80.dp),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f)
            )

            // ── Rest timer ring ───────────────────────────────────────────────
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.scale(timerScale)
            ) {
                // Outer glow ring when resting
                if (isResting) {
                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                shape = CircleShape
                            )
                    )
                }
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .background(
                            color = when {
                                isVibrating -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                                isResting   -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else        -> MaterialTheme.colorScheme.surface
                            },
                            shape = CircleShape
                        )
                        .clickable { vm.toggleRest() },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = when {
                                isVibrating -> "DISMISS"
                                isResting   -> "REST"
                                else        -> "START REST"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = when {
                                isVibrating -> MaterialTheme.colorScheme.error
                                isResting   -> MaterialTheme.colorScheme.primary
                                else        -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            },
                            letterSpacing = 3.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = when {
                                isVibrating -> "DONE"
                                isResting   -> formatTime(timerSeconds.toLong())
                                else        -> "--:--"
                            },
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isVibrating -> MaterialTheme.colorScheme.error
                                isResting   -> MaterialTheme.colorScheme.primary
                                else        -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            }
                        )
                    }
                }
            }

            // ── Hint text ─────────────────────────────────────────────────────
            Text(
                text = when {
                    isVibrating -> "Tap the circle or notification to dismiss"
                    isResting   -> "Tap the circle or notification to cancel rest"
                    else        -> "Tap the circle or notification after a set"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── End Session ───────────────────────────────────────────────────
            OutlinedButton(
                onClick = { vm.stopSession() },
                modifier = Modifier.fillMaxWidth(0.6f)
            ) {
                Text("End Session", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

private fun formatTime(totalSeconds: Long): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "%02d:%02d".format(m, s)
}
