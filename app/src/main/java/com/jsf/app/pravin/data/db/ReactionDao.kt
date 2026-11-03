package com.jsf.app.pravin.data.db

import androidx.room.*
import com.jsf.app.pravin.data.model.Reaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ReactionDao {
    @Query("SELECT * FROM reactions WHERE postId = :postId")
    fun getReactionsForPost(postId: String): Flow<List<Reaction>>

    @Query("SELECT * FROM reactions WHERE messageId = :messageId")
    fun getReactionsForMessage(messageId: String): Flow<List<Reaction>>

    @Query("SELECT * FROM reactions WHERE postId = :postId AND userId = :userId AND emoji = :emoji LIMIT 1")
    suspend fun getUserReaction(postId: String, userId: String, emoji: String): Reaction?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertReaction(reaction: Reaction)

    @Query("DELETE FROM reactions WHERE postId = :postId AND userId = :userId AND emoji = :emoji")
    suspend fun removeReaction(postId: String, userId: String, emoji: String)

    @Query("SELECT COUNT(*) FROM reactions WHERE postId = :postId AND emoji = :emoji")
    fun getReactionCount(postId: String, emoji: String): Flow<Int>

    @Query("SELECT * FROM reactions WHERE postId IN (:postIds)")
    fun getReactionsForPosts(postIds: List<String>): Flow<List<Reaction>>
}
