package com.jsf.app.pravin.ui.screens.routine

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jsf.app.pravin.data.model.WorkoutRoutine
import com.jsf.app.pravin.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerRoutinesScreen(
    serverId: String,
    onNavigateBack: () -> Unit,
    onNavigateToCreateRoutine: () -> Unit,
    onNavigateToRoutine: (String) -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as android.app.Application
    val viewModel: WorkoutRoutineViewModel = viewModel(
        factory = WorkoutRoutineViewModel.factory(app, serverId)
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = DeepSpace,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Workout Routines",
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkNavy)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreateRoutine,
                containerColor = CyanPrimary,
                contentColor = DeepSpace
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Routine")
            }
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CyanPrimary)
            }
            return@Scaffold
        }

        if (state.routines.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.FitnessCenter,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "No routines yet",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Create your first workout plan",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.routines, key = { it.id }) { routine ->
                    RoutineCard(
                        routine = routine,
                        onClick = { onNavigateToRoutine(routine.id) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineCard(routine: WorkoutRoutine, onClick: () -> Unit) {
    val difficultyColor = when (routine.difficulty) {
        "Beginner" -> NeonGreen
        "Intermediate" -> OrangeWarning
        "Advanced" -> RedDanger
        else -> CyanPrimary
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = routine.name,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = difficultyColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = routine.difficulty,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = difficultyColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            if (routine.description.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = routine.description,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (routine.targetMuscleGroups.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(routine.targetMuscleGroups.split(",").filter { it.isNotBlank() }) { muscle ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = CyanPrimary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = muscle.trim(),
                                fontSize = 11.sp,
                                color = CyanPrimary,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Timer,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "~${routine.estimatedDuration} min",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }
    }
}
