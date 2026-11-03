package com.jsf.app.pravin.data.remote.auth

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.jsf.app.pravin.data.preferences.UserPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import java.util.UUID

class AuthRepository(context: Context) {

    private val prefs = UserPreferences(context)

    val currentUserIdFlow: Flow<String> = prefs.currentUserId

    val firebaseAuthStateFlow: Flow<FirebaseUser?> = callbackFlow {
        val auth = FirebaseAuth.getInstance()
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun getCurrentUserId(): String = prefs.currentUserId.first()

    suspend fun signInWithEmail(email: String, password: String): Result<String> {
        return try {
            val result = FirebaseAuth.getInstance()
                .signInWithEmailAndPassword(email, password)
                .await()
            val uid = result.user?.uid ?: return Result.failure(Exception("Sign in failed"))
            prefs.setCurrentUserId(uid)
            prefs.setOnboardingDone(true)
            Result.success(uid)
        } catch (e: Exception) {
            Result.failure(mapFirebaseError(e))
        }
    }

    suspend fun signUpWithEmail(email: String, password: String): Result<String> {
        return try {
            val result = FirebaseAuth.getInstance()
                .createUserWithEmailAndPassword(email, password)
                .await()
            val uid = result.user?.uid ?: return Result.failure(Exception("Sign up failed"))
            result.user?.sendEmailVerification()
            prefs.setCurrentUserId(uid)
            prefs.setOnboardingDone(false)
            Result.success(uid)
        } catch (e: Exception) {
            Result.failure(mapFirebaseError(e))
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            FirebaseAuth.getInstance().sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapFirebaseError(e))
        }
    }

    suspend fun signOut() {
        runCatching { FirebaseAuth.getInstance().signOut() }
        prefs.setCurrentUserId("")
        prefs.setOnboardingDone(false)
    }

    // Guest mode — creates a persistent local session without a Firebase account.
    // Guest users cannot access trainer features or sync community data.
    suspend fun signInAsLocalUser(): String {
        val existing = prefs.currentUserId.first()
        if (existing.isNotEmpty()) return existing
        val uid = "local_${UUID.randomUUID()}"
        prefs.setCurrentUserId(uid)
        return uid
    }

    private fun mapFirebaseError(e: Exception): Exception {
        val msg = e.message ?: "Unknown error"
        return when {
            "INVALID_EMAIL" in msg || "badly formatted" in msg -> Exception("Invalid email address")
            "WRONG_PASSWORD" in msg || "invalid-credential" in msg -> Exception("Incorrect email or password")
            "EMAIL_NOT_FOUND" in msg || "no user record" in msg -> Exception("No account found with this email")
            "WEAK_PASSWORD" in msg || "at least 6" in msg -> Exception("Password must be at least 6 characters")
            "EMAIL_EXISTS" in msg || "already in use" in msg -> Exception("An account already exists with this email")
            "TOO_MANY_REQUESTS" in msg -> Exception("Too many attempts. Please try again later")
            "NETWORK_ERROR" in msg || "network" in msg.lowercase() -> Exception("Network error. Check your connection")
            else -> Exception(msg)
        }
    }
}
