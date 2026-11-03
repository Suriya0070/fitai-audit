package com.jsf.app.pravin.ui.screens.routine

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jsf.app.pravin.data.db.FitnessDatabase
import com.jsf.app.pravin.data.model.RoutineExercise
import com.jsf.app.pravin.data.model.WorkoutRoutine
import com.jsf.app.pravin.data.preferences.UserPreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class RoutineListUiState(
    val routines: List<WorkoutRoutine> = emptyList(),
    val isLoading: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutRoutineViewModel(
    application: Application,
    private val serverId: String
) : AndroidViewModel(application) {

    private val db = FitnessDatabase.getInstance(application)
    private val routineDao = db.workoutRoutineDao()
    private val exerciseDao = db.routineExerciseDao()
    private val prefs = UserPreferences(application)

    val uiState: StateFlow<RoutineListUiState> = routineDao
        .getRoutinesForServer(serverId)
        .map { RoutineListUiState(routines = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RoutineListUiState(isLoading = true))

    fun getExercisesForRoutine(routineId: String) = exerciseDao.getExercisesForRoutine(routineId)

    fun createRoutine(
        name: String,
        description: String,
        difficulty: String,
        targetMuscleGroups: String
    ): String {
        val routineId = UUID.randomUUID().toString()
        viewModelScope.launch {
            val uid = prefs.currentUserId.first()
            routineDao.upsertRoutine(
                WorkoutRoutine(
                    id = routineId,
                    serverId = serverId,
                    createdBy = uid,
                    name = name,
                    description = description,
                    difficulty = difficulty,
                    targetMuscleGroups = targetMuscleGroups
                )
            )
        }
        return routineId
    }

    fun addExercise(
        routineId: String,
        name: String,
        sets: Int,
        reps: Int,
        durationSeconds: Int,
        restSeconds: Int,
        notes: String,
        isTimeBased: Boolean,
        position: Int
    ) {
        viewModelScope.launch {
            exerciseDao.upsertExercise(
                RoutineExercise(
                    routineId = routineId,
                    exerciseName = name,
                    sets = sets,
                    targetReps = reps,
                    targetDurationSeconds = durationSeconds,
                    restSeconds = restSeconds,
                    instructions = notes,
                    position = position,
                    isTimeBased = isTimeBased
                )
            )
        }
    }

    fun deleteRoutine(routineId: String) {
        viewModelScope.launch {
            val routines = uiState.value.routines
            val routine = routines.find { it.id == routineId } ?: return@launch
            exerciseDao.deleteExercisesForRoutine(routineId)
            routineDao.deleteRoutine(routine)
        }
    }

    fun reorderExercises(exercises: List<RoutineExercise>) {
        viewModelScope.launch {
            exercises.forEachIndexed { index, exercise ->
                exerciseDao.upsertExercise(exercise.copy(position = index))
            }
        }
    }

    companion object {
        fun factory(application: Application, serverId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                    WorkoutRoutineViewModel(application, serverId) as T
            }
    }
}
