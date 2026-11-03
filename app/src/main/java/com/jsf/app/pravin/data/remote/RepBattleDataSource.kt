package com.jsf.app.pravin.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

data class RepBattle(
    val id: String = "",
    val serverId: String = "",
    val createdBy: String = "",
    val creatorName: String = "",
    val exercise: String = "Squats",
    val setsCount: Int = 3,
    val status: String = "active",
    val createdAt: Long = 0L,
    val endsAt: Long = 0L
) {
    object Status {
        const val ACTIVE = "active"
        const val ENDED = "ended"
    }
}

data class RepBattleEntry(
    val id: String = "",
    val battleId: String = "",
    val userId: String = "",
    val userName: String = "",
    val set1: Int = 0,
    val set2: Int = 0,
    val set3: Int = 0,
    val totalReps: Int = 0,
    val setsCompleted: Int = 0,
    val updatedAt: Long = 0L
)

class RepBattleDataSource {
    private val fs = FirebaseFirestore.getInstance()
    private val battles get() = fs.collection("rep_battles")
    private val entries get() = fs.collection("rep_battle_entries")

    suspend fun createBattle(
        serverId: String, createdBy: String, creatorName: String,
        exercise: String, setsCount: Int, durationHours: Int
    ): String {
        val id = UUID.randomUUID().toString().take(8)
        val now = System.currentTimeMillis()
        battles.document(id).set(
            mapOf(
                "serverId" to serverId,
                "createdBy" to createdBy,
                "creatorName" to creatorName,
                "exercise" to exercise,
                "setsCount" to setsCount,
                "status" to RepBattle.Status.ACTIVE,
                "createdAt" to now,
                "endsAt" to (now + durationHours * 3_600_000L)
            )
        ).await()
        return id
    }

    fun getActiveBattlesForServer(serverId: String): Flow<List<RepBattle>> = callbackFlow {
        val reg = battles
            .whereEqualTo("serverId", serverId)
            .whereEqualTo("status", RepBattle.Status.ACTIVE)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    android.util.Log.w("RepBattle", "Battles error: ${err.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull { doc ->
                    RepBattle(
                        id = doc.id,
                        serverId = doc.getString("serverId") ?: return@mapNotNull null,
                        createdBy = doc.getString("createdBy") ?: "",
                        creatorName = doc.getString("creatorName") ?: "",
                        exercise = doc.getString("exercise") ?: "Squats",
                        setsCount = doc.getLong("setsCount")?.toInt() ?: 3,
                        status = doc.getString("status") ?: RepBattle.Status.ACTIVE,
                        createdAt = doc.getLong("createdAt") ?: 0L,
                        endsAt = doc.getLong("endsAt") ?: 0L
                    )
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    fun getLeaderboard(battleId: String): Flow<List<RepBattleEntry>> = callbackFlow {
        val reg = entries
            .whereEqualTo("battleId", battleId)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    android.util.Log.w("RepBattle", "Leaderboard error: ${err.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val board = snap?.documents?.mapNotNull { doc ->
                    RepBattleEntry(
                        id = doc.id,
                        battleId = doc.getString("battleId") ?: return@mapNotNull null,
                        userId = doc.getString("userId") ?: return@mapNotNull null,
                        userName = doc.getString("userName") ?: "",
                        set1 = doc.getLong("set1")?.toInt() ?: 0,
                        set2 = doc.getLong("set2")?.toInt() ?: 0,
                        set3 = doc.getLong("set3")?.toInt() ?: 0,
                        totalReps = doc.getLong("totalReps")?.toInt() ?: 0,
                        setsCompleted = doc.getLong("setsCompleted")?.toInt() ?: 0,
                        updatedAt = doc.getLong("updatedAt") ?: 0L
                    )
                }?.sortedByDescending { it.totalReps } ?: emptyList()
                trySend(board)
            }
        awaitClose { reg.remove() }
    }

    suspend fun submitSet(battleId: String, userId: String, userName: String, setNumber: Int, reps: Int) {
        val entryId = "${battleId}_$userId"
        val docRef = entries.document(entryId)
        runCatching {
            val existing = docRef.get().await()
            val set1 = if (setNumber == 1) reps else existing.getLong("set1")?.toInt() ?: 0
            val set2 = if (setNumber == 2) reps else existing.getLong("set2")?.toInt() ?: 0
            val set3 = if (setNumber == 3) reps else existing.getLong("set3")?.toInt() ?: 0
            val setsCompleted = maxOf(existing.getLong("setsCompleted")?.toInt() ?: 0, setNumber)
            docRef.set(
                mapOf(
                    "battleId" to battleId,
                    "userId" to userId,
                    "userName" to userName,
                    "set1" to set1, "set2" to set2, "set3" to set3,
                    "totalReps" to (set1 + set2 + set3),
                    "setsCompleted" to setsCompleted,
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()
        }
    }

    suspend fun endBattle(battleId: String) {
        runCatching {
            battles.document(battleId).update("status", RepBattle.Status.ENDED).await()
        }
    }
}
