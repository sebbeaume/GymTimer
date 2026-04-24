package com.gymtimer.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * HomeScreen is a pure @Composable function — it describes what the UI looks
 * like for a given state, and Compose re-runs it automatically whenever
 * any State it reads changes. This is called "recomposition".
 *
 * viewModel() creates (or retrieves existing) ViewModel scoped to this
 * navigation entry. The ViewModel survives recomposition; the composable doesn't.
 *
 * collectAsState() subscribes a Flow/StateFlow and returns a Compose State<T>,
 * causing recomposition whenever a new value is emitted.
 *
 * remember { mutableStateOf(...) } creates local UI state that lives as long
 * as this composable is in the composition. Unlike ViewModel state, it is lost
 * on navigation away.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSessionStart: () -> Unit,
    onHistoryClick: () -> Unit,
    vm: HomeViewModel = viewModel()
) {
    val savedRestDuration by vm.restDuration.collectAsState()

    // Local editable state for the text field — synced to savedRestDuration initially
    var restInput by remember(savedRestDuration) { mutableStateOf(savedRestDuration.toString()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GymTimer", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onHistoryClick) {
                        Icon(Icons.Default.History, contentDescription = "History")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Text(
                text = "💪",
                fontSize = 72.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Ready to train?",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Set your default rest time, then start your session.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            // ── Rest Duration Input ───────────────────────────────────────────
            OutlinedTextField(
                value = restInput,
                onValueChange = { new ->
                    restInput = new
                    new.toIntOrNull()?.let { vm.saveRestDuration(it) }
                },
                label = { Text("Default rest time (seconds)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Quick preset chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(60, 90, 120).forEach { preset ->
                    FilterChip(
                        selected = restInput == preset.toString(),
                        onClick = {
                            restInput = preset.toString()
                            vm.saveRestDuration(preset)
                        },
                        label = { Text("${preset}s") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // ── Start Session Button ──────────────────────────────────────────
            Button(
                onClick = {
                    val rest = restInput.toIntOrNull() ?: savedRestDuration
                    vm.startSession(rest)
                    onSessionStart()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    "Start Session",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Double-press Volume Up to start/stop rest timer",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                textAlign = TextAlign.Center
            )
        }
    }
}
