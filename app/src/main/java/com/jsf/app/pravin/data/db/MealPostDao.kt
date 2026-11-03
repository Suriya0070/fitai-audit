package com.jsf.app.pravin.data.db

import androidx.room.*
import com.jsf.app.pravin.data.model.MealPost
import kotlinx.coroutines.flow.Flow

@Dao
interface MealPostDao {
    @Query("SELECT * FROM meal_posts WHERE channelId = :channelId ORDER BY timestamp DESC")
    fun getMealPostsForChannel(channelId: String): Flow<List<MealPost>>

    @Query("SELECT * FROM meal_posts WHERE serverId = :serverId ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentMealPosts(serverId: String, limit: Int = 50): Flow<List<MealPost>>

    @Query("SELECT * FROM meal_posts WHERE authorId = :userId ORDER BY timestamp DESC")
    fun getMealsByUser(userId: String): Flow<List<MealPost>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMealPost(mealPost: MealPost)

    @Delete
    suspend fun deleteMealPost(mealPost: MealPost)

    @Query("UPDATE meal_posts SET likeCount = likeCount + :delta WHERE id = :postId")
    suspend fun updateLikeCount(postId: String, delta: Int)

    @Query("UPDATE meal_posts SET aiAnalysis = :analysis WHERE id = :postId")
    suspend fun updateAiAnalysis(postId: String, analysis: String)
}
