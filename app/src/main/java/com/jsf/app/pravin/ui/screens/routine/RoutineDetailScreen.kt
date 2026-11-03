package com.jsf.app.pravin.ui.screens.routine

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jsf.app.pravin.data.db.FitnessDatabase
import com.jsf.app.pravin.data.model.RoutineExercise
import com.jsf.app.pravin.data.model.WorkoutRoutine
import com.jsf.app.pravin.ui.theme.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

data class RoutineDetailUiState(
    val routine: WorkoutRoutine? = null,
    val exercises: List<RoutineExercise> = emptyList(),
    val isLoading: Boolean = true
)

class RoutineDetailViewModel(application: Application, routineId: String) :
    AndroidViewModel(application) {

    private val db = FitnessDatabase.getInstance(application)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<RoutineDetailUiState> = db.workoutRoutineDao()
        .getRoutineById(routineId)
        .flatMapLatest { routine ->
            if (routine == null) {
                flowOf(RoutineDetailUiState(isLoading = false))
            } else {
                db.routineExerciseDao().getExercisesForRoutine(routineId)
                    .map { exercises -> RoutineDetailUiState(routine = routine, exercises = exercises, isLoading = false) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RoutineDetailUiState())

    companion object {
        fun factory(application: Application, routineId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                    RoutineDetailViewModel(application, routineId) as T
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineDetailScreen(
    routineId: String,
    onNavigateBack: () -> Unit,
    onStartWorkout: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as Application
    val viewModel: RoutineDetailViewModel = viewModel(
        factory = RoutineDetailViewModel.factory(app, routineId)
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = DeepSpace,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.routine?.name ?: "Routine",
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkNavy)
            )
        },
        bottomBar = {
            Surface(color = DarkNavy) {
                Button(
                    onClick = onStartWorkout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = DeepSpace)
                ) {
                    Icon(Icons.Default.FitnessCenter, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Start Workout", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CyanPrimary)
            }
            return@Scaffold
        }

        val routine = state.routine ?: return@Scaffold

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardDark)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val diffColor = when (routine.difficulty) {
                                "Beginner" -> NeonGreen
                                "Intermediate" -> OrangeWarning
                                else -> RedDanger
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = diffColor.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    routine.difficulty,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = diffColor,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Timer, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("~${routine.estimatedDuration} min", fontSize = 12.sp, color = TextSecondary)
                            }
                        }

                        if (routine.description.isNotBlank()) {
                            Spacer(Modifier.height(10.dp))
                            Text(routine.description, fontSize = 14.sp, color = TextSecondary)
                        }

                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FitnessCenter, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("${state.exercises.size} exercises", fontSize = 13.sp, color = TextSecondary)
                        }

                        if (routine.targetMuscleGroups.isNotBlank()) {
                            Spacer(Modifier.height(12.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(routine.targetMuscleGroups.split(",").filter { it.isNotBlank() }) { muscle ->
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = CyanPrimary.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            muscle.trim(),
                                            fontSize = 11.sp,
                                            color = CyanPrimary,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    "Exercises",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            itemsIndexed(state.exercises) { index, exercise ->
                ExerciseDetailCard(number = index + 1, exercise = exercise)
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun ExerciseDetailCard(number: Int, exercise: RoutineExercise) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = CyanPrimary.copy(alpha = 0.15f),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("$number", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyanPrimary)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(exercise.exerciseName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.height(4.dp))
                val metric = if (exercise.isTimeBased) {
                    "${exercise.sets} sets × ${exercise.targetDurationSeconds}s"
                } else {
                    "${exercise.sets} × ${exercise.targetReps} reps"
                }
                Text(metric, fontSize = 13.sp, color = CyanPrimary, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text("Rest: ${exercise.restSeconds}s", fontSize = 12.sp, color = TextSecondary)
                if (exercise.instructions.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        exercise.instructions,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        fontStyle = FontStyle.Italic
                    )
                }
            }
        }
    }
}
