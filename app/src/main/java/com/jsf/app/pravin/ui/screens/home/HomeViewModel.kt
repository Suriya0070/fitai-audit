package com.jsf.app.pravin.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jsf.app.pravin.data.db.FitnessDatabase
import com.jsf.app.pravin.data.model.UserProfile
import com.jsf.app.pravin.data.model.WorkoutSession
import com.jsf.app.pravin.data.preferences.UserPreferences
import com.jsf.app.pravin.data.repository.FitnessRepository
import com.jsf.app.pravin.domain.ConsistencyTracker
import com.jsf.app.pravin.domain.ProgressEngine
import com.jsf.app.pravin.domain.ProgressReport
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = FitnessDatabase.getInstance(application)
    private val repository = FitnessRepository(db)
    private val prefs = UserPreferences(application)
    private val consistencyTracker = ConsistencyTracker()
    private val progressEngine = ProgressEngine()

    val userProfile: StateFlow<UserProfile?> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val recentSessions: StateFlow<List<WorkoutSession>> = repository.recentSessions(10)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _streak = MutableStateFlow(0)
    val streak: StateFlow<Int> = _streak.asStateFlow()

    private val _avatarLevel = MutableStateFlow(0)
    val avatarLevel: StateFlow<Int> = _avatarLevel.asStateFlow()

    private val _progressReport = MutableStateFlow(ProgressReport())
    val progressReport: StateFlow<ProgressReport> = _progressReport.asStateFlow()

    val selectedExercise: StateFlow<String> = prefs.selectedExercise
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Squats")

    val fitnessGoal: StateFlow<String> = prefs.fitnessGoal
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Stay Active")

    val weeklyGoalSessions: StateFlow<Int> = prefs.weeklyGoalSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 3)

    init {
        viewModelScope.launch {
            repository.allSessions.collect { sessions ->
                _progressReport.value = progressEngine.generateReport(sessions)
            }
        }
        viewModelScope.launch {
            refreshStreak()
        }
    }

    fun refreshStreak() {
        viewModelScope.launch {
            val timestamps = repository.getAllTimestamps()
            val s = consistencyTracker.calculateStreak(timestamps)
            _streak.value = s
            _avatarLevel.value = consistencyTracker.getAvatarLevel(s)
        }
    }

    fun setSelectedExercise(exercise: String) {
        viewModelScope.launch {
            prefs.setSelectedExercise(exercise)
        }
    }
}
