package com.jsf.app.pravin.ui.screens.avatar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jsf.app.pravin.domain.ConsistencyTracker
import com.jsf.app.pravin.ui.components.AvatarCanvas
import com.jsf.app.pravin.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AvatarScreen(
    viewModel: AvatarViewModel = viewModel()
) {
    val streak by viewModel.streak.collectAsState()
    val avatarLevel by viewModel.avatarLevel.collectAsState()
    val motivationalMessage by viewModel.motivationalMessage.collectAsState()
    val weeklyActivity by viewModel.weeklyActivity.collectAsState()

    val tracker = remember { ConsistencyTracker() }
    val avatarTitle = tracker.getAvatarTitle(avatarLevel)

    val levelColor = when (avatarLevel) {
        0 -> TextSecondary
        1 -> CyanPrimary
        2 -> NeonGreen
        3 -> OrangeWarning
        4 -> ScoreGold
        else -> TextSecondary
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DeepSpace, DarkNavy)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))
            Text("My Avatar", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
            Text("Evolve through consistent training", fontSize = 14.sp, color = TextSecondary)

            Spacer(Modifier.height(24.dp))

            // Level badge
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = levelColor.copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .border(2.dp, levelColor, RoundedCornerShape(24.dp))
            ) {
                Text(
                    text = "LEVEL $avatarLevel • $avatarTitle",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = levelColor,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            Spacer(Modifier.height(20.dp))

            // Large animated avatar
            AvatarCanvas(
                level = avatarLevel,
                size = 220.dp
            )

            Spacer(Modifier.height(20.dp))

            // Streak counter
            Card(
                colors = CardDefaults.cardColors(containerColor = CardDark),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocalFireDepartment,
                        contentDescription = "Streak",
                        tint = OrangeWarning,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "$streak",
                            fontSize = 42.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = OrangeWarning
                        )
                        Text("Day Streak", fontSize = 14.sp, color = TextSecondary)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Motivational message
            Card(
                colors = CardDefaults.cardColors(containerColor = CyanPrimary.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = motivationalMessage,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CyanPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }

            Spacer(Modifier.height(20.dp))

            // 7-day activity calendar
            Text("Last 7 Days", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.height(12.dp))

            val dayLabels = getLast7DayLabels()
            Card(
                colors = CardDefaults.cardColors(containerColor = CardDark),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    weeklyActivity.forEachIndexed { idx, active ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (active) NeonGreen else DeepSpace
                                    )
                                    .border(
                                        2.dp,
                                        if (active) NeonGreen else CardDark,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (active) {
                                    Text("✓", fontSize = 16.sp, color = DeepSpace, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                dayLabels.getOrElse(idx) { "" },
                                fontSize = 10.sp,
                                color = if (active) NeonGreen else TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Evolution guide
            Text("Evolution Path", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.height(12.dp))

            val levels = listOf(
                Triple(0, "Beginner", "Start your journey"),
                Triple(1, "Warrior", "1+ day streak"),
                Triple(2, "Athlete", "7+ day streak"),
                Triple(3, "Champion", "14+ day streak"),
                Triple(4, "Legend", "30+ day streak")
            )

            levels.forEach { (level, title, desc) ->
                val isUnlocked = avatarLevel >= level
                val isCurrent = avatarLevel == level
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            isCurrent -> levelColor.copy(alpha = 0.15f)
                            isUnlocked -> CardDark
                            else -> DeepSpace
                        }
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .then(
                            if (isCurrent) Modifier.border(
                                1.dp,
                                levelColor,
                                RoundedCornerShape(12.dp)
                            ) else Modifier
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val lc = when (level) {
                            0 -> TextSecondary
                            1 -> CyanPrimary
                            2 -> NeonGreen
                            3 -> OrangeWarning
                            4 -> ScoreGold
                            else -> TextSecondary
                        }
                        AvatarCanvas(
                            level = level,
                            size = 48.dp,
                            modifier = Modifier.size(48.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isUnlocked) lc else TextSecondary
                            )
                            Text(desc, fontSize = 12.sp, color = TextSecondary)
                        }
                        if (isCurrent) {
                            Text(
                                "CURRENT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = levelColor
                            )
                        } else if (!isUnlocked) {
                            Text(
                                "LOCKED",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        } else {
                            Text("✓", fontSize = 16.sp, color = NeonGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}

private fun getLast7DayLabels(): List<String> {
    val days = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val cal = Calendar.getInstance()
    return (6 downTo 0).map {
        val c = Calendar.getInstance()
        c.add(Calendar.DAY_OF_YEAR, -it)
        days[c.get(Calendar.DAY_OF_WEEK) - 1]
    }
}
