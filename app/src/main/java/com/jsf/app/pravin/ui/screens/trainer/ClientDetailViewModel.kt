package com.jsf.app.pravin.ui.screens.trainer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.jsf.app.pravin.data.preferences.UserPreferences
import com.jsf.app.pravin.data.remote.ClientStatusSnapshot
import com.jsf.app.pravin.data.remote.TrainerAlert
import com.jsf.app.pravin.data.remote.TrainerDataSource
import com.jsf.app.pravin.data.remote.TrainerNote
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ClientDetailViewModel(
    application: Application,
    val clientId: String
) : AndroidViewModel(application) {

    private val prefs = UserPreferences(application)
    private val dataSource = TrainerDataSource()

    private val _trainerId = MutableStateFlow("")

    val statusSnapshot: StateFlow<ClientStatusSnapshot?> = _trainerId
        .filter { it.isNotEmpty() }
        .flatMapLatest { tid -> dataSource.getStatusSnapshot(tid, clientId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val alerts: StateFlow<List<TrainerAlert>> = _trainerId
        .filter { it.isNotEmpty() }
        .flatMapLatest { tid -> dataSource.getAlertsForClient(tid, clientId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notes: StateFlow<List<TrainerNote>> = _trainerId
        .filter { it.isNotEmpty() }
        .flatMapLatest { tid -> dataSource.getNotesForClient(tid, clientId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val workoutSessions: StateFlow<List<Map<String, Any>>> = _trainerId
        .filter { it.isNotEmpty() }
        .flatMapLatest { dataSource.getClientWorkoutSessions(clientId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _noteSaved = MutableStateFlow(false)
    val noteSaved: StateFlow<Boolean> = _noteSaved.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.currentUserId.collect { uid ->
                _trainerId.value = uid
            }
        }
    }

    fun saveNote(content: String) {
        val tid = _trainerId.value
        if (tid.isEmpty() || content.isBlank()) return
        viewModelScope.launch {
            dataSource.saveNote(tid, clientId, content)
            _noteSaved.value = true
        }
    }

    fun clearNoteSaved() { _noteSaved.value = false }

    fun markAlertRead(alertId: String) {
        viewModelScope.launch { dataSource.markAlertRead(alertId) }
    }

    class Factory(private val app: Application, private val clientId: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            return ClientDetailViewModel(app, clientId) as T
        }
    }
}
