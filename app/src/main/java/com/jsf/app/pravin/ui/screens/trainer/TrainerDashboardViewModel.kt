package com.jsf.app.pravin.ui.screens.trainer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jsf.app.pravin.data.preferences.UserPreferences
import com.jsf.app.pravin.data.remote.TrainerAlert
import com.jsf.app.pravin.data.remote.TrainerClient
import com.jsf.app.pravin.data.remote.TrainerDataSource
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TrainerDashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = UserPreferences(application)
    private val dataSource = TrainerDataSource()

    private val _trainerId = MutableStateFlow("")
    val trainerId: StateFlow<String> = _trainerId.asStateFlow()

    val clients: StateFlow<List<TrainerClient>> = _trainerId
        .filter { it.isNotEmpty() }
        .flatMapLatest { id -> dataSource.getClients(id) }
        .map { list -> list.sortedByDescending { it.dropoutRisk } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val alerts: StateFlow<List<TrainerAlert>> = _trainerId
        .filter { it.isNotEmpty() }
        .flatMapLatest { id -> dataSource.getAlerts(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadAlertCount: StateFlow<Int> = alerts
        .map { list -> list.count { !it.isRead } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _inviteResult = MutableStateFlow<InviteResult?>(null)
    val inviteResult: StateFlow<InviteResult?> = _inviteResult.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.currentUserId.collect { uid ->
                _trainerId.value = uid
            }
        }
    }

    fun inviteClient(email: String, trainerName: String) {
        val tid = _trainerId.value
        if (tid.isEmpty() || email.isBlank()) return
        viewModelScope.launch {
            val result = dataSource.inviteClient(tid, trainerName, email.trim())
            _inviteResult.value = if (result.isSuccess) {
                InviteResult.Success(email.trim())
            } else {
                InviteResult.Error(result.exceptionOrNull()?.message ?: "Unknown error")
            }
        }
    }

    fun clearInviteResult() { _inviteResult.value = null }

    fun markAlertRead(alertId: String) {
        viewModelScope.launch { dataSource.markAlertRead(alertId) }
    }

    fun removeClient(clientId: String) {
        val tid = _trainerId.value
        if (tid.isEmpty()) return
        viewModelScope.launch { dataSource.removeClient(tid, clientId) }
    }
}

sealed class InviteResult {
    data class Success(val email: String) : InviteResult()
    data class Error(val message: String) : InviteResult()
}
