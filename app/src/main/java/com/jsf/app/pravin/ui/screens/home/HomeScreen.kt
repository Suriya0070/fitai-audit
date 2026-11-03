package com.jsf.app.pravin.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jsf.app.pravin.data.model.WorkoutSession
import com.jsf.app.pravin.domain.ExerciseDefinition
import com.jsf.app.pravin.domain.ExerciseLibrary
import com.jsf.app.pravin.ui.components.AvatarCanvas
import com.jsf.app.pravin.ui.components.MetricCard
import com.jsf.app.pravin.ui.components.ScoreGauge
import com.jsf.app.pravin.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    onNavigateToWorkout: (String) -> Unit = {},
    viewModel: HomeViewModel = viewModel()
) {
    val profile by viewModel.userProfile.collectAsState()
    val recentSessions by viewModel.recentSessions.collectAsState()
    val streak by viewModel.streak.collectAsState()
    val avatarLevel by viewModel.avatarLevel.collectAsState()
    val progressReport by viewModel.progressReport.collectAsState()

    val fitnessGoal by viewModel.fitnessGoal.collectAsState()
    val weeklyGoalSessions by viewModel.weeklyGoalSessions.collectAsState()

    val lastSession = recentSessions.firstOrNull()
    val exercises = remember { ExerciseLibrary.getPopular() }

    // Sessions done this week (Mon–Sun)
    val cal = java.util.Calendar.getInstance()
    cal.set(java.util.Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
    cal.set(java.util.Calendar.HOUR_OF_DAY, 0); cal.set(java.util.Calendar.MINUTE, 0)
    val weekStart = cal.timeInMillis
    val sessionsThisWeek = recentSessions.count { it.timestamp >= weekStart }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // Greeting header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Hey, ${profile?.name?.ifBlank { "Athlete" } ?: "Athlete"}!",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextDark
                    )
                    Text(
                        text = "Ready to crush it today?",
                        fontSize = 14.sp,
                        color = TextGray
                    )
                }
                // Streak badge
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.LocalFireDepartment,
                        contentDescription = "Streak",
                        tint = OrangeWarning,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "$streak days",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = OrangeWarning
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // AI Workout Score + Avatar row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Score gauge
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = LightCard),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Last Session", fontSize = 12.sp, color = TextGray)
                        Spacer(Modifier.height(8.dp))
                        ScoreGauge(
                            score = lastSession?.totalScore ?: 0f,
                            label = "AI Score",
                            size = 110.dp
                        )
                        Spacer(Modifier.height(8.dp))
                        if (lastSession != null) {
                            Text(
                                text = lastSession.exerciseType,
                                fontSize = 13.sp,
                                color = CyanPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Avatar
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = LightCard),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = when (avatarLevel) {
                                0 -> "Beginner"
                                1 -> "Warrior"
                                2 -> "Athlete"
                                3 -> "Champion"
                                4 -> "Legend"
                                else -> "Beginner"
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (avatarLevel) {
                                1 -> CyanPrimary
                                2 -> NeonGreen
                                3 -> OrangeWarning
                                4 -> ScoreGold
                                else -> TextSecondary
                            }
                        )
                        AvatarCanvas(
                            level = avatarLevel,
                            size = 110.dp
                        )
                        Text(
                            text = "$streak day streak",
                            fontSize = 12.sp,
                            color = TextGray
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricCard(
                    label = "Sessions",
                    value = "${progressReport.totalSessions}",
                    color = CyanPrimary,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    label = "Avg Score",
                    value = "${progressReport.avgScore.toInt()}",
                    color = GreenSuccess,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    label = "Improvement",
                    value = if (progressReport.improvement >= 0) "+${progressReport.improvement.toInt()}" else "${progressReport.improvement.toInt()}",
                    color = if (progressReport.improvement >= 0) NeonGreen else RedDanger,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(16.dp))

            // Weekly goal progress
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = LightCard),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Weekly Goal",
                                fontSize = 12.sp,
                                color = TextGray
                            )
                            Text(
                                "$sessionsThisWeek / $weeklyGoalSessions sessions",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                        }
                        Text(
                            fitnessGoal,
                            fontSize = 13.sp,
                            color = CyanPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { (sessionsThisWeek.toFloat() / weeklyGoalSessions.toFloat()).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = if (sessionsThisWeek >= weeklyGoalSessions) NeonGreen else CyanPrimary,
                        trackColor = LightBorder
                    )
                    if (sessionsThisWeek >= weeklyGoalSessions) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Weekly goal reached!",
                            fontSize = 12.sp,
                            color = GreenSuccess,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Quick Start section
            Text(
                text = "Quick Start",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Text(
                text = "Choose your exercise",
                fontSize = 13.sp,
                color = TextGray
            )

            Spacer(Modifier.height(12.dp))

            // Exercise buttons grid
            exercises.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { exercise ->
                        ExerciseButton(
                            exercise = exercise,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateToWorkout(exercise.name) }
                        )
                    }
                    // Fill empty slot if odd
                    if (row.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(16.dp))

            // Recent Sessions
            if (recentSessions.isNotEmpty()) {
                Text(
                    text = "Recent Sessions",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Spacer(Modifier.height(8.dp))
                recentSessions.take(3).forEach { session ->
                    SessionCard(session = session)
                    Spacer(Modifier.height(8.dp))
                }
            } else {
                // Empty state
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = LightCard),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.FitnessCenter,
                            contentDescription = null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "No sessions yet",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextDark
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Complete your first workout to unlock AI insights, scores, and progress tracking.",
                            fontSize = 13.sp,
                            color = TextGray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { onNavigateToWorkout("Squats") },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                        ) {
                            Text(
                                "Start Your First Workout",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepSpace
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(80.dp)) // Bottom nav padding
        }
    }
}

@Composable
private fun ExerciseButton(
    exercise: ExerciseDefinition,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = LightCard),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(text = exercise.emoji, fontSize = 28.sp)
            Column {
                Text(
                    text = exercise.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Text(
                    text = exercise.primaryMuscle,
                    fontSize = 11.sp,
                    color = CyanPrimary
                )
            }
        }
    }
}

@Composable
private fun SessionCard(session: WorkoutSession) {
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LightCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                tint = ScoreGold,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.exerciseType,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark
                )
                Text(
                    text = "${dateFormat.format(Date(session.timestamp))} • ${session.repCount} reps • ${session.durationSeconds}s",
                    fontSize = 11.sp,
                    color = TextGray
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${session.totalScore.toInt()}",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = when {
                        session.totalScore >= 80f -> NeonGreen
                        session.totalScore >= 60f -> OrangeWarning
                        else -> RedDanger
                    }
                )
                Text(text = "score", fontSize = 10.sp, color = TextGray)
            }
        }
    }
}
