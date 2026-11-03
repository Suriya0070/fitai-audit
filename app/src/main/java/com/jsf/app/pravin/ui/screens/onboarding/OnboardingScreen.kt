package com.jsf.app.pravin.ui.screens.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jsf.app.pravin.data.model.UserProfile
import com.jsf.app.pravin.ui.theme.*

data class OnboardingGoals(
    val fitnessGoal: String,
    val weeklyGoalSessions: Int
)

@Composable
fun OnboardingScreen(
    uid: String = "",
    onComplete: (UserProfile, OnboardingGoals) -> Unit
) {
    var page by remember { mutableIntStateOf(0) }

    // Page 0 state
    var role by remember { mutableStateOf("athlete") }

    // Page 1 state
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("25") }
    var heightCm by remember { mutableStateOf("170") }
    var weightKg by remember { mutableStateOf("70") }
    var fitnessLevel by remember { mutableStateOf("Beginner") }
    var gender by remember { mutableStateOf("Other") }
    var errorMessage by remember { mutableStateOf("") }

    // Page 2 state
    var fitnessGoal by remember { mutableStateOf("Stay Active") }
    var weeklyGoal by remember { mutableIntStateOf(3) }

    when (page) {
        0 -> RolePage(
            selectedRole = role,
            onRoleChange = { role = it },
            onNext = { page = 1 }
        )
        1 -> ProfilePage(
            name = name, onNameChange = { name = it },
            age = age, onAgeChange = { age = it },
            heightCm = heightCm, onHeightChange = { heightCm = it },
            weightKg = weightKg, onWeightChange = { weightKg = it },
            fitnessLevel = fitnessLevel, onLevelChange = { fitnessLevel = it },
            gender = gender, onGenderChange = { gender = it },
            errorMessage = errorMessage,
            onBack = { page = 0 },
            onNext = {
                val ageInt = age.toIntOrNull()
                val heightFloat = heightCm.toFloatOrNull()
                val weightFloat = weightKg.toFloatOrNull()
                when {
                    name.isBlank() -> errorMessage = "Please enter your name"
                    ageInt == null || ageInt < 10 || ageInt > 100 ->
                        errorMessage = "Please enter a valid age (10–100)"
                    heightFloat == null || heightFloat < 100f || heightFloat > 250f ->
                        errorMessage = "Please enter a valid height (100–250 cm)"
                    weightFloat == null || weightFloat < 30f || weightFloat > 300f ->
                        errorMessage = "Please enter a valid weight (30–300 kg)"
                    else -> {
                        errorMessage = ""
                        page = 2
                    }
                }
            }
        )
        2 -> GoalsPage(
            fitnessGoal = fitnessGoal, onGoalChange = { fitnessGoal = it },
            weeklyGoal = weeklyGoal, onWeeklyGoalChange = { weeklyGoal = it },
            onBack = { page = 1 },
            onDone = {
                onComplete(
                    UserProfile(
                        uid = uid,
                        name = name.trim(),
                        age = age.toIntOrNull() ?: 25,
                        heightCm = heightCm.toFloatOrNull() ?: 170f,
                        weightKg = weightKg.toFloatOrNull() ?: 70f,
                        fitnessLevel = fitnessLevel,
                        gender = gender,
                        role = role
                    ),
                    OnboardingGoals(fitnessGoal, weeklyGoal)
                )
            }
        )
    }
}

@Composable
private fun RolePage(
    selectedRole: String,
    onRoleChange: (String) -> Unit,
    onNext: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("FitAI", fontSize = 42.sp, fontWeight = FontWeight.ExtraBold, color = CyanPrimary)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { 0.33f },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = CyanPrimary,
                trackColor = LightBorder
            )
            Text("Step 1 of 3 — Your Role", fontSize = 11.sp, color = TextGray)

            Spacer(Modifier.height(40.dp))
            Text("How will you use FitAI?", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextDark, textAlign = TextAlign.Center)
            Text("Choose your role to personalize your experience", fontSize = 13.sp, color = TextGray, textAlign = TextAlign.Center)

            Spacer(Modifier.height(32.dp))

            RoleCard(
                emoji = "🏋️",
                title = "I am Training",
                subtitle = "Track my own workouts, get AI form feedback, monitor progress",
                selected = selectedRole == "athlete",
                onClick = { onRoleChange("athlete") }
            )

            Spacer(Modifier.height(16.dp))

            RoleCard(
                emoji = "🎓",
                title = "I am a Trainer",
                subtitle = "Manage clients, monitor their form & progress, send workout plans",
                selected = selectedRole == "trainer",
                onClick = { onRoleChange("trainer") }
            )

            Spacer(Modifier.height(40.dp))

            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = DeepSpace)
            ) {
                Text("Continue", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun RoleCard(
    emoji: String,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (selected) CyanPrimary else LightBorder,
        label = "roleCardBorder"
    )
    val bgColor by animateColorAsState(
        targetValue = if (selected) CyanPrimary.copy(alpha = 0.08f) else LightCard,
        label = "roleCardBg"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(emoji, fontSize = 36.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) CyanPrimary else TextDark
                )
                Spacer(Modifier.height(4.dp))
                Text(subtitle, fontSize = 13.sp, color = TextGray, lineHeight = 18.sp)
            }
            if (selected) {
                RadioButton(
                    selected = true,
                    onClick = null,
                    colors = RadioButtonDefaults.colors(selectedColor = CyanPrimary)
                )
            }
        }
    }
}

@Composable
private fun ProfilePage(
    name: String, onNameChange: (String) -> Unit,
    age: String, onAgeChange: (String) -> Unit,
    heightCm: String, onHeightChange: (String) -> Unit,
    weightKg: String, onWeightChange: (String) -> Unit,
    fitnessLevel: String, onLevelChange: (String) -> Unit,
    gender: String, onGenderChange: (String) -> Unit,
    errorMessage: String,
    onBack: () -> Unit = {},
    onNext: () -> Unit
) {
    val fitnessLevels = listOf("Beginner", "Intermediate", "Advanced")
    val genders = listOf("Male", "Female", "Other")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(40.dp))

            Text("FitAI", fontSize = 42.sp, fontWeight = FontWeight.ExtraBold, color = CyanPrimary)
            Text(
                "AI-Powered Workout Intelligence",
                fontSize = 14.sp,
                color = TextGray,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { 0.66f },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = CyanPrimary,
                trackColor = LightBorder
            )
            Text("Step 2 of 3 — Your Profile", fontSize = 11.sp, color = TextGray)

            Spacer(Modifier.height(24.dp))
            Text("Let's set up your profile", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextDark)
            Text(
                "We'll personalize your AI coaching experience",
                fontSize = 13.sp,
                color = TextGray,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))

            OnboardingTextField(name, onNameChange, "Your Name", "Enter your name")
            Spacer(Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OnboardingTextField(age, onAgeChange, "Age", "25", KeyboardType.Number, Modifier.weight(1f))
                OnboardingTextField(heightCm, onHeightChange, "Height (cm)", "170", KeyboardType.Number, Modifier.weight(1f))
            }
            Spacer(Modifier.height(16.dp))

            OnboardingTextField(weightKg, onWeightChange, "Weight (kg)", "70", KeyboardType.Number)
            Spacer(Modifier.height(20.dp))

            Text("Fitness Level", fontSize = 14.sp, color = TextGray, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                fitnessLevels.forEach { level ->
                    val selected = fitnessLevel == level
                    Button(
                        onClick = { onLevelChange(level) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selected) CyanPrimary else LightSurface,
                            contentColor = if (selected) TextDark else TextGray
                        )
                    ) {
                        Text(level, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Gender", fontSize = 14.sp, color = TextGray, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                genders.forEach { g ->
                    val selected = gender == g
                    Button(
                        onClick = { onGenderChange(g) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selected) GreenSuccess else LightSurface,
                            contentColor = if (selected) TextDark else TextGray
                        )
                    ) {
                        Text(g, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }

            if (errorMessage.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(errorMessage, color = RedDanger, fontSize = 13.sp, textAlign = TextAlign.Center)
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = DeepSpace)
            ) {
                Text("Next — Set Your Goals", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onBack) {
                Text("Back", color = TextGray, fontSize = 14.sp)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun GoalsPage(
    fitnessGoal: String, onGoalChange: (String) -> Unit,
    weeklyGoal: Int, onWeeklyGoalChange: (Int) -> Unit,
    onBack: () -> Unit,
    onDone: () -> Unit
) {
    val goals = listOf(
        "Lose Weight" to "🔥",
        "Build Muscle" to "💪",
        "Improve Endurance" to "🏃",
        "Stay Active" to "🧘"
    )
    val weeklyOptions = listOf(2, 3, 4, 5)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(40.dp))

            Text("FitAI", fontSize = 42.sp, fontWeight = FontWeight.ExtraBold, color = CyanPrimary)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { 1f },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = CyanPrimary,
                trackColor = LightBorder
            )
            Text("Step 3 of 3 — Your Goals", fontSize = 11.sp, color = TextGray)

            Spacer(Modifier.height(32.dp))
            Text("What's your main goal?", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextDark)
            Text("This helps us tailor your AI coaching", fontSize = 13.sp, color = TextGray)
            Spacer(Modifier.height(20.dp))

            goals.forEach { (goal, emoji) ->
                val selected = fitnessGoal == goal
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selected) CyanPrimary.copy(alpha = 0.12f) else LightCard
                    ),
                    shape = RoundedCornerShape(14.dp),
                    onClick = { onGoalChange(goal) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(emoji, fontSize = 28.sp)
                            Text(
                                goal,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (selected) CyanPrimary else TextDark
                            )
                        }
                        if (selected) {
                            RadioButton(
                                selected = true,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = CyanPrimary)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
            Text("Weekly workout target", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark)
            Text("How many sessions per week?", fontSize = 13.sp, color = TextGray)
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                weeklyOptions.forEach { n ->
                    val selected = weeklyGoal == n
                    Button(
                        onClick = { onWeeklyGoalChange(n) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selected) CyanPrimary else LightSurface,
                            contentColor = if (selected) TextDark else TextGray
                        )
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$n", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                            Text("days", fontSize = 10.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = DeepSpace)
            ) {
                Text("Let's Go!", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            }

            Spacer(Modifier.height(12.dp))

            TextButton(onClick = onBack) {
                Text("Back", color = TextGray, fontSize = 14.sp)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun OnboardingTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TextGray) },
        placeholder = { Text(placeholder, color = TextGray.copy(alpha = 0.5f)) },
        modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CyanPrimary,
            unfocusedBorderColor = LightBorder,
            focusedTextColor = TextDark,
            unfocusedTextColor = TextDark,
            cursorColor = CyanPrimary,
            focusedContainerColor = LightCard,
            unfocusedContainerColor = LightCard
        ),
        shape = RoundedCornerShape(12.dp),
        singleLine = true
    )
}
