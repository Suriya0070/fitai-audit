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
    private val prefs = UserPreferences(application)

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val uid = prefs.currentUserId.first()
            _uiState.update { it.copy(currentUserId = uid) }
        }

        viewModelScope.launch {
            db.postDao().getPostsForChannel(channelId)
                .flatMapLatest { posts ->
                    _uiState.update { it.copy(posts = posts, isLoading = false) }
                    val postIds = posts.map { it.id }
                    if (postIds.isEmpty()) {
                        flowOf(Pair(emptyList<Reaction>(), emptyList<Comment>()))
                    } else {
                        combine(
                            db.reactionDao().getReactionsForPosts(postIds),
                            db.commentDao().getCommentsForPosts(postIds)
                        ) { reactions, comments -> Pair(reactions, comments) }
                    }
                }
                .collect { (reactions, comments) ->
                    val reactionsMap = reactions.groupBy { it.postId }
                    val commentsMap = comments.groupBy { it.postId }
                    _uiState.update { it.copy(reactionsMap = reactionsMap, commentsMap = commentsMap) }
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
            db.postDao().upsertPost(post)
        }
    }

    fun toggleReaction(postId: String, emoji: String) {
        val uid = _uiState.value.currentUserId
        viewModelScope.launch {
            val existing = db.reactionDao().getUserReaction(postId, uid, emoji)
            if (existing != null) {
                db.reactionDao().removeReaction(postId, uid, emoji)
                db.postDao().updateLikeCount(postId, -1)
            } else {
                db.reactionDao().insertReaction(
                    Reaction(postId = postId, userId = uid, emoji = emoji)
                )
                db.postDao().updateLikeCount(postId, 1)
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
