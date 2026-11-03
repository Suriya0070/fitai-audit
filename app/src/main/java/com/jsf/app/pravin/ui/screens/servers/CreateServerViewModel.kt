package com.jsf.app.pravin.ui.screens.servers

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jsf.app.pravin.data.db.FitnessDatabase
import com.jsf.app.pravin.data.model.Channel
import com.jsf.app.pravin.data.model.Server
import com.jsf.app.pravin.data.model.ServerMember
import com.jsf.app.pravin.data.preferences.UserPreferences
import com.jsf.app.pravin.data.repository.CommunityRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class CreateServerUiState(
    val isLoading: Boolean = false,
    val createdServerId: String? = null,
    val error: String? = null
)

class CreateServerViewModel(application: Application) : AndroidViewModel(application) {

    private val db = FitnessDatabase.getInstance(application)
    private val communityRepository = CommunityRepository(db)
    private val prefs = UserPreferences(application)

    private val _uiState = MutableStateFlow(CreateServerUiState())
    val uiState: StateFlow<CreateServerUiState> = _uiState.asStateFlow()

    fun createServer(
        name: String,
        description: String,
        isPublic: Boolean,
        accentColor: String
    ) {
        if (name.isBlank()) {
            _uiState.update { it.copy(error = "Server name is required") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val userId = prefs.currentUserId.first()
                val serverId = UUID.randomUUID().toString()

                communityRepository.saveServer(
                    Server(
                        id = serverId,
                        name = name.trim(),
                        description = description.trim(),
                        ownerId = userId,
                        isPublic = isPublic,
                        accentColor = accentColor,
                        memberCount = 1
                    )
                )

                communityRepository.joinServer(
                    ServerMember(
                        serverId = serverId,
                        userId = userId,
                        role = ServerMember.Role.OWNER
                    )
                )

                val defaultChannels = listOf(
                    Channel(UUID.randomUUID().toString(), serverId, "general", Channel.ChannelType.TEXT, position = 0),
                    Channel(UUID.randomUUID().toString(), serverId, "workouts", Channel.ChannelType.WORKOUT, position = 1),
                    Channel(UUID.randomUUID().toString(), serverId, "announcements", Channel.ChannelType.ANNOUNCEMENTS, position = 2),
                    Channel(UUID.randomUUID().toString(), serverId, "nutrition", Channel.ChannelType.NUTRITION, position = 3),
                    Channel(UUID.randomUUID().toString(), serverId, "progress", Channel.ChannelType.PROGRESS, position = 4)
                )
                defaultChannels.forEach { communityRepository.saveChannel(it) }

                communityRepository.saveDefaultRoles(serverId)

                _uiState.update { it.copy(isLoading = false, createdServerId = serverId) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Failed to create server: ${e.message}") }
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
