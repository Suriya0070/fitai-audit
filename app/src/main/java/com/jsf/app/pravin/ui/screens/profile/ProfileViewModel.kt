package com.jsf.app.pravin.ui.screens.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.jsf.app.pravin.data.db.FitnessDatabase
import com.jsf.app.pravin.data.model.UserProfile
import com.jsf.app.pravin.data.preferences.UserPreferences
import com.jsf.app.pravin.data.repository.FitnessRepository
import com.jsf.app.pravin.domain.ConsistencyTracker
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class ProfileStats(
    val totalSessions: Int = 0,
    val totalReps: Int = 0,
    val bestScore: Float = 0f,
    val streak: Int = 0,
    val totalMinutes: Int = 0
)

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val db = FitnessDatabase.getInstance(application)
    private val repository = FitnessRepository(db)
    private val prefs = UserPreferences(application)
    private val consistencyTracker = ConsistencyTracker()

    val userProfile: StateFlow<UserProfile?> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentUserId: StateFlow<String> = prefs.currentUserId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    private val _stats = MutableStateFlow(ProfileStats())
    val stats: StateFlow<ProfileStats> = _stats.asStateFlow()

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    init {
        viewModelScope.launch {
            repository.allSessions.collect { sessions ->
                val timestamps = sessions.map { it.timestamp }
                val streak = consistencyTracker.calculateStreak(timestamps)
                _stats.value = ProfileStats(
                    totalSessions = sessions.size,
                    totalReps = sessions.sumOf { it.repCount },
                    bestScore = sessions.maxOfOrNull { it.totalScore } ?: 0f,
                    streak = streak,
                    totalMinutes = sessions.sumOf { it.durationSeconds } / 60
                )
            }
        }
    }

    fun saveProfile(profile: UserProfile) {
        viewModelScope.launch {
            repository.saveProfile(profile)
            _saveSuccess.value = true
        }
    }

    fun clearSaveSuccess() {
        _saveSuccess.value = false
    }

    fun signOut(onDone: () -> Unit) {
        viewModelScope.launch {
            runCatching { FirebaseAuth.getInstance().signOut() }
            prefs.setCurrentUserId("")
            prefs.setOnboardingDone(false)
            onDone()
        }
    }

    fun deleteAccount(password: String, onDone: (success: Boolean, error: String?) -> Unit) {
        viewModelScope.launch {
            val userId = prefs.currentUserId.first()

            if (userId.startsWith("local_")) {
                // Guest user — no Firebase account to delete, just clear local data
                prefs.setCurrentUserId("")
                prefs.setOnboardingDone(false)
                db.clearAllTables()
                onDone(true, null)
                return@launch
            }

            runCatching {
                val firebaseUser = FirebaseAuth.getInstance().currentUser
                    ?: throw Exception("Not signed in")

                // Firebase requires re-authentication before account deletion
                val credential = EmailAuthProvider.getCredential(
                    firebaseUser.email ?: throw Exception("No email on account"),
                    password
                )
                firebaseUser.reauthenticate(credential).await()

                // Delete Firestore public profile document
                runCatching {
                    FirebaseFirestore.getInstance()
                        .collection("users").document(userId).delete().await()
                }

                // Delete Firebase Auth account
                firebaseUser.delete().await()

                // Clear all local data
                prefs.setCurrentUserId("")
                prefs.setOnboardingDone(false)
                db.clearAllTables()

            }.fold(
                onSuccess = { onDone(true, null) },
                onFailure = { e ->
                    val msg = e.message ?: ""
                    val readable = when {
                        "wrong-password" in msg || "invalid-credential" in msg ||
                        "INVALID_LOGIN_CREDENTIALS" in msg -> "Incorrect password. Please try again."
                        "requires-recent-login" in msg -> "Please sign out and sign back in, then try again."
                        "network" in msg.lowercase() -> "Network error. Check your connection and try again."
                        else -> "Failed to delete account: $msg"
                    }
                    onDone(false, readable)
                }
            )
        }
    }
}
