package com.jsf.app.pravin.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

data class CallSignal(
    val callId: String,
    val hostId: String,
    val serverId: String,
    val offer: String,
    val answer: String?,
    val status: String
)

data class IceCandidateData(
    val sdpMid: String,
    val sdpMLineIndex: Int,
    val candidate: String
)

data class RepCountData(
    val userId: String,
    val repCount: Int,
    val exerciseType: String
)

class WebRtcSignalingService {

    private val fs = FirebaseFirestore.getInstance()
    private val calls get() = fs.collection("live_calls")

    suspend fun createCall(serverId: String, hostId: String, offerSdp: String): String {
        val callId = UUID.randomUUID().toString().take(8).uppercase()
        calls.document(callId).set(
            mapOf(
                "callId" to callId,
                "hostId" to hostId,
                "serverId" to serverId,
                "offer" to offerSdp,
                "answer" to null,
                "status" to "waiting",
                "createdAt" to System.currentTimeMillis()
            )
        ).await()
        fs.collection("servers").document(serverId)
            .set(mapOf("activeLiveCallId" to callId), SetOptions.merge())
            .await()
        return callId
    }

    suspend fun getCallSignal(callId: String): CallSignal? {
        val doc = calls.document(callId).get().await()
        if (!doc.exists()) return null
        return CallSignal(
            callId = doc.getString("callId") ?: callId,
            hostId = doc.getString("hostId") ?: "",
            serverId = doc.getString("serverId") ?: "",
            offer = doc.getString("offer") ?: "",
            answer = doc.getString("answer"),
            status = doc.getString("status") ?: "waiting"
        )
    }

    suspend fun saveAnswer(callId: String, answerSdp: String) {
        calls.document(callId).update(
            mapOf("answer" to answerSdp, "status" to "active")
        ).await()
    }

    fun listenForAnswer(callId: String): Flow<String?> = callbackFlow {
        val reg = calls.document(callId).addSnapshotListener { snap, err ->
            if (err != null) { close(err); return@addSnapshotListener }
            trySend(snap?.getString("answer"))
        }
        awaitClose { reg.remove() }
    }

    suspend fun addHostIceCandidate(callId: String, data: IceCandidateData) {
        calls.document(callId).collection("host_ice").add(data.toMap()).await()
    }

    suspend fun addGuestIceCandidate(callId: String, data: IceCandidateData) {
        calls.document(callId).collection("guest_ice").add(data.toMap()).await()
    }

    fun listenForHostIceCandidates(callId: String): Flow<IceCandidateData> = callbackFlow {
        val reg = calls.document(callId).collection("host_ice")
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                snap?.documentChanges?.forEach { change ->
                    val d = change.document
                    trySend(
                        IceCandidateData(
                            sdpMid = d.getString("sdpMid") ?: "",
                            sdpMLineIndex = d.getLong("sdpMLineIndex")?.toInt() ?: 0,
                            candidate = d.getString("candidate") ?: ""
                        )
                    )
                }
            }
        awaitClose { reg.remove() }
    }

    fun listenForGuestIceCandidates(callId: String): Flow<IceCandidateData> = callbackFlow {
        val reg = calls.document(callId).collection("guest_ice")
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                snap?.documentChanges?.forEach { change ->
                    val d = change.document
                    trySend(
                        IceCandidateData(
                            sdpMid = d.getString("sdpMid") ?: "",
                            sdpMLineIndex = d.getLong("sdpMLineIndex")?.toInt() ?: 0,
                            candidate = d.getString("candidate") ?: ""
                        )
                    )
                }
            }
        awaitClose { reg.remove() }
    }

    suspend fun updateRepCount(callId: String, userId: String, repCount: Int, exerciseType: String) {
        calls.document(callId).collection("rep_counts").document(userId)
            .set(
                mapOf(
                    "userId" to userId,
                    "repCount" to repCount,
                    "exerciseType" to exerciseType,
                    "updatedAt" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            ).await()
    }

    fun listenForRepCounts(callId: String): Flow<List<RepCountData>> = callbackFlow {
        val reg = calls.document(callId).collection("rep_counts")
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                val counts = snap?.documents?.mapNotNull { d ->
                    RepCountData(
                        userId = d.getString("userId") ?: return@mapNotNull null,
                        repCount = d.getLong("repCount")?.toInt() ?: 0,
                        exerciseType = d.getString("exerciseType") ?: ""
                    )
                } ?: emptyList()
                trySend(counts)
            }
        awaitClose { reg.remove() }
    }

    suspend fun endCall(callId: String, serverId: String) {
        runCatching { calls.document(callId).update("status", "ended").await() }
        runCatching {
            fs.collection("servers").document(serverId)
                .set(mapOf("activeLiveCallId" to null), SetOptions.merge())
                .await()
        }
    }

    suspend fun getActiveCallForServer(serverId: String): String? {
        val doc = fs.collection("servers").document(serverId).get().await()
        return doc.getString("activeLiveCallId")
    }

    private fun IceCandidateData.toMap() = mapOf(
        "sdpMid" to sdpMid,
        "sdpMLineIndex" to sdpMLineIndex,
        "candidate" to candidate
    )
}
