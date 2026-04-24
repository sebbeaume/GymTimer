package com.gymtimer.ui.session

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gymtimer.GymTimerApp
import com.gymtimer.data.model.SessionRecord
import com.gymtimer.service.TimerService
import kotlinx.coroutines.launch

/**
 * SessionViewModel bridges the UI with:
 *  1. TimerService.Companion StateFlows (timer ticks, session duration)
 *  2. The Room database (saving session on end)
 *
 * By reading from static StateFlows on TimerService.Companion, we avoid
 * binding to the service — simpler code, same result for our use case.
 */
class SessionViewModel(app: Application) : AndroidViewModel(app) {

    private val db = (app as GymTimerApp).database

    // Delegate directly to the StateFlows published by the service
    val timerSeconds    = TimerService.timerSeconds
    val isResting       = TimerService.isResting
    val isVibrating     = TimerService.isVibrating
    val sessionSeconds  = TimerService.sessionSeconds
    val isSessionActive = TimerService.isSessionActive

    fun toggleRest() {
        val intent = Intent(getApplication(), TimerService::class.java).apply {
            action = TimerService.ACTION_REST_TOGGLE
        }
        getApplication<Application>().startService(intent)
    }

    fun stopSession() {
        // Persist the session to Room before stopping the service
        val duration = TimerService.sessionSeconds.value
        val startTs  = TimerService.sessionStartTimestamp

        viewModelScope.launch {
            if (duration > 0) {
                db.sessionDao().insert(
                    SessionRecord(
                        startTimestamp = startTs,
                        durationSeconds = duration
                    )
                )
            }
        }

        val intent = Intent(getApplication(), TimerService::class.java).apply {
            action = TimerService.ACTION_STOP_SESSION
        }
        getApplication<Application>().startService(intent)
    }
}
