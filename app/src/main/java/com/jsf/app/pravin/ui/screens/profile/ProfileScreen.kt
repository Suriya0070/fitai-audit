package com.jsf.app.pravin.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jsf.app.pravin.data.model.UserProfile
import com.jsf.app.pravin.domain.ConsistencyTracker
import com.jsf.app.pravin.ui.components.AvatarCanvas
import com.jsf.app.pravin.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onSignOut: () -> Unit,
    onNavigateToFriends: () -> Unit = {},
    onNavigateToAchievements: () -> Unit = {},
    viewModel: ProfileViewModel = viewModel()
) {
    val profile by viewModel.userProfile.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val saveSuccess by viewModel.saveSuccess.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsStateWithLifecycle()

    var showEditSheet by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showDeletePasswordDialog by remember { mutableStateOf(false) }
    var deletePassword by remember { mutableStateOf("") }
    var deleteError by remember { mutableStateOf<String?>(null) }
    var isDeletingAccount by remember { mutableStateOf(false) }

    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            showEditSheet = false
            viewModel.clearSaveSuccess()
        }
    }

    val avatarLevel = ConsistencyTracker().getAvatarLevel(stats.streak)
    val avatarLabel = listOf("Beginner", "Warrior", "Athlete", "Champion", "Legend")
        .getOrElse(avatarLevel) { "Legend" }
    val levelColor = when (avatarLevel) {
        0 -> TextSecondary
        1 -> NeonGreen
        2 -> CyanPrimary
        3 -> ScoreGold
        else -> Color(0xFFFF6EFF)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))

            // ── Header ─────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Profile",
                    color = TextDark,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Row {
                    IconButton(onClick = onNavigateToAchievements) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = "Achievements", tint = ScoreGold)
                    }
                    IconButton(onClick = onNavigateToFriends) {
                        Icon(Icons.Default.Group, contentDescription = "Friends", tint = CyanPrimary)
                    }
                    IconButton(onClick = { showEditSheet = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = CyanPrimary)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Avatar ─────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .border(3.dp, levelColor, CircleShape)
                    .background(CardDark),
                contentAlignment = Alignment.Center
            ) {
                AvatarCanvas(level = avatarLevel, modifier = Modifier.size(100.dp))
            }

            Spacer(Modifier.height(12.dp))

            Text(
                profile?.name?.ifBlank { "Athlete" } ?: "Athlete",
                color = TextDark,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = levelColor.copy(alpha = 0.15f)
            ) {
                Text(
                    avatarLabel,
                    color = levelColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            // ── Stats Row ──────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard("Sessions", "${stats.totalSessions}", Icons.Default.FitnessCenter, Modifier.weight(1f))
                StatCard("Best Score", "${stats.bestScore.toInt()}", Icons.Default.Star, Modifier.weight(1f))
                StatCard("Streak", "${stats.streak}d", Icons.Default.LocalFireDepartment, Modifier.weight(1f))
                StatCard("Minutes", "${stats.totalMinutes}", Icons.Default.Timer, Modifier.weight(1f))
            }

            Spacer(Modifier.height(20.dp))

            // ── Personal Info ──────────────────────────────────────────────
            profile?.let { p ->
                InfoSection(title = "Personal Info") {
                    InfoRow(Icons.Default.Cake, "Age", "${p.age} years")
                    InfoRow(Icons.Default.Height, "Height", "${p.heightCm.toInt()} cm")
                    InfoRow(Icons.Default.MonitorWeight, "Weight", "${p.weightKg.toInt()} kg")
                    InfoRow(Icons.Default.Person, "Gender", p.gender)
                    InfoRow(Icons.Default.FitnessCenter, "Level", p.fitnessLevel)
                    if (p.bio.isNotBlank()) {
                        InfoRow(Icons.Default.Info, "Bio", p.bio)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Total Reps ─────────────────────────────────────────────────
            InfoSection(title = "Workout Summary") {
                InfoRow(Icons.Default.Repeat, "Total Reps", "${stats.totalReps}")
            }

            Spacer(Modifier.height(24.dp))

            // ── Sign Out ───────────────────────────────────────────────────
            OutlinedButton(
                onClick = { showSignOutDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = RedDanger),
                border = androidx.compose.foundation.BorderStroke(1.dp, RedDanger)
            ) {
                Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Sign Out", fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(8.dp))

            // ── Delete Account ─────────────────────────────────────────────
            TextButton(
                onClick = { showDeleteConfirmDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = RedDanger.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Delete Account",
                    color = RedDanger.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    // ── Edit Bottom Sheet ──────────────────────────────────────────────────
    if (showEditSheet && profile != null) {
        EditProfileSheet(
            profile = profile!!,
            onDismiss = { showEditSheet = false },
            onSave = { viewModel.saveProfile(it) }
        )
    }

    // ── Sign Out Confirm ───────────────────────────────────────────────────
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            containerColor = LightCard,
            title = { Text("Sign Out", color = TextDark) },
            text = { Text("Are you sure you want to sign out?", color = TextGray) },
            confirmButton = {
                TextButton(onClick = {
                    showSignOutDialog = false
                    viewModel.signOut(onDone = onSignOut)
                }) {
                    Text("Sign Out", color = RedDanger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel", color = CyanPrimary)
                }
            }
        )
    }

    // ── Delete Account — Step 1: Confirm ──────────────────────────────────
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = LightCard,
            title = { Text("Delete Account", color = RedDanger) },
            text = {
                Text(
                    "This will permanently delete your account and all your workout data. " +
                    "This cannot be undone.",
                    color = TextGray
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirmDialog = false
                    if (currentUserId.startsWith("local_")) {
                        // Guest users have no password — delete immediately
                        isDeletingAccount = true
                        viewModel.deleteAccount("") { success, error ->
                            isDeletingAccount = false
                            if (success) onSignOut() else deleteError = error
                        }
                    } else {
                        showDeletePasswordDialog = true
                    }
                }) {
                    Text("Continue", color = RedDanger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = CyanPrimary)
                }
            }
        )
    }

    // ── Delete Account — Step 2: Password Re-authentication ───────────────
    if (showDeletePasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeletePasswordDialog = false
                deletePassword = ""
                deleteError = null
            },
            containerColor = LightCard,
            title = { Text("Confirm Your Password", color = TextDark) },
            text = {
                Column {
                    Text(
                        "Enter your password to confirm account deletion.",
                        color = TextGray,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = deletePassword,
                        onValueChange = {
                            deletePassword = it
                            deleteError = null
                        },
                        label = { Text("Password", color = TextGray) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        isError = deleteError != null,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextDark,
                            unfocusedTextColor = TextDark,
                            focusedBorderColor = RedDanger,
                            unfocusedBorderColor = LightBorder,
                            cursorColor = RedDanger
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                    if (deleteError != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(deleteError!!, color = RedDanger, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (deletePassword.isBlank()) {
                            deleteError = "Please enter your password"
                            return@TextButton
                        }
                        isDeletingAccount = true
                        viewModel.deleteAccount(deletePassword) { success, error ->
                            isDeletingAccount = false
                            if (success) {
                                showDeletePasswordDialog = false
                                onSignOut()
                            } else {
                                deleteError = error
                            }
                        }
                    },
                    enabled = !isDeletingAccount
                ) {
                    if (isDeletingAccount) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = RedDanger
                        )
                    } else {
                        Text("Delete My Account", color = RedDanger, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeletePasswordDialog = false
                    deletePassword = ""
                    deleteError = null
                }) {
                    Text("Cancel", color = CyanPrimary)
                }
            }
        )
    }
}

@Composable
private fun StatCard(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = LightCard
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.height(4.dp))
            Text(value, color = TextDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(label, color = TextGray, fontSize = 10.sp)
        }
    }
}

@Composable
private fun InfoSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = LightCard
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = CyanPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = TextGray, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, color = TextGray, fontSize = 13.sp, modifier = Modifier.width(80.dp))
        Text(value, color = TextDark, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileSheet(
    profile: UserProfile,
    onDismiss: () -> Unit,
    onSave: (UserProfile) -> Unit
) {
    var name by remember { mutableStateOf(profile.name) }
    var age by remember { mutableStateOf(profile.age.toString()) }
    var heightCm by remember { mutableStateOf(profile.heightCm.toInt().toString()) }
    var weightKg by remember { mutableStateOf(profile.weightKg.toInt().toString()) }
    var bio by remember { mutableStateOf(profile.bio) }
    var gender by remember { mutableStateOf(profile.gender) }
    var fitnessLevel by remember { mutableStateOf(profile.fitnessLevel) }

    val genders = listOf("Male", "Female", "Other")
    val levels = listOf("Beginner", "Intermediate", "Advanced")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = LightSurface,
        dragHandle = {
            Box(
                Modifier
                    .padding(vertical = 8.dp)
                    .size(40.dp, 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(TextSecondary.copy(alpha = 0.4f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text("Edit Profile", color = TextDark, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))

            ProfileField("Name", name, onValueChange = { name = it })
            ProfileField("Age", age, onValueChange = { age = it }, keyboardType = KeyboardType.Number)
            ProfileField("Height (cm)", heightCm, onValueChange = { heightCm = it }, keyboardType = KeyboardType.Number)
            ProfileField("Weight (kg)", weightKg, onValueChange = { weightKg = it }, keyboardType = KeyboardType.Number)
            ProfileField("Bio", bio, onValueChange = { bio = it }, singleLine = false)

            Spacer(Modifier.height(8.dp))
            Text("Gender", color = TextGray, fontSize = 12.sp)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                genders.forEach { g ->
                    FilterChip(
                        selected = gender == g,
                        onClick = { gender = g },
                        label = { Text(g, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyanPrimary.copy(alpha = 0.2f),
                            selectedLabelColor = CyanPrimary,
                            containerColor = LightCard,
                            labelColor = TextSecondary
                        )
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Text("Fitness Level", color = TextGray, fontSize = 12.sp)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                levels.forEach { l ->
                    FilterChip(
                        selected = fitnessLevel == l,
                        onClick = { fitnessLevel = l },
                        label = { Text(l, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyanPrimary.copy(alpha = 0.2f),
                            selectedLabelColor = CyanPrimary,
                            containerColor = LightCard,
                            labelColor = TextSecondary
                        )
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    onSave(
                        profile.copy(
                            name = name.trim(),
                            age = age.toIntOrNull() ?: profile.age,
                            heightCm = heightCm.toFloatOrNull() ?: profile.heightCm,
                            weightKg = weightKg.toFloatOrNull() ?: profile.weightKg,
                            bio = bio.trim(),
                            gender = gender,
                            fitnessLevel = fitnessLevel
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
            ) {
                Text("Save Changes", color = DeepSpace, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ProfileField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TextGray) },
        singleLine = singleLine,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextDark,
            unfocusedTextColor = TextDark,
            focusedBorderColor = CyanPrimary,
            unfocusedBorderColor = LightBorder,
            cursorColor = CyanPrimary
        ),
        shape = RoundedCornerShape(10.dp)
    )
}
