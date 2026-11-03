package com.jsf.app.pravin.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class PublicUserProfile(
    val userId: String = "",
    val name: String = "",
    val fitnessLevel: String = "",
    val totalWorkouts: Int = 0
)

data class FriendRequest(
    val id: String = "",
    val fromUserId: String = "",
    val fromName: String = "",
    val toUserId: String = "",
    val status: String = "pending",
    val createdAt: Long = 0L
) {
    object Status {
        const val PENDING = "pending"
        const val ACCEPTED = "accepted"
        const val DECLINED = "declined"
    }
}

data class Friendship(
    val id: String = "",
    val userId1: String = "",
    val userId2: String = "",
    val createdAt: Long = 0L,
    val friendName: String = "",
    val friendFitnessLevel: String = ""
)

class FriendsDataSource {
    private val fs = FirebaseFirestore.getInstance()
    private val users get() = fs.collection("users")
    private val friendRequests get() = fs.collection("friend_requests")
    private val friendships get() = fs.collection("friendships")

    suspend fun syncUserProfile(userId: String, name: String, fitnessLevel: String) {
        runCatching {
            users.document(userId).set(
                mapOf(
                    "userId" to userId,
                    "name" to name,
                    "fitnessLevel" to fitnessLevel,
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()
        }
    }

    fun searchUsers(query: String, currentUserId: String): Flow<List<PublicUserProfile>> = callbackFlow {
        if (query.length < 2) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val reg = users
            .orderBy("name")
            .startAt(query)
            .endAt(query + "")
            .limit(20)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    android.util.Log.w("Friends", "Search error: ${err.message}")
                    return@addSnapshotListener
                }
                val results = snap?.documents?.mapNotNull { doc ->
                    val uid = doc.getString("userId") ?: return@mapNotNull null
                    if (uid == currentUserId) return@mapNotNull null
                    PublicUserProfile(
                        userId = uid,
                        name = doc.getString("name") ?: "",
                        fitnessLevel = doc.getString("fitnessLevel") ?: "",
                        totalWorkouts = doc.getLong("totalWorkouts")?.toInt() ?: 0
                    )
                } ?: emptyList()
                trySend(results)
            }
        awaitClose { reg.remove() }
    }

    fun getReceivedRequests(userId: String): Flow<List<FriendRequest>> = callbackFlow {
        val reg = friendRequests
            .whereEqualTo("toUserId", userId)
            .whereEqualTo("status", FriendRequest.Status.PENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    android.util.Log.w("Friends", "Requests error: ${err.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val reqs = snap?.documents?.mapNotNull { doc ->
                    FriendRequest(
                        id = doc.id,
                        fromUserId = doc.getString("fromUserId") ?: return@mapNotNull null,
                        fromName = doc.getString("fromName") ?: "",
                        toUserId = doc.getString("toUserId") ?: return@mapNotNull null,
                        status = doc.getString("status") ?: FriendRequest.Status.PENDING,
                        createdAt = doc.getLong("createdAt") ?: 0L
                    )
                } ?: emptyList()
                trySend(reqs)
            }
        awaitClose { reg.remove() }
    }

    fun getSentRequests(userId: String): Flow<List<FriendRequest>> = callbackFlow {
        val reg = friendRequests
            .whereEqualTo("fromUserId", userId)
            .whereEqualTo("status", FriendRequest.Status.PENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    android.util.Log.w("Friends", "Sent requests error: ${err.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val reqs = snap?.documents?.mapNotNull { doc ->
                    FriendRequest(
                        id = doc.id,
                        fromUserId = doc.getString("fromUserId") ?: return@mapNotNull null,
                        fromName = doc.getString("fromName") ?: "",
                        toUserId = doc.getString("toUserId") ?: return@mapNotNull null,
                        status = doc.getString("status") ?: FriendRequest.Status.PENDING,
                        createdAt = doc.getLong("createdAt") ?: 0L
                    )
                } ?: emptyList()
                trySend(reqs)
            }
        awaitClose { reg.remove() }
    }

    fun getFriends(userId: String): Flow<List<Friendship>> = callbackFlow {
        val reg = friendships
            .whereArrayContains("userIds", userId)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    android.util.Log.w("Friends", "Friends list error: ${err.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val friends = snap?.documents?.mapNotNull { doc ->
                    val userIds = (doc.get("userIds") as? List<*>) ?: return@mapNotNull null
                    val uid1 = userIds.getOrNull(0)?.toString() ?: return@mapNotNull null
                    val uid2 = userIds.getOrNull(1)?.toString() ?: return@mapNotNull null
                    val friendId = if (uid1 == userId) uid2 else uid1
                    val friendName = if (uid1 == userId) doc.getString("name2") ?: "" else doc.getString("name1") ?: ""
                    val friendLevel = if (uid1 == userId) doc.getString("level2") ?: "" else doc.getString("level1") ?: ""
                    Friendship(
                        id = doc.id,
                        userId1 = uid1,
                        userId2 = uid2,
                        createdAt = doc.getLong("createdAt") ?: 0L,
                        friendName = friendName,
                        friendFitnessLevel = friendLevel
                    )
                } ?: emptyList()
                trySend(friends)
            }
        awaitClose { reg.remove() }
    }

    suspend fun sendFriendRequest(fromUserId: String, fromName: String, toUserId: String) {
        runCatching {
            val reqId = "${fromUserId}_$toUserId"
            friendRequests.document(reqId).set(
                mapOf(
                    "fromUserId" to fromUserId,
                    "fromName" to fromName,
                    "toUserId" to toUserId,
                    "status" to FriendRequest.Status.PENDING,
                    "createdAt" to System.currentTimeMillis()
                )
            ).await()
        }
    }

    suspend fun acceptRequest(
        requestId: String,
        fromUserId: String, fromName: String, fromLevel: String,
        toUserId: String, toName: String, toLevel: String
    ) {
        runCatching {
            friendRequests.document(requestId)
                .update("status", FriendRequest.Status.ACCEPTED).await()
            val friendshipId = if (fromUserId < toUserId) "${fromUserId}_$toUserId" else "${toUserId}_$fromUserId"
            friendships.document(friendshipId).set(
                mapOf(
                    "userIds" to listOf(fromUserId, toUserId),
                    "name1" to fromName, "level1" to fromLevel,
                    "name2" to toName, "level2" to toLevel,
                    "createdAt" to System.currentTimeMillis()
                )
            ).await()
        }
    }

    suspend fun declineRequest(requestId: String) {
        runCatching {
            friendRequests.document(requestId)
                .update("status", FriendRequest.Status.DECLINED).await()
        }
    }

    suspend fun removeFriend(friendshipId: String) {
        runCatching {
            friendships.document(friendshipId).delete().await()
        }
    }
}
