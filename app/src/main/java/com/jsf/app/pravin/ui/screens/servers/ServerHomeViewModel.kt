package com.jsf.app.pravin.ui.screens.servers

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jsf.app.pravin.data.db.FitnessDatabase
import com.jsf.app.pravin.data.model.Channel
import com.jsf.app.pravin.data.model.Server
import com.jsf.app.pravin.data.model.ServerMember
import com.jsf.app.pravin.data.repository.CommunityRepository
import kotlinx.coroutines.flow.*

class ServerHomeViewModel(application: Application, val serverId: String) : AndroidViewModel(application) {

    private val db = FitnessDatabase.getInstance(application)
    private val communityRepository = CommunityRepository(db)

    val server: StateFlow<Server?> = communityRepository.getServerById(serverId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val channels: StateFlow<List<Channel>> = communityRepository.getChannelsForServer(serverId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val members: StateFlow<List<ServerMember>> = communityRepository.getMembersOfServer(serverId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    companion object {
        fun factory(application: Application, serverId: String) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ServerHomeViewModel(application, serverId) as T
            }
    }
}
