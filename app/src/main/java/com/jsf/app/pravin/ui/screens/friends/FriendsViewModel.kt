package com.jsf.app.pravin.ui.screens.friends

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jsf.app.pravin.data.db.FitnessDatabase
import com.jsf.app.pravin.data.preferences.UserPreferences
import com.jsf.app.pravin.data.remote.FriendRequest
import com.jsf.app.pravin.data.remote.Friendship
import com.jsf.app.pravin.data.remote.FriendsDataSource
import com.jsf.app.pravin.data.remote.PublicUserProfile
import com.jsf.app.pravin.data.repository.FitnessRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class FriendsUiState(
    val friends: List<Friendship> = emptyList(),
    val receivedRequests: List<FriendRequest> = emptyList(),
    val sentRequestIds: Set<String> = emptySet(),
    val searchResults: List<PublicUserProfile> = emptyList(),
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val currentUserId: String = "",
    val currentUserName: String = "",
    val snackMessage: String? = null
)

@OptIn(FlowPreview::class)
class FriendsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = UserPreferences(application)
    private val db = FitnessDatabase.getInstance(application)
    private val repository = FitnessRepository(db)
    private val ds = FriendsDataSource()

    private val _state = MutableStateFlow(FriendsUiState())
    val state: StateFlow<FriendsUiState> = _state.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    init {
        viewModelScope.launch {
            combine(
                prefs.currentUserId,
                repository.userProfile
            ) { uid, profile -> uid to profile }
                .collect { (uid, profile) ->
                    val name = profile?.name ?: ""
                    _state.update { it.copy(currentUserId = uid, currentUserName = name) }
                    if (uid.isNotEmpty()) {
                        loadUserData(uid)
                        if (name.isNotEmpty() && profile != null) {
                            ds.syncUserProfile(uid, name, profile.fitnessLevel)
                        }
                    }
                }
        }

        viewModelScope.launch {
            _searchQuery
                .debounce(400)
                .collect { query ->
                    _state.update { it.copy(searchQuery = query, isSearching = query.length >= 2) }
                    val uid = _state.value.currentUserId
                    if (query.length >= 2 && uid.isNotEmpty()) {
                        ds.searchUsers(query, uid).collect { results ->
                            _state.update { it.copy(searchResults = results, isSearching = false) }
                        }
                    } else {
                        _state.update { it.copy(searchResults = emptyList(), isSearching = false) }
                    }
                }
        }
    }

    private fun loadUserData(userId: String) {
        viewModelScope.launch {
            ds.getFriends(userId).collect { friends ->
                _state.update { it.copy(friends = friends) }
            }
        }
        viewModelScope.launch {
            ds.getReceivedRequests(userId).collect { reqs ->
                _state.update { it.copy(receivedRequests = reqs) }
            }
        }
        viewModelScope.launch {
            ds.getSentRequests(userId).collect { sent ->
                _state.update { it.copy(sentRequestIds = sent.map { it.toUserId }.toSet()) }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun sendRequest(toUserId: String) {
        val state = _state.value
        if (state.currentUserId.isEmpty()) return
        viewModelScope.launch {
            ds.sendFriendRequest(state.currentUserId, state.currentUserName, toUserId)
            _state.update { it.copy(
                sentRequestIds = it.sentRequestIds + toUserId,
                snackMessage = "Friend request sent!"
            )}
        }
    }

    fun acceptRequest(request: FriendRequest) {
        val state = _state.value
        viewModelScope.launch {
            val profile = repository.userProfile.first()
            ds.acceptRequest(
                requestId = request.id,
                fromUserId = request.fromUserId,
                fromName = request.fromName,
                fromLevel = "",
                toUserId = state.currentUserId,
                toName = state.currentUserName,
                toLevel = profile?.fitnessLevel ?: ""
            )
            _state.update { it.copy(snackMessage = "You and ${request.fromName} are now friends!") }
        }
    }

    fun declineRequest(request: FriendRequest) {
        viewModelScope.launch {
            ds.declineRequest(request.id)
        }
    }

    fun removeFriend(friendship: Friendship) {
        viewModelScope.launch {
            ds.removeFriend(friendship.id)
            _state.update { it.copy(snackMessage = "Friend removed") }
        }
    }

    fun clearSnack() = _state.update { it.copy(snackMessage = null) }
}
