package com.jsf.app.pravin.ui.screens.community

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jsf.app.pravin.data.db.FitnessDatabase
import com.jsf.app.pravin.data.model.Channel
import com.jsf.app.pravin.data.model.Message
import com.jsf.app.pravin.data.preferences.UserPreferences
import com.jsf.app.pravin.data.repository.CommunityRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class ChannelChatUiState(
    val channel: Channel? = null,
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = true,
    val currentUserId: String = "",
    val currentUserName: String = ""
)

class ChannelChatViewModel(
    application: Application,
    private val channelId: String
) : AndroidViewModel(application) {

    private val db = FitnessDatabase.getInstance(application)
    private val communityRepo = CommunityRepository(db)
    private val prefs = UserPreferences(application)

    private val _uiState = MutableStateFlow(ChannelChatUiState())
    val uiState: StateFlow<ChannelChatUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val uid = prefs.currentUserId.first()
            _uiState.update { it.copy(currentUserId = uid) }
            db.userProfileDao().getProfileByUid(uid).first()?.let { profile ->
                _uiState.update { it.copy(currentUserName = profile.name.ifBlank { uid.take(8) }) }
            }
        }

        viewModelScope.launch {
            communityRepo.getChannelById(channelId).collect { channel ->
                _uiState.update { it.copy(channel = channel) }
            }
        }

        viewModelScope.launch {
            communityRepo.getMessagesForChannel(channelId).collect { messages ->
                _uiState.update { it.copy(messages = messages, isLoading = false) }
            }
        }
    }

    fun sendMessage(content: String) {
        if (content.isBlank()) return
        val state = _uiState.value
        val channel = state.channel ?: return
        viewModelScope.launch {
            val message = Message(
                id = UUID.randomUUID().toString(),
                channelId = channelId,
                serverId = channel.serverId,
                authorId = state.currentUserId,
                content = content.trim(),
                timestamp = System.currentTimeMillis()
            )
            communityRepo.sendMessage(message)
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            communityRepo.deleteMessage(messageId)
        }
    }

    companion object {
        fun factory(application: Application, channelId: String) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ChannelChatViewModel(application, channelId) as T
            }
    }
}
