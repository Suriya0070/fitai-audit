package com.jsf.app.pravin.data.db

import androidx.room.*
import com.jsf.app.pravin.data.model.Comment
import kotlinx.coroutines.flow.Flow

@Dao
interface CommentDao {
    @Query("SELECT * FROM comments WHERE postId = :postId ORDER BY timestamp ASC")
    fun getCommentsForPost(postId: String): Flow<List<Comment>>

    @Query("SELECT COUNT(*) FROM comments WHERE postId = :postId")
    fun getCommentCount(postId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: Comment)

    @Delete
    suspend fun deleteComment(comment: Comment)

    @Query("UPDATE comments SET likeCount = likeCount + :delta WHERE id = :commentId")
    suspend fun updateLikeCount(commentId: String, delta: Int)

    @Query("SELECT * FROM comments WHERE postId IN (:postIds) ORDER BY timestamp ASC")
    fun getCommentsForPosts(postIds: List<String>): Flow<List<Comment>>
}
