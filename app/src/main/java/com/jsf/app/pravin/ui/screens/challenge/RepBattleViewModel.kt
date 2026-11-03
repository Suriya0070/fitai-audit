package com.jsf.app.pravin.ui.screens.challenge

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jsf.app.pravin.data.db.FitnessDatabase
import com.jsf.app.pravin.data.preferences.UserPreferences
import com.jsf.app.pravin.data.remote.RepBattle
import com.jsf.app.pravin.data.remote.RepBattleDataSource
import com.jsf.app.pravin.data.remote.RepBattleEntry
import com.jsf.app.pravin.data.repository.FitnessRepository
import com.jsf.app.pravin.domain.ExerciseLibrary
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class RepBattleUiState(
    val battles: List<RepBattle> = emptyList(),
    val activeBattle: RepBattle? = null,
    val leaderboard: List<RepBattleEntry> = emptyList(),
    val myEntry: RepBattleEntry? = null,
    val currentUserId: String = "",
    val currentUserName: String = "",
    val isCreating: Boolean = false,
    val showCreateSheet: Boolean = false,
    val selectedExercise: String = "Squats",
    val selectedSets: Int = 3,
    val selectedDurationHours: Int = 24,
    val snackMessage: String? = null
)

class RepBattleViewModel(
    application: Application,
    val serverId: String
) : AndroidViewModel(application) {

    private val prefs = UserPreferences(application)
    private val db = FitnessDatabase.getInstance(application)
    private val repository = FitnessRepository(db)
    private val ds = RepBattleDataSource()

    private val _state = MutableStateFlow(RepBattleUiState())
    val state: StateFlow<RepBattleUiState> = _state.asStateFlow()

    val popularExercises = ExerciseLibrary.getPopular()

    init {
        viewModelScope.launch {
            combine(prefs.currentUserId, repository.userProfile) { uid, profile ->
                uid to (profile?.name ?: "")
            }.collect { (uid, name) ->
                _state.update { it.copy(currentUserId = uid, currentUserName = name) }
            }
        }
        viewModelScope.launch {
            ds.getActiveBattlesForServer(serverId).collect { battles ->
                _state.update { it.copy(battles = battles) }
                // Auto-select the first battle if none selected
                if (_state.value.activeBattle == null && battles.isNotEmpty()) {
                    selectBattle(battles.first())
                }
            }
        }
    }

    fun selectBattle(battle: RepBattle) {
        _state.update { it.copy(activeBattle = battle) }
        viewModelScope.launch {
            ds.getLeaderboard(battle.id).collect { board ->
                val myId = _state.value.currentUserId
                _state.update { it.copy(
                    leaderboard = board,
                    myEntry = board.find { e -> e.userId == myId }
                )}
            }
        }
    }

    fun createBattle() {
        val s = _state.value
        if (s.currentUserId.isEmpty()) return
        _state.update { it.copy(isCreating = true) }
        viewModelScope.launch {
            val id = ds.createBattle(
                serverId = serverId,
                createdBy = s.currentUserId,
                creatorName = s.currentUserName,
                exercise = s.selectedExercise,
                setsCount = s.selectedSets,
                durationHours = s.selectedDurationHours
            )
            _state.update { it.copy(isCreating = false, showCreateSheet = false, snackMessage = "Battle created!") }
        }
    }

    fun submitSet(setNumber: Int, reps: Int) {
        val s = _state.value
        val battle = s.activeBattle ?: return
        if (s.currentUserId.isEmpty()) return
        viewModelScope.launch {
            ds.submitSet(battle.id, s.currentUserId, s.currentUserName, setNumber, reps)
            _state.update { it.copy(snackMessage = "Set $setNumber recorded: $reps reps") }
        }
    }

    fun showCreateSheet() = _state.update { it.copy(showCreateSheet = true) }
    fun hideCreateSheet() = _state.update { it.copy(showCreateSheet = false) }
    fun setExercise(ex: String) = _state.update { it.copy(selectedExercise = ex) }
    fun setSets(n: Int) = _state.update { it.copy(selectedSets = n) }
    fun setDuration(h: Int) = _state.update { it.copy(selectedDurationHours = h) }
    fun clearSnack() = _state.update { it.copy(snackMessage = null) }

    companion object {
        fun factory(application: Application, serverId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    RepBattleViewModel(application, serverId) as T
            }
    }
}
