package com.jsf.app.pravin.ui.screens.workout

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.jsf.app.pravin.data.model.Achievement
import com.jsf.app.pravin.domain.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import com.jsf.app.pravin.ui.components.ScoreGauge
import com.jsf.app.pravin.ui.theme.*
import java.util.concurrent.Executors

// NOTE: Place pose_landmarker_full.task in app/src/main/assets/ before running.
// Download from:
// https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_full/float16/latest/pose_landmarker_full.task

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun WorkoutScreen(
    initialExercise: String = "Squats",
    viewModel: WorkoutViewModel = viewModel()
) {
    val cameraPermission = rememberPermissionState(android.Manifest.permission.CAMERA)
    val workoutState by viewModel.workoutState.collectAsState()
    val modelLoaded by viewModel.modelLoaded.collectAsState()
    val poseResult by viewModel.poseResult.collectAsState()

    LaunchedEffect(initialExercise) {
        viewModel.setExercise(initialExercise)
    }

    if (!cameraPermission.status.isGranted) {
        CameraPermissionRequest(onRequest = { cameraPermission.launchPermissionRequest() })
        return
    }

    // Camera is active during both EnvironmentCheck and Active.
    // By keeping it in ONE if-block here (not inside each sub-screen), the AndroidView
    // is reused across the EnvironmentCheck→Active transition — no rebind, no flicker.
    val showCamera = workoutState is WorkoutState.EnvironmentCheck || workoutState is WorkoutState.Active
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Keep screen on whenever a workout session is in progress so users don't
    // have to touch the phone while exercising at a distance.
    val workoutInProgress = workoutState is WorkoutState.EnvironmentCheck
            || workoutState is WorkoutState.Active
            || workoutState is WorkoutState.Rest
    val view = LocalView.current
    DisposableEffect(workoutInProgress) {
        view.keepScreenOn = workoutInProgress
        onDispose { view.keepScreenOn = false }
    }

    val newAchievements by viewModel.newAchievements.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        if (showCamera) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    }.also { previewView ->
                        setupCamera(ctx, lifecycleOwner, previewView, viewModel)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            PoseSkeletonOverlay(poseResult = poseResult, modifier = Modifier.fillMaxSize())
        }

        when (val state = workoutState) {
            is WorkoutState.Idle -> IdleWorkoutScreen(viewModel = viewModel, modelLoaded = modelLoaded)
            is WorkoutState.Instructions -> InstructionsScreen(viewModel = viewModel)
            is WorkoutState.EnvironmentCheck -> EnvironmentCheckScreen(viewModel = viewModel)
            is WorkoutState.Active -> ActiveWorkoutScreen(viewModel = viewModel)
            is WorkoutState.Rest -> RestTimerScreen(state = state, viewModel = viewModel)
            is WorkoutState.Summary -> WorkoutSummaryScreen(
                session = state.session,
                onDone = { viewModel.resetToIdle() }
            )
        }

        if (newAchievements.isNotEmpty()) {
            AchievementUnlockedDialog(
                achievements = newAchievements,
                onDismiss = { viewModel.clearNewAchievements() }
            )
        }
    }
}

@Composable
private fun CameraPermissionRequest(onRequest: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSpace),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = OrangeWarning,
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Camera Permission Required",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "FitAI needs camera access to analyze your workout form in real-time.",
                fontSize = 14.sp,
                color = TextSecondary
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onRequest,
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
            ) {
                Text("Grant Permission", color = DeepSpace, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun IdleWorkoutScreen(viewModel: WorkoutViewModel, modelLoaded: Boolean) {
    val selectedExercise by viewModel.selectedExercise.collectAsState()
    var selectedCategory by remember { mutableStateOf<ExerciseCategory?>(null) }

    val displayedExercises = remember(selectedCategory) {
        if (selectedCategory != null) ExerciseLibrary.getByCategory(selectedCategory!!)
        else ExerciseLibrary.all
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSpace)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(40.dp))
            Text("Workout", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
            Text("AI-powered real-time coaching", fontSize = 14.sp, color = TextSecondary)

            if (!modelLoaded) {
                Spacer(Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = OrangeWarning.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = OrangeWarning, modifier = Modifier.size(20.dp))
                        Text(
                            "Pose analysis unavailable. MediaPipe requires an ARM device or ARM64 emulator. On x86_64 emulators the native library cannot load — run on a physical device for rep counting.",
                            fontSize = 12.sp,
                            color = OrangeWarning
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Category filter chips
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Select Exercise", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text("${displayedExercises.size} exercises", fontSize = 12.sp, color = TextSecondary)
            }
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    label = { Text("🏋️ All", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyanPrimary.copy(alpha = 0.25f),
                        selectedLabelColor = CyanPrimary
                    )
                )
                ExerciseLibrary.getCategories().forEach { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = if (selectedCategory == category) null else category },
                        label = { Text("${category.emoji} ${category.displayName}", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyanPrimary.copy(alpha = 0.25f),
                            selectedLabelColor = CyanPrimary
                        )
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Exercise cards
            displayedExercises.forEach { exercise ->
                val isSelected = selectedExercise == exercise.name
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) CyanPrimary.copy(alpha = 0.18f) else CardDark
                    ),
                    shape = RoundedCornerShape(14.dp),
                    onClick = { viewModel.setExercise(exercise.name) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(exercise.emoji, fontSize = 26.sp)
                            Column {
                                Text(
                                    exercise.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) CyanPrimary else TextPrimary
                                )
                                Text(
                                    exercise.primaryMuscle,
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val (diffColor, diffBg) = when (exercise.difficulty) {
                                Difficulty.BEGINNER     -> Pair(NeonGreen, NeonGreen.copy(alpha = 0.15f))
                                Difficulty.INTERMEDIATE -> Pair(OrangeWarning, OrangeWarning.copy(alpha = 0.15f))
                                Difficulty.ADVANCED     -> Pair(RedDanger, RedDanger.copy(alpha = 0.15f))
                            }
                            Surface(shape = RoundedCornerShape(6.dp), color = diffBg) {
                                Text(
                                    exercise.difficulty.displayName,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = diffColor
                                )
                            }
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { viewModel.startInstructions() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
            ) {
                Text("Start Workout", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = DeepSpace)
            }
            Spacer(Modifier.height(80.dp))
        }
    }
}

// Camera overlay — no AndroidView here; camera is managed at WorkoutScreen level
@Composable
private fun EnvironmentCheckScreen(viewModel: WorkoutViewModel) {
    val envQuality by viewModel.environmentQuality.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = DeepSpace.copy(alpha = 0.9f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Environment Check", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.height(12.dp))

                EnvBar("Lighting", envQuality.brightness)
                EnvBar("Body Visibility", envQuality.bodyVisibility)
                EnvBar("Camera Angle", envQuality.cameraAngle)
                EnvBar("Sharpness", envQuality.sharpness)

                Spacer(Modifier.height(12.dp))
                Text(envQuality.feedback, fontSize = 13.sp, color = CyanPrimary)
                Spacer(Modifier.height(16.dp))

                val ready = envQuality.overall >= 0.6f
                Button(
                    onClick = { if (ready) viewModel.startWorkout() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (ready) NeonGreen else CardDark
                    ),
                    enabled = ready
                ) {
                    Text(
                        if (ready) "Begin Workout!" else "Adjust Environment",
                        color = if (ready) DeepSpace else TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun EnvBar(label: String, value: Float) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.sp, color = TextSecondary, modifier = Modifier.width(110.dp))
        LinearProgressIndicator(
            progress = { value.coerceIn(0f, 1f) },
            modifier = Modifier
                .weight(1f)
                .height(8.dp),
            color = when {
                value >= 0.7f -> NeonGreen
                value >= 0.4f -> OrangeWarning
                else -> RedDanger
            },
            trackColor = CardDark
        )
        Text(
            "${(value * 100).toInt()}%",
            fontSize = 11.sp,
            color = TextSecondary,
            modifier = Modifier.width(38.dp).padding(start = 6.dp)
        )
    }
}

// Active workout overlay — no AndroidView here; camera is managed at WorkoutScreen level
@Composable
private fun ActiveWorkoutScreen(viewModel: WorkoutViewModel) {
    val workoutScore by viewModel.workoutScore.collectAsState()
    val repCount by viewModel.repCount.collectAsState()
    val fatigueReport by viewModel.fatigueReport.collectAsState()
    val envQuality by viewModel.environmentQuality.collectAsState()
    val sessionDuration by viewModel.sessionDuration.collectAsState()
    val selectedExercise by viewModel.selectedExercise.collectAsState()
    val formErrors by viewModel.formErrors.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        // Top HUD
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Score + timer
                Card(
                    colors = CardDefaults.cardColors(containerColor = DeepSpace.copy(alpha = 0.85f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${workoutScore.overall.toInt()}",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = when {
                                workoutScore.overall >= 80 -> NeonGreen
                                workoutScore.overall >= 60 -> OrangeWarning
                                else -> RedDanger
                            }
                        )
                        Text("AI Score", fontSize = 10.sp, color = TextSecondary)
                        Spacer(Modifier.height(4.dp))
                        Text(formatDuration(sessionDuration), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CyanPrimary)
                    }
                }

                // Rep counter
                Card(
                    colors = CardDefaults.cardColors(containerColor = DeepSpace.copy(alpha = 0.85f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "$repCount",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = CyanPrimary
                        )
                        Text("Reps", fontSize = 10.sp, color = TextSecondary)
                        Spacer(Modifier.height(4.dp))
                        Text(selectedExercise, fontSize = 11.sp, color = TextSecondary)
                    }
                }

                // Env quality indicator
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            envQuality.overall >= 0.7f -> NeonGreen.copy(alpha = 0.2f)
                            envQuality.overall >= 0.4f -> OrangeWarning.copy(alpha = 0.2f)
                            else -> RedDanger.copy(alpha = 0.2f)
                        }
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${(envQuality.overall * 100).toInt()}%",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                envQuality.overall >= 0.7f -> NeonGreen
                                envQuality.overall >= 0.4f -> OrangeWarning
                                else -> RedDanger
                            }
                        )
                        Text("Env", fontSize = 10.sp, color = TextSecondary)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Real-time form feedback — shows the most severe error only to avoid clutter
            val topError = formErrors.maxByOrNull { it.severity.ordinal }
            AnimatedVisibility(
                visible = topError != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                topError?.let { error ->
                    val (bg, fg) = when (error.severity) {
                        FormError.Severity.MAJOR    -> Pair(RedDanger.copy(alpha = 0.2f), RedDanger)
                        FormError.Severity.MODERATE -> Pair(OrangeWarning.copy(alpha = 0.15f), OrangeWarning)
                        FormError.Severity.MINOR    -> Pair(CyanPrimary.copy(alpha = 0.12f), CyanPrimary)
                    }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = bg),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("⚠", fontSize = 14.sp)
                            Text(
                                error.message,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = fg,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // Fatigue/injury warning
            if (fatigueReport.injuryRisk > 0.3f) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = RedDanger.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = OrangeWarning, modifier = Modifier.size(16.dp))
                        Text(
                            fatigueReport.recommendation,
                            fontSize = 11.sp,
                            color = OrangeWarning,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Bottom controls
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 24.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Mini score row
                Card(
                    colors = CardDefaults.cardColors(containerColor = DeepSpace.copy(alpha = 0.9f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        MiniScore("Acc", workoutScore.accuracy)
                        MiniScore("Bal", workoutScore.balance)
                        MiniScore("Stab", workoutScore.stability)
                        MiniScore("Sym", workoutScore.symmetry)
                        MiniScore("Spd", workoutScore.speed)
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FloatingActionButton(
                            onClick = { viewModel.endSet() },
                            containerColor = CyanPrimary,
                            contentColor = DeepSpace,
                            shape = CircleShape,
                            modifier = Modifier.size(60.dp)
                        ) {
                            Icon(Icons.Default.Timer, contentDescription = "End Set", modifier = Modifier.size(28.dp))
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("End Set", fontSize = 10.sp, color = TextSecondary)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FloatingActionButton(
                            onClick = { viewModel.stopWorkout() },
                            containerColor = RedDanger,
                            contentColor = Color.White,
                            shape = CircleShape,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop", modifier = Modifier.size(36.dp))
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("End Workout", fontSize = 10.sp, color = TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniScore(label: String, value: Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "${value.toInt()}",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = when {
                value >= 80 -> NeonGreen
                value >= 60 -> OrangeWarning
                else -> RedDanger
            }
        )
        Text(label, fontSize = 9.sp, color = TextSecondary)
    }
}

@Composable
private fun InstructionsScreen(viewModel: WorkoutViewModel) {
    val selectedExercise by viewModel.selectedExercise.collectAsState()
    val exercise = ExerciseLibrary.getByName(selectedExercise)

    val tips = exercise?.formTips ?: listOf(
        "Position yourself in front of the camera",
        "Make sure your full body is visible",
        "Maintain good posture throughout the exercise",
        "Move at a controlled, steady pace"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSpace)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(40.dp))

            Text(text = exercise?.emoji ?: "🏋️", fontSize = 64.sp)

            Spacer(Modifier.height(12.dp))
            Text(selectedExercise, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
            if (exercise != null) {
                Text(
                    "${exercise.primaryMuscle} · ${exercise.difficulty.displayName}",
                    fontSize = 13.sp,
                    color = CyanPrimary
                )
                Spacer(Modifier.height(4.dp))
            }
            Text("Form tips", fontSize = 14.sp, color = TextSecondary)

            Spacer(Modifier.height(28.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = CardDark),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    tips.forEachIndexed { index, tip ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(CyanPrimary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "${index + 1}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepSpace
                                )
                            }
                            Text(
                                tip,
                                fontSize = 14.sp,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (index < tips.lastIndex) Spacer(Modifier.height(14.dp))
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = { viewModel.startEnvironmentCheck() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = DeepSpace, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text("Ready — Start Camera Check", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = DeepSpace)
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun RestTimerScreen(state: WorkoutState.Rest, viewModel: WorkoutViewModel) {
    val progress = state.secondsLeft / 60f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSpace),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text("Rest", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
            Text("Set ${state.setsCompleted} complete — ${state.totalReps} reps total", fontSize = 14.sp, color = TextSecondary)

            Spacer(Modifier.height(40.dp))

            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.size(180.dp),
                    color = CyanPrimary,
                    trackColor = CardDark,
                    strokeWidth = 12.dp
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${state.secondsLeft}",
                        fontSize = 56.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CyanPrimary
                    )
                    Text("seconds", fontSize = 14.sp, color = TextSecondary)
                }
            }

            Spacer(Modifier.height(48.dp))

            Button(
                onClick = { viewModel.skipRest() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
            ) {
                Text("Skip Rest — Start Next Set", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DeepSpace)
            }

            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                onClick = { viewModel.stopWorkout() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = RedDanger),
                border = androidx.compose.foundation.BorderStroke(1.dp, RedDanger)
            ) {
                Text("End Workout", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun WorkoutSummaryScreen(
    session: com.jsf.app.pravin.data.model.WorkoutSession,
    onDone: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSpace)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(40.dp))
            Text("Workout Complete!", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = NeonGreen)
            Text(session.exerciseType, fontSize = 16.sp, color = TextSecondary)

            Spacer(Modifier.height(24.dp))

            ScoreGauge(score = session.totalScore, label = "Final AI Score", size = 160.dp)

            Spacer(Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatBox("Reps", "${session.repCount}", Modifier.weight(1f))
                StatBox("Duration", formatDuration(session.durationSeconds), Modifier.weight(1f))
                StatBox("Env Quality", "${(session.environmentQuality).toInt()}%", Modifier.weight(1f))
            }

            Spacer(Modifier.height(16.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = CardDark),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Score Breakdown", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(Modifier.height(12.dp))
                    ScoreRow("Accuracy", session.accuracyScore)
                    ScoreRow("Balance", session.balanceScore)
                    ScoreRow("Stability", session.stabilityScore)
                    ScoreRow("Symmetry", session.symmetryScore)
                    ScoreRow("Speed", session.speedScore)
                }
            }

            Spacer(Modifier.height(16.dp))

            if (session.injuryRisk > 0.2f) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = OrangeWarning.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = OrangeWarning, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Injury Risk: ${(session.injuryRisk * 100).toInt()}%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = OrangeWarning)
                            Text("Fatigue: ${(session.fatigueLevel * 100).toInt()}%", fontSize = 12.sp, color = TextSecondary)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            Button(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
            ) {
                Text("Done", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DeepSpace)
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun StatBox(label: String, value: String, modifier: Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CyanPrimary)
            Text(label, fontSize = 11.sp, color = TextSecondary)
        }
    }
}

@Composable
private fun ScoreRow(label: String, value: Float) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp, color = TextSecondary, modifier = Modifier.width(80.dp))
        LinearProgressIndicator(
            progress = { (value / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .weight(1f)
                .height(6.dp),
            color = when {
                value >= 80f -> NeonGreen
                value >= 60f -> OrangeWarning
                else -> RedDanger
            },
            trackColor = DeepSpace
        )
        Text(
            "${value.toInt()}",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = when {
                value >= 80f -> NeonGreen
                value >= 60f -> OrangeWarning
                else -> RedDanger
            },
            modifier = Modifier.width(36.dp).padding(start = 8.dp)
        )
    }
}

@Composable
private fun PoseSkeletonOverlay(poseResult: PoseResult, modifier: Modifier) {
    if (poseResult.landmarks.isEmpty()) return
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        fun lm(idx: Int): Offset? {
            val lm = poseResult.getLandmark(idx) ?: return null
            if (lm.visibility < 0.1f) return null
            return Offset(lm.x * w, lm.y * h)
        }

        val connections = listOf(
            Pair(11, 12),
            Pair(11, 13), Pair(13, 15),
            Pair(12, 14), Pair(14, 16),
            Pair(11, 23), Pair(12, 24),
            Pair(23, 24),
            Pair(23, 25), Pair(25, 27),
            Pair(24, 26), Pair(26, 28)
        )

        val jointColor = CyanPrimary
        val boneColor = NeonGreen.copy(alpha = 0.7f)

        connections.forEach { (a, b) ->
            val start = lm(a) ?: return@forEach
            val end = lm(b) ?: return@forEach
            drawLine(color = boneColor, start = start, end = end, strokeWidth = 4f, cap = StrokeCap.Round)
        }

        for (idx in listOf(0, 11, 12, 13, 14, 15, 16, 23, 24, 25, 26, 27, 28)) {
            val pt = lm(idx) ?: continue
            drawCircle(color = jointColor, radius = 6f, center = pt)
            drawCircle(color = DeepSpace, radius = 3f, center = pt, style = Stroke(2f))
        }
    }
}

private fun setupCamera(
    context: Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    previewView: PreviewView,
    viewModel: WorkoutViewModel
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()
        val preview = Preview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
        }
        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

        val imageAnalyzer = ImageAnalysis.Builder()
            .setTargetResolution(android.util.Size(640, 480))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
            .also { analysis ->
                // Reuse the ViewModel's executor — avoids creating a leaked thread per setup call
                analysis.setAnalyzer(viewModel.cameraExecutor) { imageProxy ->
                    val frameTime = imageProxy.imageInfo.timestamp
                    val bitmap = imageProxyToBitmap(imageProxy)
                    if (bitmap != null) {
                        viewModel.onNewFrame(bitmap, frameTime)
                    }
                    imageProxy.close()
                }
            }

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalyzer)
        } catch (e: Exception) {
            Log.e("WorkoutScreen", "Camera binding failed", e)
        }
    }, ContextCompat.getMainExecutor(context))
}

private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
    return try {
        val plane = imageProxy.planes[0]
        val buffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val width = imageProxy.width
        val height = imageProxy.height

        if (pixelStride == 1) {
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } else {
            val rowPadding = rowStride - pixelStride * width
            buffer.rewind()
            if (rowPadding == 0) {
                val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                bmp.copyPixelsFromBuffer(buffer)
                bmp
            } else {
                val paddedWidth = rowStride / pixelStride
                val padded = Bitmap.createBitmap(paddedWidth, height, Bitmap.Config.ARGB_8888)
                padded.copyPixelsFromBuffer(buffer)
                Bitmap.createBitmap(padded, 0, 0, width, height)
            }
        }
    } catch (e: Exception) {
        null
    }
}

private fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
}

@Composable
private fun AchievementUnlockedDialog(
    achievements: List<Achievement>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = com.jsf.app.pravin.ui.theme.CardDark,
        title = {
            androidx.compose.foundation.layout.Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("🏆", fontSize = 24.sp)
                Text(
                    if (achievements.size == 1) "Achievement Unlocked!" else "${achievements.size} Achievements Unlocked!",
                    color = ScoreGold,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            androidx.compose.foundation.layout.Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                achievements.forEach { a ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ScoreGold.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(achievementEmoji(a.type), fontSize = 28.sp)
                            androidx.compose.foundation.layout.Column {
                                Text(a.title, color = TextDark, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(a.description, color = TextGray, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
            ) {
                Text("Nice!", color = DeepSpace, fontWeight = FontWeight.Bold)
            }
        }
    )
}

private fun achievementEmoji(type: String) = when (type) {
    Achievement.AchievementType.FIRST_WORKOUT    -> "🏋️"
    Achievement.AchievementType.STREAK_7         -> "🔥"
    Achievement.AchievementType.STREAK_30        -> "⚡"
    Achievement.AchievementType.TOTAL_REPS_1000  -> "💪"
    Achievement.AchievementType.TOTAL_REPS_10000 -> "🦾"
    Achievement.AchievementType.PERFECT_SCORE    -> "⭐"
    Achievement.AchievementType.CHALLENGE_COMPLETE -> "🏆"
    Achievement.AchievementType.FIRST_COMMUNITY  -> "🌐"
    Achievement.AchievementType.LIVE_WORKOUT     -> "📺"
    else -> "🎖️"
}
