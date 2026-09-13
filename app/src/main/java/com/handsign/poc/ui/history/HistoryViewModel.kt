package com.handsign.poc.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.handsign.poc.data.db.entity.SessionEntity
import com.handsign.poc.data.prefs.AppPreferences
import com.handsign.poc.data.repository.SessionRepository
import com.handsign.poc.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val userRepository: UserRepository,
    private val prefs: AppPreferences
) : ViewModel() {

    val sessions: StateFlow<List<SessionEntity>> = prefs.activeUserId
        .filterNotNull()
        .flatMapLatest { userId -> sessionRepository.getSessionsByUser(userId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _deleteEvent = MutableSharedFlow<String>()
    val deleteEvent: SharedFlow<String> = _deleteEvent.asSharedFlow()

    fun deleteSession(session: SessionEntity) {
        viewModelScope.launch {
            sessionRepository.deleteSession(session.id)
            _deleteEvent.emit("Session deleted")
        }
    }

    /** Placeholder — activate Firebase to make this functional */
    fun syncToFirebase() {
        viewModelScope.launch {
            // TODO: trigger FirebaseSyncWorker once Firebase is configured
        }
    }
}
