package com.jsf.app.pravin.data.db

import androidx.room.*
import com.jsf.app.pravin.data.model.NutritionLog
import kotlinx.coroutines.flow.Flow

@Dao
interface NutritionLogDao {

    @Query("SELECT * FROM nutrition_logs WHERE userId = :userId ORDER BY timestamp DESC LIMIT :limit")
    fun getLogsForUser(userId: String, limit: Int = 200): Flow<List<NutritionLog>>

    @Query("""
        SELECT * FROM nutrition_logs
        WHERE userId = :userId AND timestamp >= :startMs AND timestamp < :endMs
        ORDER BY timestamp ASC
    """)
    fun getLogsForDay(userId: String, startMs: Long, endMs: Long): Flow<List<NutritionLog>>

    @Query("""
        SELECT * FROM nutrition_logs
        WHERE userId = :userId AND timestamp >= :startMs AND timestamp < :endMs
        ORDER BY timestamp ASC
    """)
    suspend fun getLogsForDaySync(userId: String, startMs: Long, endMs: Long): List<NutritionLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(log: NutritionLog)

    @Delete
    suspend fun delete(log: NutritionLog)

    @Query("UPDATE nutrition_logs SET imageUrl = :url WHERE id = :id")
    suspend fun updateImageUrl(id: String, url: String)

    @Query("""
        UPDATE nutrition_logs
        SET aiStatus = :status, recognizedFoods = :foods,
            estimatedCalories = :calories, estimatedProteinG = :proteinG,
            estimatedCarbsG = :carbsG, estimatedFatG = :fatG
        WHERE id = :id
    """)
    suspend fun updateAiResult(
        id: String, status: String, foods: String,
        calories: Int, proteinG: Float, carbsG: Float, fatG: Float
    )
}
