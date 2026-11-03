package com.jsf.app.pravin.data.remote

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.jsf.app.pravin.data.model.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreCommunityDataSource {

    private val fs = FirebaseFirestore.getInstance()

    private val servers get() = fs.collection("servers")
    private val channels get() = fs.collection("channels")
    private val messages get() = fs.collection("messages")
    private val members get() = fs.collection("server_members")
    private val posts get() = fs.collection("posts")
    private val reactions get() = fs.collection("post_reactions")
    private val comments get() = fs.collection("post_comments")

    // ── Servers ───────────────────────────────────────────────────────────────

    fun getPublicServers(): Flow<List<Server>> = collectionFlow(
        servers.whereEqualTo("isPublic", true)
    ) { docs -> docs.mapNotNull { it.toServer() }.sortedByDescending { it.memberCount } }

    fun getServerById(id: String): Flow<Server?> =
        documentFlow(servers.document(id)) { it.toServer() }

    fun getServersByOwner(ownerId: String): Flow<List<Server>> = collectionFlow(
        servers.whereEqualTo("ownerId", ownerId)
    ) { docs -> docs.mapNotNull { it.toServer() }.sortedByDescending { it.createdAt } }

    fun searchPublicServers(query: String): Flow<List<Server>> = collectionFlow(
        servers.whereEqualTo("isPublic", true)
    ) { docs ->
        val q = query.lowercase()
        docs.mapNotNull { it.toServer() }.filter { it.name.lowercase().contains(q) }
    }

    suspend fun saveServer(server: Server) {
        servers.document(server.id).set(server.toMap()).await()
    }

    suspend fun deleteServer(server: Server) {
        servers.document(server.id).delete().await()
    }

    // ── Channels ──────────────────────────────────────────────────────────────

    fun getChannelsForServer(serverId: String): Flow<List<Channel>> = collectionFlow(
        channels.whereEqualTo("serverId", serverId)
    ) { docs -> docs.mapNotNull { it.toChannel() }.sortedBy { it.position } }

    fun getChannelById(id: String): Flow<Channel?> =
        documentFlow(channels.document(id)) { it.toChannel() }

    suspend fun saveChannel(channel: Channel) {
        channels.document(channel.id).set(channel.toMap()).await()
    }

    suspend fun deleteChannel(channel: Channel) {
        channels.document(channel.id).delete().await()
    }

    // ── Messages ──────────────────────────────────────────────────────────────

    fun getMessagesForChannel(channelId: String): Flow<List<Message>> = collectionFlow(
        messages.whereEqualTo("channelId", channelId)
            .orderBy("timestamp")
            .limit(100)
    ) { docs ->
        docs.mapNotNull { it.toMessage() }.filter { !it.isDeleted }
    }

    fun getRecentMessages(channelId: String, limit: Int = 50): Flow<List<Message>> = collectionFlow(
        messages.whereEqualTo("channelId", channelId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit.toLong())
    ) { docs ->
        docs.mapNotNull { it.toMessage() }.filter { !it.isDeleted }.reversed()
    }

    suspend fun sendMessage(message: Message) {
        messages.document(message.id).set(message.toMap()).await()
    }

    suspend fun deleteMessage(messageId: String) {
        messages.document(messageId).update("isDeleted", true).await()
    }

    suspend fun editMessage(messageId: String, content: String, editedAt: Long) {
        messages.document(messageId)
            .update(mapOf("content" to content, "isEdited" to true, "editedAt" to editedAt))
            .await()
    }

    // ── Members ───────────────────────────────────────────────────────────────

    fun getMembersOfServer(serverId: String): Flow<List<ServerMember>> = collectionFlow(
        members.whereEqualTo("serverId", serverId)
    ) { docs -> docs.mapNotNull { it.toServerMember() }.filter { it.isActive } }

    fun getServersForUser(userId: String): Flow<List<ServerMember>> = collectionFlow(
        members.whereEqualTo("userId", userId)
    ) { docs -> docs.mapNotNull { it.toServerMember() }.filter { it.isActive } }

    fun getMembership(serverId: String, userId: String): Flow<ServerMember?> =
        documentFlow(members.document("${serverId}_${userId}")) { it.toServerMember() }

    suspend fun getUserRole(serverId: String, userId: String): String? = runCatching {
        members.document("${serverId}_${userId}").get().await().getString("role")
    }.getOrNull()

    suspend fun joinServer(member: ServerMember) {
        members.document("${member.serverId}_${member.userId}").set(member.toMap()).await()
    }

    suspend fun leaveServer(serverId: String, userId: String) {
        members.document("${serverId}_${userId}").update("isActive", false).await()
        runCatching {
            servers.document(serverId)
                .update("memberCount", com.google.firebase.firestore.FieldValue.increment(-1))
                .await()
        }
    }

    suspend fun updateMemberRole(serverId: String, userId: String, role: String) {
        members.document("${serverId}_${userId}").update("role", role).await()
    }

    // ── Posts ─────────────────────────────────────────────────────────────────

    fun getPostsForChannel(channelId: String, limit: Int = 50): Flow<List<com.jsf.app.pravin.data.model.Post>> =
        collectionFlow(
            posts.whereEqualTo("channelId", channelId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit.toLong())
        ) { docs -> docs.mapNotNull { it.toPost() }.reversed() }

    suspend fun savePost(post: com.jsf.app.pravin.data.model.Post) {
        posts.document(post.id).set(post.toPostMap()).await()
    }

    suspend fun deletePost(postId: String) {
        posts.document(postId).delete().await()
    }

    suspend fun incrementPostLike(postId: String, delta: Int) {
        posts.document(postId)
            .update("likeCount", com.google.firebase.firestore.FieldValue.increment(delta.toLong()))
            .await()
    }

    suspend fun incrementPostComment(postId: String, delta: Int) {
        posts.document(postId)
            .update("commentCount", com.google.firebase.firestore.FieldValue.increment(delta.toLong()))
            .await()
    }

    // ── Post Reactions ────────────────────────────────────────────────────────

    fun getReactionsForPost(postId: String): Flow<List<com.jsf.app.pravin.data.model.Reaction>> =
        collectionFlow(reactions.whereEqualTo("postId", postId)) { docs ->
            docs.mapNotNull { it.toReaction() }
        }

    suspend fun addReaction(reaction: com.jsf.app.pravin.data.model.Reaction) {
        val docId = "${reaction.postId}_${reaction.userId}_${reaction.emoji}"
        reactions.document(docId).set(reaction.toReactionMap()).await()
    }

    suspend fun removeReaction(postId: String, userId: String, emoji: String) {
        reactions.document("${postId}_${userId}_${emoji}").delete().await()
    }

    // ── Post Comments ─────────────────────────────────────────────────────────

    fun getCommentsForPost(postId: String): Flow<List<com.jsf.app.pravin.data.model.Comment>> =
        collectionFlow(
            comments.whereEqualTo("postId", postId)
                .orderBy("timestamp")
                .limit(100)
        ) { docs -> docs.mapNotNull { it.toComment() } }

    suspend fun saveComment(comment: com.jsf.app.pravin.data.model.Comment) {
        comments.document(comment.id).set(comment.toCommentMap()).await()
    }

    // ── Flow helpers ──────────────────────────────────────────────────────────

    private fun <T> collectionFlow(
        query: Query,
        transform: (List<DocumentSnapshot>) -> T
    ): Flow<T> = callbackFlow {
        val reg = query.addSnapshotListener { snap, err ->
            if (err != null) {
                android.util.Log.w("Firestore", "Query error: ${err.message}")
                return@addSnapshotListener  // don't crash — just skip this update
            }
            trySend(transform(snap?.documents ?: emptyList()))
        }
        awaitClose { reg.remove() }
    }

    private fun <T> documentFlow(
        ref: com.google.firebase.firestore.DocumentReference,
        transform: (DocumentSnapshot) -> T?
    ): Flow<T?> = callbackFlow {
        val reg = ref.addSnapshotListener { snap, err ->
            if (err != null) {
                android.util.Log.w("Firestore", "Document error: ${err.message}")
                return@addSnapshotListener  // don't crash — just skip this update
            }
            trySend(snap?.let { transform(it) })
        }
        awaitClose { reg.remove() }
    }

    // ── Converters ────────────────────────────────────────────────────────────

    private fun DocumentSnapshot.toServer() = runCatching {
        if (!exists()) return@runCatching null
        Server(
            id = id,
            name = getString("name") ?: return@runCatching null,
            description = getString("description") ?: "",
            iconUrl = getString("iconUrl") ?: "",
            bannerUrl = getString("bannerUrl") ?: "",
            ownerId = getString("ownerId") ?: "",
            isPublic = getBoolean("isPublic") ?: true,
            memberCount = getLong("memberCount")?.toInt() ?: 0,
            categories = getString("categories") ?: "[]",
            tags = getString("tags") ?: "[]",
            rules = getString("rules") ?: "",
            welcomeMessage = getString("welcomeMessage") ?: "",
            accentColor = getString("accentColor") ?: "#00D4FF",
            createdAt = getLong("createdAt") ?: 0L,
            updatedAt = getLong("updatedAt") ?: 0L
        )
    }.getOrNull()

    private fun DocumentSnapshot.toChannel() = runCatching {
        if (!exists()) return@runCatching null
        Channel(
            id = id,
            serverId = getString("serverId") ?: return@runCatching null,
            name = getString("name") ?: return@runCatching null,
            type = getString("type") ?: Channel.ChannelType.TEXT,
            description = getString("description") ?: "",
            position = getLong("position")?.toInt() ?: 0,
            isPublic = getBoolean("isPublic") ?: true,
            createdAt = getLong("createdAt") ?: 0L
        )
    }.getOrNull()

    private fun DocumentSnapshot.toMessage() = runCatching {
        if (!exists()) return@runCatching null
        Message(
            id = id,
            channelId = getString("channelId") ?: return@runCatching null,
            serverId = getString("serverId") ?: "",
            authorId = getString("authorId") ?: "",
            content = getString("content") ?: "",
            attachmentUrl = getString("attachmentUrl") ?: "",
            attachmentType = getString("attachmentType") ?: "",
            replyToId = getString("replyToId") ?: "",
            isEdited = getBoolean("isEdited") ?: false,
            isDeleted = getBoolean("isDeleted") ?: false,
            timestamp = getLong("timestamp") ?: 0L,
            editedAt = getLong("editedAt") ?: 0L
        )
    }.getOrNull()

    private fun DocumentSnapshot.toServerMember() = runCatching {
        if (!exists()) return@runCatching null
        ServerMember(
            serverId = getString("serverId") ?: return@runCatching null,
            userId = getString("userId") ?: return@runCatching null,
            role = getString("role") ?: ServerMember.Role.MEMBER,
            joinedAt = getLong("joinedAt") ?: 0L,
            nickname = getString("nickname") ?: "",
            isActive = getBoolean("isActive") ?: true
        )
    }.getOrNull()

    // ── toMap ─────────────────────────────────────────────────────────────────

    private fun Server.toMap() = mapOf(
        "name" to name, "description" to description, "iconUrl" to iconUrl,
        "bannerUrl" to bannerUrl, "ownerId" to ownerId, "isPublic" to isPublic,
        "memberCount" to memberCount, "categories" to categories, "tags" to tags,
        "rules" to rules, "welcomeMessage" to welcomeMessage, "accentColor" to accentColor,
        "createdAt" to createdAt, "updatedAt" to updatedAt
    )

    private fun Channel.toMap() = mapOf(
        "serverId" to serverId, "name" to name, "type" to type,
        "description" to description, "position" to position,
        "isPublic" to isPublic, "createdAt" to createdAt
    )

    private fun Message.toMap() = mapOf(
        "channelId" to channelId, "serverId" to serverId, "authorId" to authorId,
        "content" to content, "attachmentUrl" to attachmentUrl, "attachmentType" to attachmentType,
        "replyToId" to replyToId, "isEdited" to isEdited, "isDeleted" to isDeleted,
        "timestamp" to timestamp, "editedAt" to editedAt
    )

    private fun ServerMember.toMap() = mapOf(
        "serverId" to serverId, "userId" to userId, "role" to role,
        "joinedAt" to joinedAt, "nickname" to nickname, "isActive" to isActive
    )

    private fun DocumentSnapshot.toPost() = runCatching {
        if (!exists()) return@runCatching null
        com.jsf.app.pravin.data.model.Post(
            id = id,
            channelId = getString("channelId") ?: return@runCatching null,
            serverId = getString("serverId") ?: "",
            authorId = getString("authorId") ?: "",
            type = getString("type") ?: com.jsf.app.pravin.data.model.Post.PostType.TEXT,
            content = getString("content") ?: "",
            mediaUrl = getString("mediaUrl") ?: "",
            workoutSessionId = getLong("workoutSessionId") ?: 0L,
            exerciseType = getString("exerciseType") ?: "",
            repCount = getLong("repCount")?.toInt() ?: 0,
            totalScore = getDouble("totalScore")?.toFloat() ?: 0f,
            likeCount = getLong("likeCount")?.toInt() ?: 0,
            commentCount = getLong("commentCount")?.toInt() ?: 0,
            timestamp = getLong("timestamp") ?: 0L
        )
    }.getOrNull()

    private fun DocumentSnapshot.toReaction() = runCatching {
        if (!exists()) return@runCatching null
        com.jsf.app.pravin.data.model.Reaction(
            postId = getString("postId") ?: return@runCatching null,
            userId = getString("userId") ?: "",
            emoji = getString("emoji") ?: "",
            timestamp = getLong("timestamp") ?: 0L
        )
    }.getOrNull()

    private fun DocumentSnapshot.toComment() = runCatching {
        if (!exists()) return@runCatching null
        com.jsf.app.pravin.data.model.Comment(
            id = id,
            postId = getString("postId") ?: return@runCatching null,
            authorId = getString("authorId") ?: "",
            content = getString("content") ?: "",
            timestamp = getLong("timestamp") ?: 0L,
            likeCount = getLong("likeCount")?.toInt() ?: 0
        )
    }.getOrNull()

    private fun com.jsf.app.pravin.data.model.Post.toPostMap() = mapOf(
        "channelId" to channelId, "serverId" to serverId, "authorId" to authorId,
        "type" to type, "content" to content, "mediaUrl" to mediaUrl,
        "workoutSessionId" to workoutSessionId, "exerciseType" to exerciseType,
        "repCount" to repCount, "totalScore" to totalScore,
        "likeCount" to likeCount, "commentCount" to commentCount, "timestamp" to timestamp
    )

    private fun com.jsf.app.pravin.data.model.Reaction.toReactionMap() = mapOf(
        "postId" to postId, "userId" to userId, "emoji" to emoji, "timestamp" to timestamp
    )

    private fun com.jsf.app.pravin.data.model.Comment.toCommentMap() = mapOf(
        "postId" to postId, "authorId" to authorId, "content" to content,
        "timestamp" to timestamp, "likeCount" to likeCount
    )
}
