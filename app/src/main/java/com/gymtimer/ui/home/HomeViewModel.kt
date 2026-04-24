package com.gymtimer.ui.home

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gymtimer.GymTimerApp
import com.gymtimer.service.TimerService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * AndroidViewModel is a ViewModel that also holds a reference to the Application.
 * We need the Application to start a Service and access our singletons.
 *
 * ViewModel survives screen rotations — if the user rotates the phone,
 * the composable rebuilds but the ViewModel (and its state) stays alive.
 *
 * stateIn converts a cold Flow into a StateFlow that the UI can read directly.
 *   - SharingStarted.WhileSubscribed(5000): keeps the upstream alive for 5s
 *     after the last collector disappears (e.g. screen rotation), avoiding
 *     a needless restart.
 *   - initialValue: what to emit before the first DB read completes.
 */
class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = (app as GymTimerApp).userPreferences

    val restDuration = prefs.restDurationFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 90
    )

    fun saveRestDuration(seconds: Int) {
        viewModelScope.launch {
            prefs.setRestDuration(seconds)
        }
    }

    fun startSession(restSeconds: Int) {
        val intent = Intent(getApplication(), TimerService::class.java).apply {
            action = TimerService.ACTION_START_SESSION
            putExtra(TimerService.EXTRA_REST_DURATION, restSeconds)
        }
        getApplication<Application>().startForegroundService(intent)
    }
}
