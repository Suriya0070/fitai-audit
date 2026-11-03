package com.jsf.app.pravin.ui.screens.progress

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jsf.app.pravin.data.db.FitnessDatabase
import com.jsf.app.pravin.data.model.UserProfile
import com.jsf.app.pravin.data.model.WorkoutSession
import com.jsf.app.pravin.data.repository.FitnessRepository
import com.jsf.app.pravin.domain.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProgressViewModel(application: Application) : AndroidViewModel(application) {

    private val db = FitnessDatabase.getInstance(application)
    private val repository = FitnessRepository(db)
    private val progressEngine = ProgressEngine()

    val allSessions: StateFlow<List<WorkoutSession>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userProfile: StateFlow<UserProfile?> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _progressReport = MutableStateFlow(ProgressReport())
    val progressReport: StateFlow<ProgressReport> = _progressReport.asStateFlow()

    private val _weeklyScores = MutableStateFlow<List<Pair<String, Float>>>(emptyList())
    val weeklyScores: StateFlow<List<Pair<String, Float>>> = _weeklyScores.asStateFlow()

    // ── Per-exercise filter ───────────────────────────────────────────────────
    private val _selectedExercise = MutableStateFlow("Squats")
    val selectedExercise: StateFlow<String> = _selectedExercise.asStateFlow()

    val exerciseScores: StateFlow<List<Pair<String, Float>>> =
        combine(_selectedExercise, allSessions) { exercise, sessions ->
            progressEngine.getExerciseScores(sessions, exercise)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val exerciseMetrics: StateFlow<List<Pair<String, Float>>> =
        combine(_selectedExercise, allSessions) { exercise, sessions ->
            progressEngine.getExerciseMetrics(sessions, exercise)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Personal records ──────────────────────────────────────────────────────
    val personalRecords: StateFlow<Map<String, PersonalRecord>> = allSessions.map { sessions ->
        progressEngine.getPersonalRecords(sessions)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val exercisePersonalRecord: StateFlow<PersonalRecord?> =
        combine(_selectedExercise, personalRecords) { exercise, records ->
            records[exercise]
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // ── Muscle group volume ────────────────────────────────────────────────────
    val muscleGroupVolume: StateFlow<Map<MuscleGroup, Int>> = allSessions.map { sessions ->
        progressEngine.getMuscleGroupVolume(sessions)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // ── Calendar heatmap data ─────────────────────────────────────────────────
    val calendarData: StateFlow<Map<Long, Float>> = allSessions.map { sessions ->
        progressEngine.getCalendarData(sessions)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    init {
        viewModelScope.launch {
            allSessions.collect { sessions ->
                _progressReport.value = progressEngine.generateReport(sessions)
                _weeklyScores.value = progressEngine.getWeeklyScores(sessions)
            }
        }
    }

    fun setSelectedExercise(name: String) {
        _selectedExercise.value = name
    }
}
