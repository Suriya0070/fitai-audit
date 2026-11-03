package com.jsf.app.pravin.ui.screens.servers

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jsf.app.pravin.data.db.FitnessDatabase
import com.jsf.app.pravin.data.model.ServerMember
import com.jsf.app.pravin.data.model.ServerRole
import com.jsf.app.pravin.data.preferences.UserPreferences
import com.jsf.app.pravin.data.repository.CommunityRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class MembersUiState(
    val members: List<ServerMember> = emptyList(),
    val currentUserRole: String = ServerRole.MEMBER,
    val isLoading: Boolean = true
)

class ServerMembersViewModel(
    application: Application,
    private val serverId: String
) : AndroidViewModel(application) {

    private val db = FitnessDatabase.getInstance(application)
    private val communityRepo = CommunityRepository(db)
    private val prefs = UserPreferences(application)

    val uiState: StateFlow<MembersUiState> = combine(
        communityRepo.getMembersOfServer(serverId),
        prefs.currentUserId
    ) { members, currentUid ->
        val currentRole = members.find { it.userId == currentUid }?.role ?: ServerRole.MEMBER
        MembersUiState(
            members = members,
            currentUserRole = currentRole,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MembersUiState())

    fun updateMemberRole(member: ServerMember, newRole: String) {
        viewModelScope.launch {
            communityRepo.updateMemberRole(serverId, member.userId, newRole)
        }
    }

    fun removeMember(member: ServerMember) {
        viewModelScope.launch {
            communityRepo.leaveServer(serverId, member.userId)
        }
    }

    companion object {
        fun factory(application: Application, serverId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                    ServerMembersViewModel(application, serverId) as T
            }
    }
}
