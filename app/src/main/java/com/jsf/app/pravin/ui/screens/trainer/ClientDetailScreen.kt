package com.jsf.app.pravin.ui.screens.trainer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import com.jsf.app.pravin.data.remote.ClientStatusSnapshot
import com.jsf.app.pravin.data.remote.TrainerAlert
import com.jsf.app.pravin.data.remote.TrainerNote
import com.jsf.app.pravin.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDetailScreen(
    clientId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val vm: ClientDetailViewModel = viewModel(
        factory = ClientDetailViewModel.Factory(context.applicationContext as android.app.Application, clientId)
    )
    val snapshot by vm.statusSnapshot.collectAsStateWithLifecycle()
    val alerts by vm.alerts.collectAsStateWithLifecycle()
    val notes by vm.notes.collectAsStateWithLifecycle()
    val sessions by vm.workoutSessions.collectAsStateWithLifecycle()
    val noteSaved by vm.noteSaved.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Overview", "Workouts", "Alerts", "Notes")

    if (noteSaved) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(1500)
            vm.clearNoteSaved()
        }
    }

    val clientName = snapshot?.clientName ?: "Client"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(clientName, fontWeight = FontWeight.Bold, color = TextDark) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextDark)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LightCard)
            )
        },
        containerColor = LightBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Client header
            if (snapshot != null) {
                ClientHeader(snapshot = snapshot!!)
            }

            // Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = LightCard,
                contentColor = CyanPrimary,
                edgePadding = 16.dp
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> OverviewTab(snapshot = snapshot)
                1 -> WorkoutsTab(sessions = sessions)
                2 -> AlertsTab(alerts = alerts, onMarkRead = { vm.markAlertRead(it) })
                3 -> NotesTab(notes = notes, onSaveNote = { vm.saveNote(it) }, noteSaved = noteSaved)
            }
        }
    }
}

@Composable
private fun ClientHeader(snapshot: ClientStatusSnapshot) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(LightCard)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(CyanPrimary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                snapshot.clientName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = CyanPrimary
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(snapshot.clientName, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDark)
            Text("${snapshot.sessionCount} sessions total", fontSize = 13.sp, color = TextGray)
            if (snapshot.lastWorkoutAt > 0) {
                val days = ((System.currentTimeMillis() - snapshot.lastWorkoutAt) / (1000 * 60 * 60 * 24)).toInt()
                Text(
                    if (days == 0) "Worked out today" else "$days days since last workout",
                    fontSize = 12.sp,
                    color = if (days >= 7) OrangeWarning else TextGray
                )
            }
        }
        // Dropout risk indicator
        if (snapshot.dropoutRisk > 0) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val riskColor = when {
                    snapshot.dropoutRisk > 0.6f -> RedDanger
                    snapshot.dropoutRisk > 0.3f -> OrangeWarning
                    else -> GreenSuccess
                }
                Text(
                    "${(snapshot.dropoutRisk * 100).toInt()}%",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = riskColor
                )
                Text("risk", fontSize = 11.sp, color = TextGray)
            }
        }
    }
}

@Composable
private fun OverviewTab(snapshot: ClientStatusSnapshot?) {
    if (snapshot == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = CyanPrimary)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Performance Dimensions", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = TextDark)

        ScoreDimension("Overall Score", snapshot.overallScore)
        ScoreDimension("Form Quality", snapshot.formScore)
        ScoreDimension("Workout Frequency", snapshot.workoutFrequency)
        ScoreDimension("Consistency", snapshot.consistency)
        ScoreDimension("Engagement", snapshot.engagement)

        Spacer(Modifier.height(8.dp))
        Text("Dropout Risk", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = TextDark)

        val riskColor = when {
            snapshot.dropoutRisk > 0.6f -> RedDanger
            snapshot.dropoutRisk > 0.3f -> OrangeWarning
            else -> GreenSuccess
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = riskColor.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        when {
                            snapshot.dropoutRisk > 0.6f -> "High Risk"
                            snapshot.dropoutRisk > 0.3f -> "Moderate Risk"
                            else -> "Low Risk"
                        },
                        fontWeight = FontWeight.Bold,
                        color = riskColor,
                        fontSize = 16.sp
                    )
                    Text(
                        when {
                            snapshot.dropoutRisk > 0.6f -> "Immediate intervention recommended"
                            snapshot.dropoutRisk > 0.3f -> "Monitor closely"
                            else -> "Client is on track"
                        },
                        fontSize = 13.sp,
                        color = TextGray
                    )
                }
                Text(
                    "${(snapshot.dropoutRisk * 100).toInt()}%",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = riskColor
                )
            }
        }
    }
}

@Composable
private fun ScoreDimension(label: String, score: Float) {
    val normalized = (score / 100f).coerceIn(0f, 1f)
    val color = when {
        score >= 75 -> GreenSuccess
        score >= 50 -> OrangeWarning
        else -> RedDanger
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(label, modifier = Modifier.width(140.dp), fontSize = 14.sp, color = TextDark)
        LinearProgressIndicator(
            progress = { normalized },
            modifier = Modifier.weight(1f).height(8.dp),
            color = color,
            trackColor = LightBorder
        )
        Text("${score.toInt()}", modifier = Modifier.width(36.dp), fontSize = 13.sp, color = color, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
    }
}

@Composable
private fun WorkoutsTab(sessions: List<Map<String, Any>>) {
    if (sessions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = TextGray, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(12.dp))
                Text("No workouts recorded", fontSize = 16.sp, color = TextGray)
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(sessions) { session ->
            WorkoutSessionCard(session)
        }
    }
}

@Composable
private fun WorkoutSessionCard(session: Map<String, Any>) {
    val exercise = session["exerciseType"] as? String ?: "Unknown"
    val reps = (session["repCount"] as? Long)?.toInt() ?: 0
    val score = ((session["totalScore"] as? Double) ?: 0.0).toFloat()
    val duration = (session["durationSeconds"] as? Long)?.toInt() ?: 0
    val timestamp = session["timestamp"] as? Long ?: 0L

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = LightCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(exercise, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = TextDark)
                Text(
                    SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(timestamp)),
                    fontSize = 12.sp,
                    color = TextGray
                )
                Text("$reps reps · ${duration / 60}m ${duration % 60}s", fontSize = 12.sp, color = TextGray)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val scoreColor = when {
                    score >= 75 -> GreenSuccess
                    score >= 50 -> OrangeWarning
                    else -> RedDanger
                }
                Text("${score.toInt()}", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = scoreColor)
                Text("score", fontSize = 10.sp, color = TextGray)
            }
        }
    }
}

@Composable
private fun AlertsTab(
    alerts: List<TrainerAlert>,
    onMarkRead: (String) -> Unit
) {
    if (alerts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No alerts for this client", fontSize = 16.sp, color = TextGray)
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(alerts, key = { it.id }) { alert ->
            AlertItem(alert = alert, onMarkRead = { onMarkRead(alert.id) })
        }
    }
}

@Composable
private fun NotesTab(
    notes: List<TrainerNote>,
    onSaveNote: (String) -> Unit,
    noteSaved: Boolean
) {
    var noteText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Add Note", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = TextDark)

        OutlinedTextField(
            value = noteText,
            onValueChange = { noteText = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Write a note about this client...", color = TextGray.copy(alpha = 0.5f)) },
            minLines = 3,
            maxLines = 6,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyanPrimary,
                unfocusedBorderColor = LightBorder,
                focusedTextColor = TextDark,
                unfocusedTextColor = TextDark,
                cursorColor = CyanPrimary,
                focusedContainerColor = LightCard,
                unfocusedContainerColor = LightCard
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (noteSaved) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GreenSuccess, modifier = Modifier.size(20.dp))
                Text("Note saved!", color = GreenSuccess, fontSize = 13.sp)
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    onSaveNote(noteText)
                    noteText = ""
                },
                enabled = noteText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = DeepSpace),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save Note")
            }
        }

        if (notes.isNotEmpty()) {
            HorizontalDivider(color = LightBorder)
            Text("Previous Notes", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextGray)
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(notes, key = { it.id }) { note ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = LightCard),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(note.content, fontSize = 14.sp, color = TextDark)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                                    .format(Date(note.updatedAt)),
                                fontSize = 11.sp,
                                color = TextGray
                            )
                        }
                    }
                }
            }
        }
    }
}
