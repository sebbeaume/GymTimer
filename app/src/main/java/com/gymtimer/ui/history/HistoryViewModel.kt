package com.gymtimer.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gymtimer.GymTimerApp
import com.gymtimer.data.model.SessionRecord
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = (app as GymTimerApp).database.sessionDao()

    val sessions: StateFlow<List<SessionRecord>> =
        dao.getAllSessions()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    fun deleteSession(session: SessionRecord) {
        viewModelScope.launch { dao.delete(session) }
    }
}
