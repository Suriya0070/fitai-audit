package com.jsf.app.pravin.ui.screens.challenge

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jsf.app.pravin.data.remote.RepBattle
import com.jsf.app.pravin.data.remote.RepBattleEntry
import com.jsf.app.pravin.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepBattleScreen(
    serverId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: RepBattleViewModel = viewModel(
        factory = RepBattleViewModel.factory(context.applicationContext as android.app.Application, serverId)
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.snackMessage) {
        state.snackMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnack()
        }
    }

    Scaffold(
        containerColor = LightBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text("Rep Battles", fontWeight = FontWeight.ExtraBold, color = TextDark, fontSize = 20.sp)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = TextDark)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.showCreateSheet() }) {
                        Icon(Icons.Default.Add, "Create Battle", tint = CyanPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LightCard)
            )
        }
    ) { padding ->
        if (state.battles.isEmpty()) {
            NoBattlesPlaceholder(
                modifier = Modifier.fillMaxSize().padding(padding),
                onCreate = { viewModel.showCreateSheet() }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Battle selector chips
                if (state.battles.size > 1) {
                    androidx.compose.foundation.lazy.LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.battles.size) { idx ->
                            val battle = state.battles[idx]
                            val selected = state.activeBattle?.id == battle.id
                            FilterChip(
                                selected = selected,
                                onClick = { viewModel.selectBattle(battle) },
                                label = {
                                    Text(
                                        "${battle.exercise} Battle",
                                        fontSize = 13.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyanPrimary,
                                    selectedLabelColor = DeepSpace
                                )
                            )
                        }
                    }
                }

                state.activeBattle?.let { battle ->
                    BattleContent(
                        battle = battle,
                        leaderboard = state.leaderboard,
                        myEntry = state.myEntry,
                        currentUserId = state.currentUserId,
                        onSubmitSet = { set, reps -> viewModel.submitSet(set, reps) }
                    )
                }
            }
        }

        if (state.showCreateSheet) {
            CreateBattleSheet(
                exercises = viewModel.popularExercises.map { it.name },
                selectedExercise = state.selectedExercise,
                selectedSets = state.selectedSets,
                selectedDurationHours = state.selectedDurationHours,
                isCreating = state.isCreating,
                onExerciseChange = { viewModel.setExercise(it) },
                onSetsChange = { viewModel.setSets(it) },
                onDurationChange = { viewModel.setDuration(it) },
                onCreate = { viewModel.createBattle() },
                onDismiss = { viewModel.hideCreateSheet() }
            )
        }
    }
}

@Composable
private fun BattleContent(
    battle: RepBattle,
    leaderboard: List<RepBattleEntry>,
    myEntry: RepBattleEntry?,
    currentUserId: String,
    onSubmitSet: (Int, Int) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Battle header card
        item {
            BattleHeaderCard(battle = battle, participantCount = leaderboard.size)
        }

        // My submission card
        item {
            MySubmissionCard(
                myEntry = myEntry,
                setsCount = battle.setsCount,
                exercise = battle.exercise,
                onSubmitSet = onSubmitSet
            )
        }

        // Leaderboard header
        item {
            Text(
                "Leaderboard",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
        }

        if (leaderboard.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No entries yet. Be the first!", fontSize = 14.sp, color = TextGray)
                }
            }
        } else {
            itemsIndexed(leaderboard) { index, entry ->
                LeaderboardRow(
                    rank = index + 1,
                    entry = entry,
                    isMe = entry.userId == currentUserId
                )
            }
        }
    }
}

@Composable
private fun BattleHeaderCard(battle: RepBattle, participantCount: Int) {
    val timeLeft = remember(battle.endsAt) {
        val remaining = battle.endsAt - System.currentTimeMillis()
        when {
            remaining <= 0 -> "Ended"
            remaining < 3_600_000 -> "${remaining / 60_000}m left"
            remaining < 86_400_000 -> "${remaining / 3_600_000}h left"
            else -> "${remaining / 86_400_000}d left"
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CyanPrimary.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.EmojiEvents, null, tint = ScoreGold, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "${battle.exercise} Battle",
                    fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = TextDark
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                InfoChip(icon = Icons.Default.FitnessCenter, label = "${battle.setsCount} sets")
                InfoChip(icon = Icons.Default.Group, label = "$participantCount joined")
                InfoChip(icon = Icons.Default.Timer, label = timeLeft)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Created by ${battle.creatorName}",
                fontSize = 12.sp, color = TextGray
            )
        }
    }
}

@Composable
private fun InfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(LightCard)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(icon, null, tint = CyanPrimary, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun MySubmissionCard(
    myEntry: RepBattleEntry?,
    setsCount: Int,
    exercise: String,
    onSubmitSet: (Int, Int) -> Unit
) {
    var setRepsInput by remember { mutableStateOf("") }
    var activeSet by remember { mutableIntStateOf((myEntry?.setsCompleted ?: 0) + 1) }

    LaunchedEffect(myEntry) {
        activeSet = (myEntry?.setsCompleted ?: 0) + 1
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LightCard),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("My Performance", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextDark)
            Spacer(Modifier.height(10.dp))

            // Set boxes
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (i in 1..setsCount) {
                    val reps = when (i) {
                        1 -> myEntry?.set1 ?: 0
                        2 -> myEntry?.set2 ?: 0
                        else -> myEntry?.set3 ?: 0
                    }
                    val completed = (myEntry?.setsCompleted ?: 0) >= i
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (completed) CyanPrimary.copy(0.15f)
                                else if (activeSet == i) NeonGreen.copy(0.10f)
                                else LightBg
                            )
                            .border(
                                1.5.dp,
                                if (completed) CyanPrimary
                                else if (activeSet == i) NeonGreen
                                else LightBorder,
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Set $i", fontSize = 11.sp, color = TextGray)
                            Text(
                                if (completed) "$reps" else "-",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (completed) CyanPrimary else TextSecondary
                            )
                            if (completed) Text("reps", fontSize = 10.sp, color = TextGray)
                        }
                    }
                }
            }

            if (activeSet <= setsCount) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Enter your reps for Set $activeSet",
                    fontSize = 13.sp, color = TextSecondary
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = setRepsInput,
                        onValueChange = { if (it.length <= 3 && it.all { c -> c.isDigit() }) setRepsInput = it },
                        placeholder = { Text("Reps", color = TextGray) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanPrimary,
                            unfocusedBorderColor = LightBorder,
                            focusedTextColor = TextDark,
                            unfocusedTextColor = TextDark,
                            cursorColor = CyanPrimary,
                            focusedContainerColor = LightCard,
                            unfocusedContainerColor = LightCard
                        ),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            val reps = setRepsInput.toIntOrNull() ?: return@Button
                            if (reps > 0) {
                                onSubmitSet(activeSet, reps)
                                setRepsInput = ""
                            }
                        },
                        enabled = setRepsInput.toIntOrNull()?.let { it > 0 } == true,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = DeepSpace),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Submit", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Spacer(Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = GreenSuccess.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = GreenSuccess, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "All $setsCount sets done! Total: ${myEntry?.totalReps ?: 0} reps",
                            color = GreenSuccess, fontWeight = FontWeight.Bold, fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LeaderboardRow(rank: Int, entry: RepBattleEntry, isMe: Boolean) {
    val rankColor = when (rank) {
        1 -> Color(0xFFFFD700)
        2 -> Color(0xFFC0C0C0)
        3 -> Color(0xFFCD7F32)
        else -> TextGray
    }
    val rankIcon = when (rank) {
        1 -> "🥇"; 2 -> "🥈"; 3 -> "🥉"; else -> "$rank"
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isMe) CyanPrimary.copy(alpha = 0.08f) else LightCard
        ),
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isMe) Modifier.border(1.dp, CyanPrimary.copy(0.3f), RoundedCornerShape(12.dp))
                else Modifier
            )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(36.dp),
                contentAlignment = Alignment.Center
            ) {
                if (rank <= 3) {
                    Text(rankIcon, fontSize = 22.sp)
                } else {
                    Text(rankIcon, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextGray)
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        entry.userName.ifEmpty { "Unknown" },
                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        color = if (isMe) CyanPrimary else TextDark
                    )
                    if (isMe) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = CyanPrimary.copy(0.15f)
                        ) {
                            Text(
                                "You",
                                fontSize = 10.sp, color = CyanPrimary, fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Text(
                    "Sets: ${entry.set1}+${entry.set2}+${entry.set3}",
                    fontSize = 11.sp, color = TextGray
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${entry.totalReps}",
                    fontSize = 22.sp, fontWeight = FontWeight.ExtraBold,
                    color = if (rank == 1) ScoreGold else TextDark
                )
                Text("total reps", fontSize = 10.sp, color = TextGray)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateBattleSheet(
    exercises: List<String>,
    selectedExercise: String,
    selectedSets: Int,
    selectedDurationHours: Int,
    isCreating: Boolean,
    onExerciseChange: (String) -> Unit,
    onSetsChange: (Int) -> Unit,
    onDurationChange: (Int) -> Unit,
    onCreate: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = LightCard
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .navigationBarsPadding()
        ) {
            Text("Create Rep Battle", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = TextDark)
            Spacer(Modifier.height(16.dp))

            Text("Exercise", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(exercises.size) { i ->
                    val ex = exercises[i]
                    FilterChip(
                        selected = selectedExercise == ex,
                        onClick = { onExerciseChange(ex) },
                        label = { Text(ex, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyanPrimary,
                            selectedLabelColor = DeepSpace
                        )
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Number of Sets", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1, 2, 3, 4, 5).forEach { n ->
                    FilterChip(
                        selected = selectedSets == n,
                        onClick = { onSetsChange(n) },
                        label = { Text("$n", fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyanPrimary,
                            selectedLabelColor = DeepSpace
                        )
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Duration", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1 to "1h", 6 to "6h", 24 to "24h", 72 to "3 days").forEach { (h, label) ->
                    FilterChip(
                        selected = selectedDurationHours == h,
                        onClick = { onDurationChange(h) },
                        label = { Text(label, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyanPrimary,
                            selectedLabelColor = DeepSpace
                        )
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onCreate,
                enabled = !isCreating,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = DeepSpace),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isCreating) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = DeepSpace)
                } else {
                    Icon(Icons.Default.EmojiEvents, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Start Battle!", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun NoBattlesPlaceholder(modifier: Modifier, onCreate: () -> Unit) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Text("🏆", fontSize = 64.sp)
            Spacer(Modifier.height(16.dp))
            Text("No Active Battles", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = TextDark)
            Spacer(Modifier.height(8.dp))
            Text(
                "Challenge your friends! Create a rep battle\nand see who gets the most reps in 3 sets.",
                fontSize = 14.sp, color = TextGray, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onCreate,
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = DeepSpace),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.EmojiEvents, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Create a Battle", fontWeight = FontWeight.Bold)
            }
        }
    }
}
