package com.jsf.app.pravin.ui.screens.routine

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jsf.app.pravin.data.model.RoutineExercise
import com.jsf.app.pravin.ui.theme.*

private data class ExerciseDraft(
    val name: String,
    val sets: Int,
    val reps: Int,
    val durationSeconds: Int,
    val restSeconds: Int,
    val notes: String,
    val isTimeBased: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRoutineScreen(
    serverId: String,
    onNavigateBack: () -> Unit,
    onRoutineCreated: (String) -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as android.app.Application
    val viewModel: WorkoutRoutineViewModel = viewModel(
        factory = WorkoutRoutineViewModel.factory(app, serverId)
    )

    var step by remember { mutableIntStateOf(1) }

    // Step 1 state
    var routineName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var difficulty by remember { mutableStateOf("Beginner") }
    val selectedMuscles = remember { mutableStateListOf<String>() }
    var nameError by remember { mutableStateOf(false) }

    // Step 2 state
    val exercises = remember { mutableStateListOf<ExerciseDraft>() }
    var showAddExerciseSheet by remember { mutableStateOf(false) }

    if (step == 1) {
        RoutineInfoStep(
            name = routineName,
            onNameChange = { routineName = it; nameError = false },
            description = description,
            onDescriptionChange = { description = it },
            difficulty = difficulty,
            onDifficultyChange = { difficulty = it },
            selectedMuscles = selectedMuscles,
            nameError = nameError,
            onNavigateBack = onNavigateBack,
            onNext = {
                if (routineName.isBlank()) {
                    nameError = true
                } else {
                    step = 2
                }
            }
        )
    } else {
        ExerciseBuilderStep(
            exercises = exercises,
            showAddSheet = showAddExerciseSheet,
            onShowAddSheet = { showAddExerciseSheet = it },
            onNavigateBack = { step = 1 },
            onAddExercise = { draft -> exercises.add(draft) },
            onRemoveExercise = { idx -> exercises.removeAt(idx) },
            onMoveUp = { idx -> if (idx > 0) { val e = exercises[idx]; exercises[idx] = exercises[idx - 1]; exercises[idx - 1] = e } },
            onMoveDown = { idx -> if (idx < exercises.lastIndex) { val e = exercises[idx]; exercises[idx] = exercises[idx + 1]; exercises[idx + 1] = e } },
            onCreateRoutine = {
                val routineId = viewModel.createRoutine(
                    name = routineName,
                    description = description,
                    difficulty = difficulty,
                    targetMuscleGroups = selectedMuscles.joinToString(",")
                )
                exercises.forEachIndexed { index, draft ->
                    viewModel.addExercise(
                        routineId = routineId,
                        name = draft.name,
                        sets = draft.sets,
                        reps = draft.reps,
                        durationSeconds = draft.durationSeconds,
                        restSeconds = draft.restSeconds,
                        notes = draft.notes,
                        isTimeBased = draft.isTimeBased,
                        position = index
                    )
                }
                onRoutineCreated(routineId)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun RoutineInfoStep(
    name: String,
    onNameChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    difficulty: String,
    onDifficultyChange: (String) -> Unit,
    selectedMuscles: MutableList<String>,
    nameError: Boolean,
    onNavigateBack: () -> Unit,
    onNext: () -> Unit
) {
    val difficulties = listOf("Beginner", "Intermediate", "Advanced")
    val muscles = listOf("Chest", "Back", "Legs", "Shoulders", "Arms", "Core", "Full Body")

    Scaffold(
        containerColor = DeepSpace,
        topBar = {
            TopAppBar(
                title = { Text("Create Routine", fontWeight = FontWeight.ExtraBold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkNavy)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Step 1 of 2 — Routine Info", fontSize = 13.sp, color = TextSecondary)

            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Routine Name *", color = TextSecondary) },
                isError = nameError,
                supportingText = if (nameError) ({ Text("Name is required", color = RedDanger) }) else null,
                modifier = Modifier.fillMaxWidth(),
                colors = routineFieldColors(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChange,
                label = { Text("Description", color = TextSecondary) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                colors = routineFieldColors(),
                shape = RoundedCornerShape(12.dp),
                minLines = 3
            )

            Text("Difficulty", fontSize = 14.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                difficulties.forEach { d ->
                    val selected = difficulty == d
                    val color = when (d) {
                        "Beginner" -> NeonGreen
                        "Intermediate" -> OrangeWarning
                        else -> RedDanger
                    }
                    FilterChip(
                        selected = selected,
                        onClick = { onDifficultyChange(d) },
                        label = { Text(d, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = color.copy(alpha = 0.25f),
                            selectedLabelColor = color,
                            containerColor = CardDark,
                            labelColor = TextSecondary
                        )
                    )
                }
            }

            Text("Target Muscles", fontSize = 14.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                muscles.forEach { muscle ->
                    val selected = muscle in selectedMuscles
                    FilterChip(
                        selected = selected,
                        onClick = {
                            if (selected) selectedMuscles.remove(muscle)
                            else selectedMuscles.add(muscle)
                        },
                        label = { Text(muscle, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyanPrimary.copy(alpha = 0.2f),
                            selectedLabelColor = CyanPrimary,
                            containerColor = CardDark,
                            labelColor = TextSecondary
                        )
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = DeepSpace)
            ) {
                Text("Next: Add Exercises", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseBuilderStep(
    exercises: List<ExerciseDraft>,
    showAddSheet: Boolean,
    onShowAddSheet: (Boolean) -> Unit,
    onNavigateBack: () -> Unit,
    onAddExercise: (ExerciseDraft) -> Unit,
    onRemoveExercise: (Int) -> Unit,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
    onCreateRoutine: () -> Unit
) {
    Scaffold(
        containerColor = DeepSpace,
        topBar = {
            TopAppBar(
                title = { Text("Add Exercises", fontWeight = FontWeight.ExtraBold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkNavy)
            )
        },
        bottomBar = {
            Surface(color = DarkNavy) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onShowAddSheet(true) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanPrimary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanPrimary)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Add Exercise", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onCreateRoutine,
                        enabled = exercises.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = DeepSpace)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Create Routine", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        if (exercises.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.FitnessCenter, null, tint = TextSecondary, modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Step 2 of 2 — Add Exercises", fontSize = 13.sp, color = TextSecondary)
                    Spacer(Modifier.height(4.dp))
                    Text("Tap 'Add Exercise' to build your routine", fontSize = 14.sp, color = TextSecondary)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        "Step 2 of 2 — ${exercises.size} exercise${if (exercises.size == 1) "" else "s"}",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(4.dp))
                }
                itemsIndexed(exercises) { idx, draft ->
                    ExerciseDraftCard(
                        draft = draft,
                        index = idx,
                        canMoveUp = idx > 0,
                        canMoveDown = idx < exercises.lastIndex,
                        onMoveUp = { onMoveUp(idx) },
                        onMoveDown = { onMoveDown(idx) },
                        onDelete = { onRemoveExercise(idx) }
                    )
                }
            }
        }
    }

    if (showAddSheet) {
        AddExerciseBottomSheet(
            onDismiss = { onShowAddSheet(false) },
            onAdd = { draft ->
                onAddExercise(draft)
                onShowAddSheet(false)
            }
        )
    }
}

@Composable
private fun ExerciseDraftCard(
    draft: ExerciseDraft,
    index: Int,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = CyanPrimary.copy(alpha = 0.15f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "${index + 1}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanPrimary
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(draft.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                val summary = if (draft.isTimeBased) {
                    "${draft.sets} sets × ${draft.durationSeconds}s  |  ${draft.restSeconds}s rest"
                } else {
                    "${draft.sets} × ${draft.reps} reps  |  ${draft.restSeconds}s rest"
                }
                Text(summary, fontSize = 12.sp, color = TextSecondary)
            }
            Column {
                IconButton(onClick = onMoveUp, enabled = canMoveUp, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.KeyboardArrowUp, null, tint = if (canMoveUp) TextSecondary else TextSecondary.copy(alpha = 0.3f), modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onMoveDown, enabled = canMoveDown, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.KeyboardArrowDown, null, tint = if (canMoveDown) TextSecondary else TextSecondary.copy(alpha = 0.3f), modifier = Modifier.size(18.dp))
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, null, tint = RedDanger.copy(alpha = 0.7f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExerciseBottomSheet(
    onDismiss: () -> Unit,
    onAdd: (ExerciseDraft) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var sets by remember { mutableStateOf("3") }
    var reps by remember { mutableStateOf("10") }
    var durationSeconds by remember { mutableStateOf("30") }
    var restSeconds by remember { mutableStateOf("60") }
    var notes by remember { mutableStateOf("") }
    var isTimeBased by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkNavy
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Add Exercise", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

            OutlinedTextField(
                value = name,
                onValueChange = { name = it; nameError = false },
                label = { Text("Exercise Name *", color = TextSecondary) },
                isError = nameError,
                modifier = Modifier.fillMaxWidth(),
                colors = routineFieldColors(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Reps", fontSize = 14.sp, color = if (!isTimeBased) CyanPrimary else TextSecondary)
                Switch(
                    checked = isTimeBased,
                    onCheckedChange = { isTimeBased = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = CyanPrimary, checkedTrackColor = CyanPrimary.copy(alpha = 0.4f))
                )
                Text("Duration", fontSize = 14.sp, color = if (isTimeBased) CyanPrimary else TextSecondary)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = sets,
                    onValueChange = { sets = it },
                    label = { Text("Sets", color = TextSecondary) },
                    modifier = Modifier.weight(1f),
                    colors = routineFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                if (!isTimeBased) {
                    OutlinedTextField(
                        value = reps,
                        onValueChange = { reps = it },
                        label = { Text("Reps", color = TextSecondary) },
                        modifier = Modifier.weight(1f),
                        colors = routineFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                } else {
                    OutlinedTextField(
                        value = durationSeconds,
                        onValueChange = { durationSeconds = it },
                        label = { Text("Duration (s)", color = TextSecondary) },
                        modifier = Modifier.weight(1f),
                        colors = routineFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
                OutlinedTextField(
                    value = restSeconds,
                    onValueChange = { restSeconds = it },
                    label = { Text("Rest (s)", color = TextSecondary) },
                    modifier = Modifier.weight(1f),
                    colors = routineFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optional)", color = TextSecondary) },
                modifier = Modifier.fillMaxWidth(),
                colors = routineFieldColors(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Button(
                onClick = {
                    if (name.isBlank()) { nameError = true; return@Button }
                    onAdd(
                        ExerciseDraft(
                            name = name.trim(),
                            sets = sets.toIntOrNull() ?: 3,
                            reps = reps.toIntOrNull() ?: 10,
                            durationSeconds = durationSeconds.toIntOrNull() ?: 30,
                            restSeconds = restSeconds.toIntOrNull() ?: 60,
                            notes = notes.trim(),
                            isTimeBased = isTimeBased
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = DeepSpace)
            ) {
                Text("Add to Routine", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun routineFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = CyanPrimary,
    unfocusedBorderColor = CardDark,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    cursorColor = CyanPrimary,
    focusedContainerColor = DarkNavy,
    unfocusedContainerColor = DarkNavy
)
