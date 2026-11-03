package com.jsf.app.pravin.ui.screens.nutrition

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.vertexai.vertexAI
import com.google.firebase.vertexai.type.content
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.jsf.app.pravin.data.db.FitnessDatabase
import com.jsf.app.pravin.data.model.NutritionLog
import com.jsf.app.pravin.data.preferences.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.*

class NutritionViewModel(application: Application) : AndroidViewModel(application) {

    private val db = FitnessDatabase.getInstance(application)
    private val prefs = UserPreferences(application)
    private val gson = Gson()
    private val storage = FirebaseStorage.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _userId = MutableStateFlow("")
    private val _selectedDayStart = MutableStateFlow(todayMidnight())
    val selectedDayStart: StateFlow<Long> = _selectedDayStart.asStateFlow()

    val logsForDay: StateFlow<List<NutritionLog>> = combine(
        _userId.filter { it.isNotEmpty() },
        _selectedDayStart
    ) { uid, day -> Pair(uid, day) }
        .flatMapLatest { (uid, day) ->
            db.nutritionLogDao().getLogsForDay(uid, day, day + DAY_MS)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dailySummary: StateFlow<NutritionSummary> = logsForDay.map { logs ->
        NutritionSummary(
            totalCalories = logs.sumOf { it.effectiveCalories },
            totalProteinG = logs.sumOf { it.estimatedProteinG.toDouble() }.toFloat(),
            totalCarbsG = logs.sumOf { it.estimatedCarbsG.toDouble() }.toFloat(),
            totalFatG = logs.sumOf { it.estimatedFatG.toDouble() }.toFloat(),
            mealCount = logs.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NutritionSummary())

    private val _isLogging = MutableStateFlow(false)
    val isLogging: StateFlow<Boolean> = _isLogging.asStateFlow()

    private val _analyzingAi = MutableStateFlow(false)
    val analyzingAi: StateFlow<Boolean> = _analyzingAi.asStateFlow()

    private val _logError = MutableStateFlow<String?>(null)
    val logError: StateFlow<String?> = _logError.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.currentUserId.collect { uid -> _userId.value = uid }
        }
    }

    fun goToPreviousDay() { _selectedDayStart.value -= DAY_MS }

    fun goToNextDay() {
        if (_selectedDayStart.value < todayMidnight()) _selectedDayStart.value += DAY_MS
    }

    fun logMeal(
        mealType: String,
        description: String,
        imageUri: Uri?,
        manualCalories: Int = 0
    ) {
        val uid = _userId.value
        if (uid.isEmpty() || uid.startsWith("local_")) {
            _logError.value = "Sign in to log meals"
            return
        }
        _isLogging.value = true
        viewModelScope.launch {
            try {
                val logId = UUID.randomUUID().toString()
                val bitmap = imageUri?.let { loadBitmap(getApplication(), it) }

                val storageUrl = if (bitmap != null) {
                    runCatching { uploadPhoto(uid, logId, bitmap) }.getOrDefault("")
                } else ""

                var aiStatus = NutritionLog.AiStatus.NONE
                var foods = "[]"
                var calories = manualCalories
                var protein = 0f
                var carbs = 0f
                var fat = 0f

                if (bitmap != null) {
                    _analyzingAi.value = true
                    val result = runCatching { analyzeWithGemini(bitmap) }.getOrNull()
                    if (result != null) {
                        aiStatus = NutritionLog.AiStatus.COMPLETE
                        foods = gson.toJson(result.foods)
                        if (calories == 0) calories = result.calories
                        protein = result.proteinG
                        carbs = result.carbsG
                        fat = result.fatG
                    } else {
                        aiStatus = NutritionLog.AiStatus.FAILED
                    }
                    _analyzingAi.value = false
                }

                val log = NutritionLog(
                    id = logId,
                    userId = uid,
                    mealType = mealType,
                    description = description,
                    imageUrl = storageUrl,
                    aiStatus = aiStatus,
                    recognizedFoods = foods,
                    estimatedCalories = calories,
                    estimatedProteinG = protein,
                    estimatedCarbsG = carbs,
                    estimatedFatG = fat,
                    trainerVisible = true
                )
                db.nutritionLogDao().upsert(log)
                runCatching { syncToFirestore(log) }
            } catch (e: Exception) {
                _logError.value = e.message ?: "Failed to log meal"
            } finally {
                _isLogging.value = false
                _analyzingAi.value = false
            }
        }
    }

    fun deleteLog(log: NutritionLog) {
        viewModelScope.launch { db.nutritionLogDao().delete(log) }
    }

    fun clearError() { _logError.value = null }

    private suspend fun loadBitmap(context: Context, uri: Uri): Bitmap? =
        withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)?.let { bmp ->
                        val maxDim = 768
                        val scale = (maxDim.toFloat() / maxOf(bmp.width, bmp.height)).coerceAtMost(1f)
                        if (scale < 1f)
                            Bitmap.createScaledBitmap(bmp, (bmp.width * scale).toInt(), (bmp.height * scale).toInt(), true)
                        else bmp
                    }
                }
            }.getOrNull()
        }

    private suspend fun uploadPhoto(userId: String, logId: String, bitmap: Bitmap): String =
        withContext(Dispatchers.IO) {
            val bytes = ByteArrayOutputStream().also {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it)
            }.toByteArray()
            val ref = storage.reference.child("meals/$userId/$logId.jpg")
            ref.putBytes(bytes).await()
            ref.downloadUrl.await().toString()
        }

    private suspend fun analyzeWithGemini(bitmap: Bitmap): GeminiNutritionResult =
        withContext(Dispatchers.IO) {
            val model = Firebase.vertexAI.generativeModel("gemini-1.5-flash")
            val prompt = """
                Analyze this food photo. Return ONLY valid JSON (no markdown, no extra text):
                {"foods":["food1","food2"],"calories":350,"proteinG":12.5,"carbsG":45.0,"fatG":8.0}
                Estimate calories and macros for the total visible portion.
                If no food visible: {"foods":[],"calories":0,"proteinG":0,"carbsG":0,"fatG":0}
            """.trimIndent()
            val response = model.generateContent(content {
                image(bitmap)
                text(prompt)
            })
            val raw = (response.text ?: "{}").trim()
                .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            parseGeminiResult(raw)
        }

    private fun parseGeminiResult(json: String): GeminiNutritionResult = runCatching {
        val obj = JsonParser.parseString(json).asJsonObject
        GeminiNutritionResult(
            foods = obj.getAsJsonArray("foods")?.map { it.asString } ?: emptyList(),
            calories = obj.get("calories")?.asInt ?: 0,
            proteinG = obj.get("proteinG")?.asFloat ?: 0f,
            carbsG = obj.get("carbsG")?.asFloat ?: 0f,
            fatG = obj.get("fatG")?.asFloat ?: 0f
        )
    }.getOrDefault(GeminiNutritionResult())

    private fun syncToFirestore(log: NutritionLog) {
        if (log.userId.isEmpty() || log.userId.startsWith("local_")) return
        firestore.collection("nutritionLogs")
            .document("${log.userId}_${log.id}")
            .set(mapOf(
                "userId" to log.userId,
                "mealType" to log.mealType,
                "description" to log.description,
                "imageUrl" to log.imageUrl,
                "aiStatus" to log.aiStatus,
                "recognizedFoods" to gson.fromJson(log.recognizedFoods, List::class.java),
                "estimatedCalories" to log.estimatedCalories,
                "estimatedProteinG" to log.estimatedProteinG,
                "estimatedCarbsG" to log.estimatedCarbsG,
                "estimatedFatG" to log.estimatedFatG,
                "trainerVisible" to log.trainerVisible,
                "timestamp" to log.timestamp
            ))
    }

    companion object {
        const val DAY_MS = 24 * 60 * 60 * 1000L
        fun todayMidnight(): Long {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }
    }
}

data class NutritionSummary(
    val totalCalories: Int = 0,
    val totalProteinG: Float = 0f,
    val totalCarbsG: Float = 0f,
    val totalFatG: Float = 0f,
    val mealCount: Int = 0
)

data class GeminiNutritionResult(
    val foods: List<String> = emptyList(),
    val calories: Int = 0,
    val proteinG: Float = 0f,
    val carbsG: Float = 0f,
    val fatG: Float = 0f
)
