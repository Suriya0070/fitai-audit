package com.jsf.app.pravin.data.remote.auth

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.jsf.app.pravin.data.preferences.UserPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import java.util.UUID

class AuthRepository(private val context: Context) {

    private val prefs = UserPreferences(context)
    private val analytics get() = FirebaseAnalytics.getInstance(context)

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
            analytics.logEvent(FirebaseAnalytics.Event.LOGIN) { param(FirebaseAnalytics.Param.METHOD, "email") }
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
            syncMinimalProfile(uid, email.substringBefore('@'))
            analytics.logEvent(FirebaseAnalytics.Event.SIGN_UP) { param(FirebaseAnalytics.Param.METHOD, "email") }
            Result.success(uid)
        } catch (e: Exception) {
            Result.failure(mapFirebaseError(e))
        }
    }

    // Exchanges a Google ID token (from GoogleSignIn / Credential Manager) for a Firebase user.
    // ACTION REQUIRED: Add your OAuth 2.0 Web Client ID (from Firebase Console → Authentication →
    // Google → Web SDK configuration) to strings.xml as <string name="default_web_client_id">.
    suspend fun signInWithGoogle(idToken: String): Result<String> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = FirebaseAuth.getInstance()
                .signInWithCredential(credential)
                .await()
            val uid = result.user?.uid ?: return Result.failure(Exception("Google sign-in failed"))
            val isNew = result.additionalUserInfo?.isNewUser == true
            prefs.setCurrentUserId(uid)
            prefs.setOnboardingDone(!isNew)
            if (isNew) {
                val displayName = result.user?.displayName ?: ""
                syncMinimalProfile(uid, displayName)
            }
            val event = if (isNew) FirebaseAnalytics.Event.SIGN_UP else FirebaseAnalytics.Event.LOGIN
            analytics.logEvent(event) { param(FirebaseAnalytics.Param.METHOD, "google") }
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

    suspend fun signInAsLocalUser(): String {
        val existing = prefs.currentUserId.first()
        if (existing.isNotEmpty()) return existing
        val uid = "local_${UUID.randomUUID()}"
        prefs.setCurrentUserId(uid)
        return uid
    }

    // Writes a minimal public profile on first sign-up so the user is discoverable
    // immediately — before they ever open the Friends screen.
    private suspend fun syncMinimalProfile(uid: String, displayName: String) {
        runCatching {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .set(
                    mapOf(
                        "userId" to uid,
                        "name" to displayName,
                        "fitnessLevel" to "Beginner",
                        "updatedAt" to System.currentTimeMillis()
                    )
                )
                .await()
        }
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
