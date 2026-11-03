package com.jsf.app.pravin.ui.screens.nutrition

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.jsf.app.pravin.data.model.NutritionLog
import com.jsf.app.pravin.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionScreen() {
    val vm: NutritionViewModel = viewModel()
    val logs by vm.logsForDay.collectAsStateWithLifecycle()
    val summary by vm.dailySummary.collectAsStateWithLifecycle()
    val selectedDay by vm.selectedDayStart.collectAsStateWithLifecycle()
    val isLogging by vm.isLogging.collectAsStateWithLifecycle()
    val analyzingAi by vm.analyzingAi.collectAsStateWithLifecycle()
    val logError by vm.logError.collectAsStateWithLifecycle()

    var showLogSheet by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> selectedImageUri = uri }

    logError?.let { error ->
        LaunchedEffect(error) {
            kotlinx.coroutines.delay(3000)
            vm.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nutrition", fontWeight = FontWeight.Bold, color = TextDark) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LightCard)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showLogSheet = true },
                containerColor = GreenSuccess,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Log Meal")
            }
        },
        containerColor = LightBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Date navigation
            DateNavigationRow(
                dayStart = selectedDay,
                onPrev = { vm.goToPreviousDay() },
                onNext = { vm.goToNextDay() }
            )

            // Daily macro summary
            MacroSummaryRow(summary = summary)

            // Error banner
            AnimatedVisibility(visible = logError != null, enter = fadeIn(), exit = fadeOut()) {
                logError?.let {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = RedDanger.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(it, modifier = Modifier.padding(12.dp), color = RedDanger, fontSize = 13.sp)
                    }
                }
            }

            // AI analyzing indicator
            AnimatedVisibility(visible = analyzingAi, enter = fadeIn(), exit = fadeOut()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = GreenSuccess)
                    Text("Analyzing meal with AI...", fontSize = 13.sp, color = GreenSuccess, fontWeight = FontWeight.Medium)
                }
            }

            if (logs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Restaurant, contentDescription = null, tint = TextGray, modifier = Modifier.size(64.dp))
                        Text("No meals logged", fontSize = 18.sp, color = TextGray, fontWeight = FontWeight.SemiBold)
                        Text("Tap + to log a meal", fontSize = 14.sp, color = TextGray)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(logs, key = { it.id }) { log ->
                        MealLogCard(log = log, onDelete = { vm.deleteLog(log) })
                    }
                }
            }
        }
    }

    if (showLogSheet) {
        LogMealBottomSheet(
            isLogging = isLogging,
            selectedUri = selectedImageUri,
            onPickPhoto = {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onLog = { mealType, description, manualCalories ->
                vm.logMeal(mealType, description, selectedImageUri, manualCalories)
                showLogSheet = false
                selectedImageUri = null
            },
            onDismiss = {
                showLogSheet = false
                selectedImageUri = null
            }
        )
    }
}

@Composable
private fun DateNavigationRow(dayStart: Long, onPrev: () -> Unit, onNext: () -> Unit) {
    val today = NutritionViewModel.todayMidnight()
    val isToday = dayStart == today
    val label = when {
        isToday -> "Today"
        dayStart == today - NutritionViewModel.DAY_MS -> "Yesterday"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(dayStart))
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(LightCard)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onPrev) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous day", tint = TextGray)
        }
        Text(label, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = TextDark)
        IconButton(onClick = onNext, enabled = !isToday) {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Next day",
                tint = if (!isToday) TextGray else TextGray.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
private fun MacroSummaryRow(summary: NutritionSummary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MacroCard("kcal", "${summary.totalCalories}", Color(0xFFEF4444), Modifier.weight(1f))
        MacroCard("protein", "${summary.totalProteinG.toInt()}g", Color(0xFF3B82F6), Modifier.weight(1f))
        MacroCard("carbs", "${summary.totalCarbsG.toInt()}g", Color(0xFFF59E0B), Modifier.weight(1f))
        MacroCard("fat", "${summary.totalFatG.toInt()}g", Color(0xFF8B5CF6), Modifier.weight(1f))
    }
}

@Composable
private fun MacroCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = color)
            Text(label, fontSize = 10.sp, color = TextGray)
        }
    }
}

@Composable
private fun MealLogCard(log: NutritionLog, onDelete: () -> Unit) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = LightCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Meal photo or type icon
            if (log.imageUrl.isNotEmpty()) {
                AsyncImage(
                    model = log.imageUrl,
                    contentDescription = "Meal photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(72.dp).clip(RoundedCornerShape(10.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(GreenSuccess.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Restaurant, contentDescription = null, tint = GreenSuccess, modifier = Modifier.size(32.dp))
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            NutritionLog.MealType.label(log.mealType),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = TextDark
                        )
                        Text(
                            SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(log.timestamp)),
                            fontSize = 11.sp, color = TextGray
                        )
                    }
                    IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextGray, modifier = Modifier.size(16.dp))
                    }
                }

                if (log.description.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(log.description, fontSize = 13.sp, color = TextDark, maxLines = 2)
                }

                // Nutrition pills
                if (log.estimatedCalories > 0 || log.effectiveCalories > 0) {
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        NutritionPill("${log.effectiveCalories} kcal", Color(0xFFEF4444))
                        if (log.estimatedProteinG > 0) NutritionPill("${log.estimatedProteinG.toInt()}g P", Color(0xFF3B82F6))
                        if (log.estimatedCarbsG > 0) NutritionPill("${log.estimatedCarbsG.toInt()}g C", Color(0xFFF59E0B))
                    }
                }

                // AI identified foods
                if (log.aiStatus == NutritionLog.AiStatus.COMPLETE) {
                    val foods = runCatching {
                        com.google.gson.JsonParser.parseString(log.recognizedFoods).asJsonArray
                            .take(3).joinToString(", ") { it.asString }
                    }.getOrDefault("")
                    if (foods.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = GreenSuccess, modifier = Modifier.size(12.dp))
                            Text(foods, fontSize = 11.sp, color = TextGray, maxLines = 1)
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete meal?") },
            text = { Text("This will remove this meal log permanently.") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteDialog = false }) {
                    Text("Delete", color = RedDanger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun NutritionPill(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(text, fontSize = 11.sp, color = color, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogMealBottomSheet(
    isLogging: Boolean,
    selectedUri: Uri?,
    onPickPhoto: () -> Unit,
    onLog: (mealType: String, description: String, manualCalories: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedMealType by remember { mutableStateOf(NutritionLog.MealType.MEAL) }
    var description by remember { mutableStateOf("") }
    var manualCaloriesText by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = LightCard,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Log a Meal", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDark)

            // Meal type selector
            Text("Meal Type", fontSize = 13.sp, color = TextGray, fontWeight = FontWeight.Medium)
            val types = NutritionLog.MealType.all
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                types.take(3).forEach { type ->
                    MealTypeChip(
                        label = NutritionLog.MealType.label(type),
                        selected = selectedMealType == type,
                        onClick = { selectedMealType = type },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                types.drop(3).forEach { type ->
                    MealTypeChip(
                        label = NutritionLog.MealType.label(type),
                        selected = selectedMealType == type,
                        onClick = { selectedMealType = type },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Photo section
            Text("Photo (optional — enables AI analysis)", fontSize = 13.sp, color = TextGray, fontWeight = FontWeight.Medium)
            if (selectedUri != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onPickPhoto)
                ) {
                    AsyncImage(
                        model = selectedUri,
                        contentDescription = "Selected meal photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(LightCard.copy(alpha = 0.85f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Change", fontSize = 12.sp, color = TextDark)
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(LightBorder)
                        .border(1.dp, LightBorder, RoundedCornerShape(12.dp))
                        .clickable(onClick = onPickPhoto),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = TextGray, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.height(4.dp))
                        Text("Pick a photo", fontSize = 13.sp, color = TextGray)
                    }
                }
            }

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Description (optional)") },
                placeholder = { Text("e.g. Grilled chicken with rice", color = TextGray.copy(alpha = 0.5f)) },
                maxLines = 2,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GreenSuccess,
                    unfocusedBorderColor = LightBorder,
                    focusedTextColor = TextDark,
                    unfocusedTextColor = TextDark,
                    cursorColor = GreenSuccess,
                    focusedContainerColor = LightCard,
                    unfocusedContainerColor = LightCard
                ),
                shape = RoundedCornerShape(10.dp)
            )

            // Manual calorie override
            OutlinedTextField(
                value = manualCaloriesText,
                onValueChange = { if (it.all { c -> c.isDigit() }) manualCaloriesText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Calories (optional override)") },
                placeholder = { Text("e.g. 450", color = TextGray.copy(alpha = 0.5f)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GreenSuccess,
                    unfocusedBorderColor = LightBorder,
                    focusedTextColor = TextDark,
                    unfocusedTextColor = TextDark,
                    cursorColor = GreenSuccess,
                    focusedContainerColor = LightCard,
                    unfocusedContainerColor = LightCard
                ),
                shape = RoundedCornerShape(10.dp)
            )

            if (selectedUri != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = GreenSuccess, modifier = Modifier.size(16.dp))
                    Text(
                        "AI will analyze your photo to estimate calories & macros",
                        fontSize = 12.sp,
                        color = GreenSuccess,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Button(
                onClick = {
                    val calories = manualCaloriesText.toIntOrNull() ?: 0
                    onLog(selectedMealType, description.trim(), calories)
                },
                enabled = !isLogging && (description.isNotBlank() || selectedUri != null),
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess, contentColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLogging) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Logging...")
                } else {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Log Meal", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun MealTypeChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val bg = if (selected) GreenSuccess else LightBorder
    val textColor = if (selected) Color.White else TextGray
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 12.sp, color = textColor, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}
