package com.gymtimer.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gymtimer.GymTimerApp
import com.gymtimer.data.model.SessionRecord
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class HistoryViewModel(app: Application) : AndroidViewModel(app) {

    /**
     * getAllSessions() returns a Flow. We convert it to StateFlow with stateIn()
     * so Compose can collect it efficiently.
     * Whenever a new session is saved to Room, this Flow automatically emits
     * the updated list — the UI refreshes with zero extra code.
     */
    val sessions: StateFlow<List<SessionRecord>> =
        (app as GymTimerApp).database
            .sessionDao()
            .getAllSessions()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
}
