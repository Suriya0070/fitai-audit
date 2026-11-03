package com.jsf.app.pravin.ui.screens.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jsf.app.pravin.data.model.WorkoutSession
import com.jsf.app.pravin.domain.ExerciseLibrary
import com.jsf.app.pravin.domain.PersonalRecord
import com.jsf.app.pravin.ui.components.*
import com.jsf.app.pravin.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@Composable
fun ProgressScreen(viewModel: ProgressViewModel = viewModel()) {
    val sessions by viewModel.allSessions.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBg)
    ) {
        if (sessions.isEmpty()) {
            EmptyProgressState()
        } else {
            ProgressContent(viewModel)
        }
    }
}

@Composable
private fun ProgressContent(viewModel: ProgressViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Overview", "By Exercise", "Muscles")

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Spacer(Modifier.height(8.dp))
            Text("Progress", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = TextDark)
            Text("Your fitness journey", fontSize = 14.sp, color = TextGray)
        }

        // Tab row
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = LightCard,
            contentColor = CyanPrimary
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            title,
                            fontSize = 13.sp,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    selectedContentColor = CyanPrimary,
                    unselectedContentColor = TextGray
                )
            }
        }

        // Tab content
        when (selectedTab) {
            0 -> OverviewTab(viewModel)
            1 -> ByExerciseTab(viewModel)
            2 -> MusclesTab(viewModel)
        }
    }
}

// ── Tab 1: Overview ────────────────────────────────────────────────────────────

@Composable
private fun OverviewTab(viewModel: ProgressViewModel) {
    val progressReport by viewModel.progressReport.collectAsStateWithLifecycle()
    val weeklyScores by viewModel.weeklyScores.collectAsStateWithLifecycle()
    val calendarData by viewModel.calendarData.collectAsStateWithLifecycle()
    val sessions by viewModel.allSessions.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        // Key metric cards
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard("Total Sessions", "${progressReport.totalSessions}", color = CyanPrimary, modifier = Modifier.weight(1f))
            MetricCard("Avg Score", "${progressReport.avgScore.toInt()}", color = GreenSuccess, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val trendColor = if (progressReport.weeklyTrend >= 0) NeonGreen else RedDanger
            MetricCard(
                label = "Weekly Trend",
                value = if (progressReport.weeklyTrend >= 0) "+${progressReport.weeklyTrend.toInt()}%" else "${progressReport.weeklyTrend.toInt()}%",
                color = trendColor,
                modifier = Modifier.weight(1f)
            )
            val impColor = if (progressReport.improvement >= 0) NeonGreen else RedDanger
            MetricCard(
                label = "Overall Change",
                value = if (progressReport.improvement >= 0) "+${progressReport.improvement.toInt()}" else "${progressReport.improvement.toInt()}",
                color = impColor,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(20.dp))

        // Weekly performance chart
        if (weeklyScores.isNotEmpty()) {
            SectionHeader("Weekly Performance")
            Spacer(Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = LightCard),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ProgressChart(
                    data = weeklyScores,
                    modifier = Modifier.fillMaxWidth().height(180.dp).padding(8.dp)
                )
            }
            Spacer(Modifier.height(20.dp))
        }

        // AI Insights
        Card(
            colors = CardDefaults.cardColors(containerColor = LightCard),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("AI Insights", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.TrendingUp, null, tint = GreenSuccess, modifier = Modifier.size(18.dp))
                            Text("Strength", fontSize = 12.sp, color = TextGray)
                        }
                        Text(progressReport.bestMetric, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = GreenSuccess)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.TrendingDown, null, tint = OrangeWarning, modifier = Modifier.size(18.dp))
                            Text("Focus on", fontSize = 12.sp, color = TextGray)
                        }
                        Text(progressReport.weakestMetric, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = OrangeWarning)
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Activity calendar
        SectionHeader("Activity — Last 5 Weeks")
        Spacer(Modifier.height(10.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = LightCard),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            CalendarHeatmap(
                data = calendarData,
                modifier = Modifier.padding(16.dp).fillMaxWidth()
            )
        }

        Spacer(Modifier.height(20.dp))

        // Recent session history
        SectionHeader("Session History")
        Spacer(Modifier.height(8.dp))
        sessions.forEach { session ->
            ProgressSessionCard(session = session)
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(80.dp))
    }
}

// ── Tab 2: By Exercise ─────────────────────────────────────────────────────────

@Composable
private fun ByExerciseTab(viewModel: ProgressViewModel) {
    val selectedExercise by viewModel.selectedExercise.collectAsStateWithLifecycle()
    val exerciseScores by viewModel.exerciseScores.collectAsStateWithLifecycle()
    val exerciseMetrics by viewModel.exerciseMetrics.collectAsStateWithLifecycle()
    val pr by viewModel.exercisePersonalRecord.collectAsStateWithLifecycle()
    val sessions by viewModel.allSessions.collectAsStateWithLifecycle()

    val exerciseNames = remember { ExerciseLibrary.getNames() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        // Exercise picker
        Text("Select Exercise", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            exerciseNames.forEach { name ->
                val exercise = ExerciseLibrary.getByName(name)
                val isSelected = name == selectedExercise
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.setSelectedExercise(name) },
                    label = {
                        Text(
                            "${exercise?.emoji ?: "🏋️"} $name",
                            fontSize = 12.sp
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyanPrimary.copy(alpha = 0.20f),
                        selectedLabelColor = CyanPrimary
                    )
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // No data state
        val hasData = exerciseScores.isNotEmpty()
        if (!hasData) {
            Card(
                colors = CardDefaults.cardColors(containerColor = LightCard),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(32.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.FitnessCenter, null, tint = TextGray, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "No sessions for $selectedExercise yet",
                        fontSize = 14.sp, color = TextGray,
                        fontWeight = FontWeight.Medium
                    )
                    Text("Complete a workout to see insights!", fontSize = 12.sp, color = TextGray.copy(0.7f))
                }
            }
        } else {
            // Score trend chart
            SectionHeader("Score Trend")
            Spacer(Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = LightCard),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ProgressChart(
                    data = exerciseScores,
                    modifier = Modifier.fillMaxWidth().height(170.dp).padding(8.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            // Personal record
            if (pr != null) {
                SectionHeader("Personal Record")
                Spacer(Modifier.height(8.dp))
                PersonalRecordCard(pr = pr!!)
                Spacer(Modifier.height(16.dp))
            }

            // 5-metric radar
            if (exerciseMetrics.isNotEmpty()) {
                SectionHeader("Average Metrics")
                Spacer(Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = LightCard),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        RadarChart(
                            metrics = exerciseMetrics,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        // Metric values list
                        exerciseMetrics.forEach { (label, value) ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(label, fontSize = 13.sp, color = TextGray)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    LinearProgressIndicator(
                                        progress = { (value / 100f).coerceIn(0f, 1f) },
                                        modifier = Modifier.width(100.dp).height(6.dp),
                                        color = when {
                                            value >= 80f -> GreenSuccess
                                            value >= 60f -> CyanPrimary
                                            else -> OrangeWarning
                                        },
                                        trackColor = LightBorder
                                    )
                                    Text(
                                        "${value.roundToInt()}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            value >= 80f -> GreenSuccess
                                            value >= 60f -> CyanPrimary
                                            else -> OrangeWarning
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}

// ── Tab 3: Muscles ─────────────────────────────────────────────────────────────

@Composable
private fun MusclesTab(viewModel: ProgressViewModel) {
    val muscleVolume by viewModel.muscleGroupVolume.collectAsStateWithLifecycle()
    val personalRecords by viewModel.personalRecords.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        // Muscle group volume chart
        SectionHeader("Muscle Volume (Total Reps)")
        Spacer(Modifier.height(4.dp))
        Text("How hard each muscle has been trained", fontSize = 12.sp, color = TextGray)
        Spacer(Modifier.height(10.dp))

        if (muscleVolume.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = LightCard),
                shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()
            ) {
                Box(Modifier.padding(32.dp).fillMaxWidth(), Alignment.Center) {
                    Text("Complete workouts to see muscle breakdown", fontSize = 13.sp, color = TextGray)
                }
            }
        } else {
            Card(
                colors = CardDefaults.cardColors(containerColor = LightCard),
                shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()
            ) {
                MuscleGroupChart(
                    data = muscleVolume,
                    modifier = Modifier.padding(16.dp).fillMaxWidth()
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // Personal records per exercise
        if (personalRecords.isNotEmpty()) {
            SectionHeader("Personal Records")
            Spacer(Modifier.height(8.dp))
            personalRecords.values
                .sortedByDescending { it.bestScore }
                .forEach { pr ->
                    PersonalRecordCard(pr = pr)
                    Spacer(Modifier.height(8.dp))
                }
        }

        Spacer(Modifier.height(80.dp))
    }
}

// ── Shared sub-composables ─────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark)
}

@Composable
private fun PersonalRecordCard(pr: PersonalRecord) {
    val exercise = ExerciseLibrary.getByName(pr.exerciseName)
    Card(
        colors = CardDefaults.cardColors(containerColor = LightCard),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(CyanPrimary.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(exercise?.emoji ?: "🏋️", fontSize = 20.sp)
                }
                Column {
                    Text(pr.exerciseName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    Text("${pr.totalSessions} sessions", fontSize = 11.sp, color = TextGray)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        Icon(Icons.Default.EmojiEvents, null, tint = ScoreGold, modifier = Modifier.size(14.dp))
                        Text(
                            "${pr.bestScore.toInt()}",
                            fontSize = 18.sp, fontWeight = FontWeight.ExtraBold,
                            color = when {
                                pr.bestScore >= 80f -> GreenSuccess
                                pr.bestScore >= 60f -> CyanPrimary
                                else -> OrangeWarning
                            }
                        )
                    }
                    Text("Best Score", fontSize = 10.sp, color = TextGray)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${pr.bestReps}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = TextDark)
                    Text("Best Reps", fontSize = 10.sp, color = TextGray)
                }
            }
        }
    }
}

@Composable
private fun ProgressSessionCard(session: WorkoutSession) {
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    val exercise = ExerciseLibrary.getByName(session.exerciseType)
    Card(
        colors = CardDefaults.cardColors(containerColor = LightCard),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(exercise?.emoji ?: "🏋️", fontSize = 20.sp)
                    Column {
                        Text(session.exerciseType, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark)
                        Text(dateFormat.format(Date(session.timestamp)), fontSize = 11.sp, color = TextGray)
                    }
                }
                Text(
                    "${session.totalScore.toInt()}",
                    fontSize = 26.sp, fontWeight = FontWeight.ExtraBold,
                    color = when {
                        session.totalScore >= 80f -> GreenSuccess
                        session.totalScore >= 60f -> CyanPrimary
                        else -> RedDanger
                    }
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SmallStat("Reps", "${session.repCount}")
                SmallStat("Duration", "${session.durationSeconds}s")
                SmallStat("Accuracy", "${session.accuracyScore.toInt()}")
                SmallStat("Balance", "${session.balanceScore.toInt()}")
                SmallStat("Stability", "${session.stabilityScore.toInt()}")
            }
        }
    }
}

@Composable
private fun SmallStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyanPrimary)
        Text(label, fontSize = 10.sp, color = TextGray)
    }
}

@Composable
private fun EmptyProgressState() {
    Box(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("No sessions yet", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextGray)
            Spacer(Modifier.height(8.dp))
            Text(
                "Complete your first workout to see progress insights!",
                fontSize = 14.sp, color = TextGray.copy(alpha = 0.7f)
            )
        }
    }
}
