package com.jsf.app.pravin.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class TrainerClient(
    val id: String = "",
    val trainerId: String = "",
    val clientId: String = "",
    val clientEmail: String = "",
    val clientName: String = "",
    val status: String = "active",
    val grantedAt: Long = 0L,
    val lastActiveAt: Long = 0L,
    val dropoutRisk: Float = 0f,
    val overallScore: Float = 0f
) {
    object Status {
        const val ACTIVE = "active"
        const val PENDING = "pending"
        const val REMOVED = "removed"
    }
}

data class TrainerInvite(
    val id: String = "",
    val trainerId: String = "",
    val trainerName: String = "",
    val email: String = "",
    val token: String = "",
    val status: String = "pending",
    val createdAt: Long = 0L
)

data class TrainerAlert(
    val id: String = "",
    val trainerId: String = "",
    val clientId: String = "",
    val clientName: String = "",
    val type: String = "",
    val message: String = "",
    val createdAt: Long = 0L,
    val isRead: Boolean = false
) {
    object Type {
        const val MISSED_WORKOUT = "missed_workout"
        const val FORM_REGRESSION = "form_regression"
        const val DROPOUT_RISK = "dropout_risk"
        const val STREAK_BROKEN = "streak_broken"
    }
}

data class TrainerNote(
    val id: String = "",
    val trainerId: String = "",
    val clientId: String = "",
    val content: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

data class ClientStatusSnapshot(
    val trainerId: String = "",
    val clientId: String = "",
    val clientName: String = "",
    val overallScore: Float = 0f,
    val workoutFrequency: Float = 0f,
    val formScore: Float = 0f,
    val consistency: Float = 0f,
    val engagement: Float = 0f,
    val dropoutRisk: Float = 0f,
    val sessionCount: Int = 0,
    val lastWorkoutAt: Long = 0L,
    val updatedAt: Long = 0L
)

class TrainerDataSource {
    private val fs = FirebaseFirestore.getInstance()
    private val trainerClients get() = fs.collection("trainer_clients")
    private val trainerInvites get() = fs.collection("trainer_invites")
    private val trainerAlerts get() = fs.collection("trainer_alerts")
    private val trainerNotes get() = fs.collection("trainer_notes")
    private val clientStatusSnapshots get() = fs.collection("client_status_snapshots")
    private val workoutSessions get() = fs.collection("workoutSessions")

    // ── Client list ───────────────────────────────────────────────────────

    fun getClients(trainerId: String): Flow<List<TrainerClient>> = callbackFlow {
        val reg = trainerClients
            .whereEqualTo("trainerId", trainerId)
            .whereEqualTo("status", TrainerClient.Status.ACTIVE)
            .addSnapshotListener { snap, err ->
                if (err != null) { trySend(emptyList()); return@addSnapshotListener }
                val clients = snap?.documents?.mapNotNull { doc ->
                    TrainerClient(
                        id = doc.id,
                        trainerId = doc.getString("trainerId") ?: return@mapNotNull null,
                        clientId = doc.getString("clientId") ?: return@mapNotNull null,
                        clientEmail = doc.getString("clientEmail") ?: "",
                        clientName = doc.getString("clientName") ?: "",
                        status = doc.getString("status") ?: TrainerClient.Status.ACTIVE,
                        grantedAt = doc.getLong("grantedAt") ?: 0L,
                        lastActiveAt = doc.getLong("lastActiveAt") ?: 0L,
                        dropoutRisk = (doc.getDouble("dropoutRisk") ?: 0.0).toFloat(),
                        overallScore = (doc.getDouble("overallScore") ?: 0.0).toFloat()
                    )
                } ?: emptyList()
                trySend(clients)
            }
        awaitClose { reg.remove() }
    }

    fun getPendingInvites(trainerId: String): Flow<List<TrainerInvite>> = callbackFlow {
        val reg = trainerInvites
            .whereEqualTo("trainerId", trainerId)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snap, err ->
                if (err != null) { trySend(emptyList()); return@addSnapshotListener }
                val invites = snap?.documents?.mapNotNull { doc ->
                    TrainerInvite(
                        id = doc.id,
                        trainerId = doc.getString("trainerId") ?: return@mapNotNull null,
                        trainerName = doc.getString("trainerName") ?: "",
                        email = doc.getString("email") ?: "",
                        token = doc.getString("token") ?: "",
                        status = doc.getString("status") ?: "pending",
                        createdAt = doc.getLong("createdAt") ?: 0L
                    )
                } ?: emptyList()
                trySend(invites)
            }
        awaitClose { reg.remove() }
    }

    // ── Invite a client ────────────────────────────────────────────────────

    suspend fun inviteClient(trainerId: String, trainerName: String, email: String): Result<String> =
        runCatching {
            val token = java.util.UUID.randomUUID().toString()
            val inviteId = "${trainerId}_${email.replace("@", "_").replace(".", "_")}"
            trainerInvites.document(inviteId).set(
                mapOf(
                    "trainerId" to trainerId,
                    "trainerName" to trainerName,
                    "email" to email.lowercase(),
                    "token" to token,
                    "status" to "pending",
                    "createdAt" to System.currentTimeMillis()
                )
            ).await()
            token
        }

    // ── Alerts ─────────────────────────────────────────────────────────────

    fun getAlerts(trainerId: String, limit: Long = 50): Flow<List<TrainerAlert>> = callbackFlow {
        val reg = trainerAlerts
            .whereEqualTo("trainerId", trainerId)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(limit)
            .addSnapshotListener { snap, err ->
                if (err != null) { trySend(emptyList()); return@addSnapshotListener }
                val alerts = snap?.documents?.mapNotNull { doc ->
                    TrainerAlert(
                        id = doc.id,
                        trainerId = doc.getString("trainerId") ?: return@mapNotNull null,
                        clientId = doc.getString("clientId") ?: return@mapNotNull null,
                        clientName = doc.getString("clientName") ?: "",
                        type = doc.getString("type") ?: "",
                        message = doc.getString("message") ?: "",
                        createdAt = doc.getLong("createdAt") ?: 0L,
                        isRead = doc.getBoolean("isRead") ?: false
                    )
                } ?: emptyList()
                trySend(alerts)
            }
        awaitClose { reg.remove() }
    }

    fun getAlertsForClient(trainerId: String, clientId: String): Flow<List<TrainerAlert>> = callbackFlow {
        val reg = trainerAlerts
            .whereEqualTo("trainerId", trainerId)
            .whereEqualTo("clientId", clientId)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(30)
            .addSnapshotListener { snap, err ->
                if (err != null) { trySend(emptyList()); return@addSnapshotListener }
                val alerts = snap?.documents?.mapNotNull { doc ->
                    TrainerAlert(
                        id = doc.id,
                        trainerId = doc.getString("trainerId") ?: return@mapNotNull null,
                        clientId = doc.getString("clientId") ?: return@mapNotNull null,
                        clientName = doc.getString("clientName") ?: "",
                        type = doc.getString("type") ?: "",
                        message = doc.getString("message") ?: "",
                        createdAt = doc.getLong("createdAt") ?: 0L,
                        isRead = doc.getBoolean("isRead") ?: false
                    )
                } ?: emptyList()
                trySend(alerts)
            }
        awaitClose { reg.remove() }
    }

    suspend fun markAlertRead(alertId: String) {
        runCatching { trainerAlerts.document(alertId).update("isRead", true).await() }
    }

    // ── Trainer notes ──────────────────────────────────────────────────────

    fun getNotesForClient(trainerId: String, clientId: String): Flow<List<TrainerNote>> = callbackFlow {
        val reg = trainerNotes
            .whereEqualTo("trainerId", trainerId)
            .whereEqualTo("clientId", clientId)
            .orderBy("updatedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) { trySend(emptyList()); return@addSnapshotListener }
                val notes = snap?.documents?.mapNotNull { doc ->
                    TrainerNote(
                        id = doc.id,
                        trainerId = doc.getString("trainerId") ?: return@mapNotNull null,
                        clientId = doc.getString("clientId") ?: return@mapNotNull null,
                        content = doc.getString("content") ?: "",
                        createdAt = doc.getLong("createdAt") ?: 0L,
                        updatedAt = doc.getLong("updatedAt") ?: 0L
                    )
                } ?: emptyList()
                trySend(notes)
            }
        awaitClose { reg.remove() }
    }

    suspend fun saveNote(trainerId: String, clientId: String, content: String, existingId: String? = null) {
        runCatching {
            val now = System.currentTimeMillis()
            val docId = existingId ?: "${trainerId}_${clientId}_$now"
            val data = mutableMapOf<String, Any>(
                "trainerId" to trainerId,
                "clientId" to clientId,
                "content" to content,
                "updatedAt" to now
            )
            if (existingId == null) data["createdAt"] = now
            trainerNotes.document(docId).set(data, com.google.firebase.firestore.SetOptions.merge()).await()
        }
    }

    // ── Client status snapshots ────────────────────────────────────────────

    fun getStatusSnapshot(trainerId: String, clientId: String): Flow<ClientStatusSnapshot?> = callbackFlow {
        val docId = "${trainerId}_$clientId"
        val reg = clientStatusSnapshots.document(docId)
            .addSnapshotListener { doc, err ->
                if (err != null) { trySend(null); return@addSnapshotListener }
                val snap = doc?.let {
                    ClientStatusSnapshot(
                        trainerId = it.getString("trainerId") ?: "",
                        clientId = it.getString("clientId") ?: "",
                        clientName = it.getString("clientName") ?: "",
                        overallScore = (it.getDouble("overallScore") ?: 0.0).toFloat(),
                        workoutFrequency = (it.getDouble("workoutFrequency") ?: 0.0).toFloat(),
                        formScore = (it.getDouble("formScore") ?: 0.0).toFloat(),
                        consistency = (it.getDouble("consistency") ?: 0.0).toFloat(),
                        engagement = (it.getDouble("engagement") ?: 0.0).toFloat(),
                        dropoutRisk = (it.getDouble("dropoutRisk") ?: 0.0).toFloat(),
                        sessionCount = it.getLong("sessionCount")?.toInt() ?: 0,
                        lastWorkoutAt = it.getLong("lastWorkoutAt") ?: 0L,
                        updatedAt = it.getLong("updatedAt") ?: 0L
                    )
                }
                trySend(snap)
            }
        awaitClose { reg.remove() }
    }

    // ── Client workout sessions ────────────────────────────────────────────

    fun getClientWorkoutSessions(clientId: String, limit: Long = 20): Flow<List<Map<String, Any>>> = callbackFlow {
        val reg = workoutSessions
            .whereEqualTo("userId", clientId)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(limit)
            .addSnapshotListener { snap, err ->
                if (err != null) { trySend(emptyList()); return@addSnapshotListener }
                val sessions = snap?.documents?.mapNotNull { doc -> doc.data } ?: emptyList()
                trySend(sessions)
            }
        awaitClose { reg.remove() }
    }

    // ── Accept invite (called from client's side) ──────────────────────────

    suspend fun acceptInvite(inviteId: String, clientId: String, clientName: String): Result<Unit> =
        runCatching {
            val inviteDoc = trainerInvites.document(inviteId).get().await()
            val trainerId = inviteDoc.getString("trainerId") ?: error("Invalid invite")
            val clientEmail = inviteDoc.getString("email") ?: ""
            trainerInvites.document(inviteId).update("status", "accepted").await()
            val relationId = "${trainerId}_$clientId"
            trainerClients.document(relationId).set(
                mapOf(
                    "trainerId" to trainerId,
                    "clientId" to clientId,
                    "clientEmail" to clientEmail,
                    "clientName" to clientName,
                    "status" to TrainerClient.Status.ACTIVE,
                    "grantedAt" to System.currentTimeMillis(),
                    "lastActiveAt" to System.currentTimeMillis(),
                    "dropoutRisk" to 0.0,
                    "overallScore" to 0.0
                )
            ).await()
        }

    // ── Remove client ──────────────────────────────────────────────────────

    suspend fun removeClient(trainerId: String, clientId: String) {
        runCatching {
            val relationId = "${trainerId}_$clientId"
            trainerClients.document(relationId).update("status", TrainerClient.Status.REMOVED).await()
        }
    }

    // ── Update trainer profile in users collection ─────────────────────────

    suspend fun updateTrainerProfile(userId: String, bio: String, specialization: String, certifications: String) {
        runCatching {
            fs.collection("users").document(userId).update(
                mapOf(
                    "role" to "trainer",
                    "bio" to bio,
                    "specialization" to specialization,
                    "certifications" to certifications,
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()
        }
    }

    // ── On-device alert generation (called from client WorkoutViewModel) ───

    suspend fun generateAlerts(trainerId: String, clientId: String, clientName: String,
                               daysSinceLastWorkout: Int, currentFormScore: Float, previousFormScore: Float) {
        runCatching {
            val now = System.currentTimeMillis()
            if (daysSinceLastWorkout >= 7) {
                val alertId = "${trainerId}_${clientId}_missed_${now / (1000 * 60 * 60 * 24)}"
                trainerAlerts.document(alertId).set(
                    mapOf(
                        "trainerId" to trainerId,
                        "clientId" to clientId,
                        "clientName" to clientName,
                        "type" to TrainerAlert.Type.MISSED_WORKOUT,
                        "message" to "$clientName hasn't worked out in $daysSinceLastWorkout days",
                        "createdAt" to now,
                        "isRead" to false
                    )
                ).await()
            }
            if (previousFormScore > 0 && currentFormScore < previousFormScore - 15) {
                val alertId = "${trainerId}_${clientId}_form_${now / (1000 * 60 * 60 * 24)}"
                trainerAlerts.document(alertId).set(
                    mapOf(
                        "trainerId" to trainerId,
                        "clientId" to clientId,
                        "clientName" to clientName,
                        "type" to TrainerAlert.Type.FORM_REGRESSION,
                        "message" to "$clientName's form score dropped by ${(previousFormScore - currentFormScore).toInt()} points",
                        "createdAt" to now,
                        "isRead" to false
                    )
                ).await()
            }
        }
    }
}
