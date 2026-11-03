package com.jsf.app.pravin.ui.screens.community

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jsf.app.pravin.data.db.FitnessDatabase
import com.jsf.app.pravin.data.model.Comment
import com.jsf.app.pravin.data.model.Post
import com.jsf.app.pravin.data.model.Reaction
import com.jsf.app.pravin.data.preferences.UserPreferences
import com.jsf.app.pravin.data.remote.FirestoreCommunityDataSource
import com.jsf.app.pravin.data.repository.CommunityRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class FeedUiState(
    val posts: List<Post> = emptyList(),
    val reactionsMap: Map<String, List<Reaction>> = emptyMap(),
    val commentsMap: Map<String, List<Comment>> = emptyMap(),
    val isLoading: Boolean = true,
    val currentUserId: String = ""
)

class CommunityFeedViewModel(
    application: Application,
    private val channelId: String
) : AndroidViewModel(application) {

    private val db = FitnessDatabase.getInstance(application)
    private val communityRepo = CommunityRepository(db)
    private val firestoreSource = FirestoreCommunityDataSource()
    private val prefs = UserPreferences(application)

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val uid = prefs.currentUserId.first()
            _uiState.update { it.copy(currentUserId = uid) }
        }

        // Firestore is the source of truth for posts — Room is kept as an offline cache.
        viewModelScope.launch {
            firestoreSource.getPostsForChannel(channelId)
                .catch { android.util.Log.w("Feed", "Firestore posts error: ${it.message}") }
                .collect { remotePosts ->
                    // Upsert into Room for offline access
                    remotePosts.forEach { db.postDao().upsertPost(it) }
                    _uiState.update { it.copy(posts = remotePosts, isLoading = false) }
                }
        }

        // Fall back to Room while Firestore is loading (or offline)
        viewModelScope.launch {
            db.postDao().getPostsForChannel(channelId)
                .collect { localPosts ->
                    if (_uiState.value.isLoading) {
                        _uiState.update { it.copy(posts = localPosts, isLoading = false) }
                    }
                }
        }

        // Reactions and comments: Firestore for connected users, Room for offline
        viewModelScope.launch {
            _uiState.flatMapLatest { state ->
                val postIds = state.posts.map { it.id }
                if (postIds.isEmpty()) flowOf(Pair(emptyList<Reaction>(), emptyList<Comment>()))
                else combine(
                    db.reactionDao().getReactionsForPosts(postIds),
                    db.commentDao().getCommentsForPosts(postIds)
                ) { reactions, comments -> Pair(reactions, comments) }
            }.collect { (reactions, comments) ->
                _uiState.update { it.copy(
                    reactionsMap = reactions.groupBy { it.postId },
                    commentsMap = comments.groupBy { it.postId }
                ) }
            }
        }
    }

    fun createPost(content: String, type: String, exerciseType: String = "") {
        if (content.isBlank()) return
        val state = _uiState.value
        viewModelScope.launch {
            val uid = state.currentUserId
            val channelData = communityRepo.getChannelById(channelId).first()
            val post = Post(
                id = UUID.randomUUID().toString(),
                channelId = channelId,
                serverId = channelData?.serverId ?: "",
                authorId = uid,
                type = type,
                content = content.trim(),
                exerciseType = exerciseType.trim(),
                timestamp = System.currentTimeMillis()
            )
            // Write to Firestore first; the snapshot listener above will push it to Room.
            if (uid.startsWith("local_")) {
                db.postDao().upsertPost(post)
            } else {
                firestoreSource.savePost(post)
            }
        }
    }

    fun toggleReaction(postId: String, emoji: String) {
        val uid = _uiState.value.currentUserId
        viewModelScope.launch {
            val existing = db.reactionDao().getUserReaction(postId, uid, emoji)
            if (existing != null) {
                db.reactionDao().removeReaction(postId, uid, emoji)
                db.postDao().updateLikeCount(postId, -1)
                if (!uid.startsWith("local_")) {
                    firestoreSource.removeReaction(postId, uid, emoji)
                    firestoreSource.incrementPostLike(postId, -1)
                }
            } else {
                val reaction = Reaction(postId = postId, userId = uid, emoji = emoji)
                db.reactionDao().insertReaction(reaction)
                db.postDao().updateLikeCount(postId, 1)
                if (!uid.startsWith("local_")) {
                    firestoreSource.addReaction(reaction)
                    firestoreSource.incrementPostLike(postId, 1)
                }
            }
        }
    }

    fun addComment(postId: String, content: String) {
        if (content.isBlank()) return
        val uid = _uiState.value.currentUserId
        viewModelScope.launch {
            val comment = Comment(
                id = UUID.randomUUID().toString(),
                postId = postId,
                authorId = uid,
                content = content.trim(),
                timestamp = System.currentTimeMillis()
            )
            db.commentDao().insertComment(comment)
            db.postDao().updateCommentCount(postId, 1)
            if (!uid.startsWith("local_")) {
                firestoreSource.saveComment(comment)
                firestoreSource.incrementPostComment(postId, 1)
            }
        }
    }

    companion object {
        fun factory(application: Application, channelId: String) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    CommunityFeedViewModel(application, channelId) as T
            }
    }
}
