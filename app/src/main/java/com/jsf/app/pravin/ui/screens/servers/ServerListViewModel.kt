package com.jsf.app.pravin.ui.screens.servers

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jsf.app.pravin.data.db.FitnessDatabase
import com.jsf.app.pravin.data.model.Server
import com.jsf.app.pravin.data.model.ServerMember
import com.jsf.app.pravin.data.preferences.UserPreferences
import com.jsf.app.pravin.data.repository.CommunityRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ServerListViewModel(application: Application) : AndroidViewModel(application) {

    private val db = FitnessDatabase.getInstance(application)
    private val communityRepository = CommunityRepository(db)
    private val prefs = UserPreferences(application)

    private val _currentUserId = MutableStateFlow("")
    val currentUserId: StateFlow<String> = _currentUserId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val myServers: StateFlow<List<Server>> = _currentUserId
        .flatMapLatest { userId ->
            if (userId.isEmpty()) flowOf(emptyList())
            else {
                communityRepository.getServersForUser(userId)
                    .flatMapLatest { memberships ->
                        if (memberships.isEmpty()) {
                            flowOf(emptyList())
                        } else {
                            val serverFlows = memberships.map { m ->
                                communityRepository.getServerById(m.serverId)
                            }
                            combine(serverFlows) { arr -> arr.filterNotNull().toList() }
                        }
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val discoverServers: StateFlow<List<Server>> = _searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isBlank()) communityRepository.publicServers
            else communityRepository.searchServers(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            prefs.currentUserId.collect { uid ->
                _currentUserId.value = uid
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun joinServer(serverId: String) {
        viewModelScope.launch {
            val userId = _currentUserId.value
            if (userId.isEmpty()) return@launch
            communityRepository.joinServer(
                ServerMember(
                    serverId = serverId,
                    userId = userId,
                    role = ServerMember.Role.MEMBER
                )
            )
        }
    }
}
