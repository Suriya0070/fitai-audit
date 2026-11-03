package com.jsf.app.pravin.data.db

import androidx.room.*
import com.jsf.app.pravin.data.model.Achievement
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements WHERE userId = :userId ORDER BY earnedAt DESC")
    fun getAchievementsForUser(userId: String): Flow<List<Achievement>>

    @Query("SELECT * FROM achievements WHERE userId = :userId AND type = :type LIMIT 1")
    suspend fun getAchievementByType(userId: String, type: String): Achievement?

    @Query("SELECT COUNT(*) FROM achievements WHERE userId = :userId")
    fun getAchievementCount(userId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAchievement(achievement: Achievement): Long
}
