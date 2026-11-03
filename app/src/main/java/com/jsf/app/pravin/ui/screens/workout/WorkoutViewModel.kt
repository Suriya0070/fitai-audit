package com.jsf.app.pravin.ui.screens.workout

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import com.google.gson.Gson
import com.jsf.app.pravin.data.db.FitnessDatabase
import com.jsf.app.pravin.data.model.Achievement
import com.jsf.app.pravin.data.model.RepFormRecord
import com.jsf.app.pravin.data.model.WorkoutSession
import com.jsf.app.pravin.data.preferences.UserPreferences
import com.jsf.app.pravin.data.repository.FitnessRepository
import com.jsf.app.pravin.domain.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

sealed class WorkoutState {
    object Idle : WorkoutState()
    object Instructions : WorkoutState()
    object EnvironmentCheck : WorkoutState()
    object Active : WorkoutState()
    data class Rest(val secondsLeft: Int, val setsCompleted: Int, val totalReps: Int) : WorkoutState()
    data class Summary(val session: WorkoutSession) : WorkoutState()
}

class WorkoutViewModel(application: Application) : AndroidViewModel(application) {

    private val db = FitnessDatabase.getInstance(application)
    private val repository = FitnessRepository(db)
    private val prefs = UserPreferences(application)
    private val analytics = FirebaseAnalytics.getInstance(application)
    private val gson = Gson()

    val poseAnalyzer = PoseAnalyzer(application)
    private val scoreEngine = WorkoutScoreEngine()
    private val repCounter = RepCounter()
    private val envAnalyzer = EnvironmentAnalyzer()
    private val fatiguePredictor = FatiguePredictor()
    private var formAnalyzer: ExerciseFormAnalyzer = NoOpFormAnalyzer()

    private val _workoutState = MutableStateFlow<WorkoutState>(WorkoutState.Idle)
    val workoutState: StateFlow<WorkoutState> = _workoutState.asStateFlow()

    private val _poseResult = MutableStateFlow(PoseResult.EMPTY)
    val poseResult: StateFlow<PoseResult> = _poseResult.asStateFlow()

    private val _workoutScore = MutableStateFlow(WorkoutScore())
    val workoutScore: StateFlow<WorkoutScore> = _workoutScore.asStateFlow()

    private val _repCount = MutableStateFlow(0)
    val repCount: StateFlow<Int> = _repCount.asStateFlow()

    private val _fatigueReport = MutableStateFlow(FatigueReport())
    val fatigueReport: StateFlow<FatigueReport> = _fatigueReport.asStateFlow()

    private val _environmentQuality = MutableStateFlow(EnvironmentQuality())
    val environmentQuality: StateFlow<EnvironmentQuality> = _environmentQuality.asStateFlow()

    private val _sessionDuration = MutableStateFlow(0)
    val sessionDuration: StateFlow<Int> = _sessionDuration.asStateFlow()

    private val _selectedExercise = MutableStateFlow("Squats")
    val selectedExercise: StateFlow<String> = _selectedExercise.asStateFlow()

    private val _modelLoaded = MutableStateFlow(false)
    val modelLoaded: StateFlow<Boolean> = _modelLoaded.asStateFlow()

    // Single executor shared across camera sessions — shut down in onCleared() to avoid thread leaks
    val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private var timerJob: Job? = null
    private var restTimerJob: Job? = null
    private var sessionStartTime = 0L
    private val sessionScoreHistory = java.util.concurrent.CopyOnWriteArrayList<WorkoutScore>()
    private val MAX_SCORE_HISTORY = 500
    private val completedSetReps = mutableListOf<Int>()
    private var setsCompleted = 0

    private val _newAchievements = MutableStateFlow<List<Achievement>>(emptyList())
    val newAchievements: StateFlow<List<Achievement>> = _newAchievements.asStateFlow()

    // Real-time form errors from the current frame — shown as an overlay during active workout
    private val _formErrors = MutableStateFlow<List<FormError>>(emptyList())
    val formErrors: StateFlow<List<FormError>> = _formErrors.asStateFlow()

    // Per-rep form records collected during this session
    private val sessionRepRecords = mutableListOf<RepFormRecord>()
    private var lastRepScoreSnapshot = WorkoutScore()

    fun clearNewAchievements() { _newAchievements.value = emptyList() }

    init {
        viewModelScope.launch {
            prefs.selectedExercise.collect { exercise ->
                _selectedExercise.value = exercise
            }
        }
        initializePoseAnalyzer()
    }

    private fun initializePoseAnalyzer() {
        poseAnalyzer.initialize { poseResult ->
            _poseResult.value = poseResult
            if (_workoutState.value is WorkoutState.Active) {
                val time = System.currentTimeMillis()
                val score = scoreEngine.calculateScore(poseResult, _selectedExercise.value, time)
                _workoutScore.value = score
                fatiguePredictor.update(score.overall)
                _fatigueReport.value = fatiguePredictor.predict()

                val errors = formAnalyzer.analyze(poseResult)
                _formErrors.value = errors

                val repCompleted = repCounter.update(poseResult, _selectedExercise.value)
                if (repCompleted) {
                    val repNum = repCounter.getCount()
                    _repCount.value = repNum
                    scoreEngine.onRepCompleted(time)
                    lastRepScoreSnapshot = score
                    val repRecord = RepFormRecord(
                        sessionId = sessionStartTime,
                        repNumber = repNum,
                        exerciseType = _selectedExercise.value,
                        formErrors = gson.toJson(errors),
                        repScore = score.overall
                    )
                    sessionRepRecords.add(repRecord)
                }
                if (sessionScoreHistory.size >= MAX_SCORE_HISTORY) {
                    try { sessionScoreHistory.removeAt(0) } catch (_: Exception) {}
                }
                sessionScoreHistory.add(score)
            }
        }
        _modelLoaded.value = poseAnalyzer.isModelLoaded
    }

    fun onNewFrame(bitmap: Bitmap, frameTime: Long) {
        poseAnalyzer.analyze(bitmap, frameTime)
        if (_workoutState.value is WorkoutState.EnvironmentCheck ||
            _workoutState.value is WorkoutState.Active
        ) {
            val env = envAnalyzer.analyze(bitmap, _poseResult.value)
            _environmentQuality.value = env
        }
    }

    fun startInstructions() {
        _workoutState.value = WorkoutState.Instructions
    }

    fun startEnvironmentCheck() {
        _workoutState.value = WorkoutState.EnvironmentCheck
    }

    fun startWorkout() {
        scoreEngine.reset()
        repCounter.reset()
        fatiguePredictor.reset()
        sessionScoreHistory.clear()
        sessionRepRecords.clear()
        completedSetReps.clear()
        setsCompleted = 0
        _repCount.value = 0
        _workoutScore.value = WorkoutScore()
        _fatigueReport.value = FatigueReport()
        _formErrors.value = emptyList()
        _sessionDuration.value = 0
        sessionStartTime = System.currentTimeMillis()
        formAnalyzer = ExerciseFormAnalyzer.forExercise(_selectedExercise.value)
        analytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT) {
            param(FirebaseAnalytics.Param.CONTENT_TYPE, "workout_start")
            param(FirebaseAnalytics.Param.ITEM_NAME, _selectedExercise.value)
        }

        _workoutState.value = WorkoutState.Active
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                _sessionDuration.value = ((System.currentTimeMillis() - sessionStartTime) / 1000).toInt()
            }
        }
    }

    fun endSet(restSeconds: Int = 60) {
        completedSetReps.add(_repCount.value)
        setsCompleted++
        val totalReps = completedSetReps.sum()
        repCounter.reset()
        _repCount.value = 0
        _workoutState.value = WorkoutState.Rest(restSeconds, setsCompleted, totalReps)
        restTimerJob?.cancel()
        restTimerJob = viewModelScope.launch {
            var remaining = restSeconds
            while (remaining > 0) {
                delay(1000L)
                remaining--
                _workoutState.value = WorkoutState.Rest(remaining, setsCompleted, totalReps)
            }
            resumeAfterRest()
        }
    }

    fun skipRest() {
        restTimerJob?.cancel()
        resumeAfterRest()
    }

    private fun resumeAfterRest() {
        triggerRestEndAlert()
        _workoutState.value = WorkoutState.Active
    }

    private fun triggerRestEndAlert() {
        // Vibrate: 3 pulses (off, short, pause, short, pause, long)
        val app = getApplication<Application>()
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (app.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                app.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            val pattern = longArrayOf(0, 150, 80, 150, 80, 400)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        } catch (_: Exception) {}

        // Beep sound
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 600)
            Handler(Looper.getMainLooper()).postDelayed({ toneGen.release() }, 1200)
        } catch (_: Exception) {}
    }

    fun stopWorkout() {
        timerJob?.cancel()
        restTimerJob?.cancel()
        // Include reps from any in-progress set
        val finalRepCount = completedSetReps.sum() + _repCount.value
        val avgScore = if (sessionScoreHistory.isNotEmpty()) {
            sessionScoreHistory.map { it.overall }.average().toFloat()
        } else 0f

        val avgAccuracy = if (sessionScoreHistory.isNotEmpty())
            sessionScoreHistory.map { it.accuracy }.average().toFloat() else 0f
        val avgBalance = if (sessionScoreHistory.isNotEmpty())
            sessionScoreHistory.map { it.balance }.average().toFloat() else 0f
        val avgStability = if (sessionScoreHistory.isNotEmpty())
            sessionScoreHistory.map { it.stability }.average().toFloat() else 0f
        val avgSymmetry = if (sessionScoreHistory.isNotEmpty())
            sessionScoreHistory.map { it.symmetry }.average().toFloat() else 0f
        val avgSpeed = if (sessionScoreHistory.isNotEmpty())
            sessionScoreHistory.map { it.speed }.average().toFloat() else 0f

        val fatigue = _fatigueReport.value
        val session = WorkoutSession(
            exerciseType = _selectedExercise.value,
            durationSeconds = _sessionDuration.value,
            repCount = finalRepCount,
            totalScore = avgScore,
            accuracyScore = avgAccuracy,
            balanceScore = avgBalance,
            stabilityScore = avgStability,
            symmetryScore = avgSymmetry,
            speedScore = avgSpeed,
            injuryRisk = fatigue.injuryRisk,
            fatigueLevel = fatigue.fatigueLevel,
            environmentQuality = _environmentQuality.value.overall * 100f
        )

        viewModelScope.launch {
            val savedId = repository.saveSession(session)
            val savedSession = session.copy(id = savedId)
            val repRecordsCopy = sessionRepRecords.map { it.copy(sessionId = savedId) }
            repRecordsCopy.forEach { db.repFormRecordDao().insert(it) }
            val userId = prefs.currentUserId.first()
            if (userId.isNotEmpty()) {
                val allSessions = repository.allSessions.first()
                checkAndAwardAchievements(savedSession, allSessions, userId)
            }
            analytics.logEvent("workout_completed") {
                param("exercise", savedSession.exerciseType)
                param("reps", savedSession.repCount.toLong())
                param("score", savedSession.totalScore.toLong())
                param("duration_seconds", savedSession.durationSeconds.toLong())
            }
            _workoutState.value = WorkoutState.Summary(savedSession)
        }
    }

    private suspend fun checkAndAwardAchievements(
        session: WorkoutSession,
        allSessions: List<WorkoutSession>,
        userId: String
    ) {
        val tracker = ConsistencyTracker()
        val streak = tracker.calculateStreak(allSessions.map { it.timestamp })
        val totalReps = allSessions.sumOf { it.repCount }
        val earned = mutableListOf<Achievement>()

        suspend fun maybeAward(type: String, title: String, description: String) {
            if (repository.getAchievementByType(userId, type) == null) {
                val a = Achievement(
                    userId = userId, type = type,
                    title = title, description = description
                )
                repository.insertAchievement(a)
                earned.add(a)
            }
        }

        if (allSessions.size == 1)
            maybeAward(Achievement.AchievementType.FIRST_WORKOUT, "First Rep", "Completed your first workout")
        if (streak >= 7)
            maybeAward(Achievement.AchievementType.STREAK_7, "On Fire", "Maintained a 7-day streak")
        if (streak >= 30)
            maybeAward(Achievement.AchievementType.STREAK_30, "Unstoppable", "Maintained a 30-day streak")
        if (totalReps >= 1_000)
            maybeAward(Achievement.AchievementType.TOTAL_REPS_1000, "Rep Warrior", "Completed 1,000 total reps")
        if (totalReps >= 10_000)
            maybeAward(Achievement.AchievementType.TOTAL_REPS_10000, "Rep Legend", "Completed 10,000 total reps")
        if (session.totalScore >= 95f)
            maybeAward(Achievement.AchievementType.PERFECT_SCORE, "Perfect Form", "Achieved a perfect score of 95+")

        if (earned.isNotEmpty()) _newAchievements.value = earned
    }

    fun resetToIdle() {
        restTimerJob?.cancel()
        timerJob?.cancel()
        _workoutState.value = WorkoutState.Idle
        _repCount.value = 0
        _workoutScore.value = WorkoutScore()
        _fatigueReport.value = FatigueReport()
        _formErrors.value = emptyList()
        _sessionDuration.value = 0
        sessionScoreHistory.clear()
        sessionRepRecords.clear()
        completedSetReps.clear()
        setsCompleted = 0
    }

    fun setExercise(exercise: String) {
        viewModelScope.launch {
            prefs.setSelectedExercise(exercise)
        }
        _selectedExercise.value = exercise
    }

    override fun onCleared() {
        super.onCleared()
        poseAnalyzer.close()
        timerJob?.cancel()
        restTimerJob?.cancel()
        cameraExecutor.shutdownNow()
    }
}
