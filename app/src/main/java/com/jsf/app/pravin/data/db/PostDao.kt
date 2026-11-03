package com.jsf.app.pravin.data.db

import androidx.room.*
import com.jsf.app.pravin.data.model.Post
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {
    @Query("SELECT * FROM posts WHERE channelId = :channelId ORDER BY timestamp DESC")
    fun getPostsForChannel(channelId: String): Flow<List<Post>>

    @Query("SELECT * FROM posts WHERE serverId = :serverId ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentPostsForServer(serverId: String, limit: Int = 50): Flow<List<Post>>

    @Query("SELECT * FROM posts WHERE authorId = :userId ORDER BY timestamp DESC")
    fun getPostsByUser(userId: String): Flow<List<Post>>

    @Query("SELECT * FROM posts WHERE id = :postId LIMIT 1")
    suspend fun getPostById(postId: String): Post?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPost(post: Post)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPosts(posts: List<Post>)

    @Delete
    suspend fun deletePost(post: Post)

    @Query("UPDATE posts SET likeCount = likeCount + :delta WHERE id = :postId")
    suspend fun updateLikeCount(postId: String, delta: Int)

    @Query("UPDATE posts SET commentCount = commentCount + :delta WHERE id = :postId")
    suspend fun updateCommentCount(postId: String, delta: Int)
}
