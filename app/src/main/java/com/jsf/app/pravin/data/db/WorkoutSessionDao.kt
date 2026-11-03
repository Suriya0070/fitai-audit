package com.jsf.app.pravin.data.db

import androidx.room.*
import com.jsf.app.pravin.data.model.WorkoutSession
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutSessionDao {
    @Query("SELECT * FROM workout_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_sessions ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentSessions(limit: Int): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_sessions WHERE timestamp >= :since ORDER BY timestamp ASC")
    suspend fun getSessionsSince(since: Long): List<WorkoutSession>

    @Query("SELECT COUNT(*) FROM workout_sessions")
    suspend fun getSessionCount(): Int

    @Insert
    suspend fun insertSession(session: WorkoutSession): Long

    @Query("SELECT timestamp FROM workout_sessions ORDER BY timestamp DESC")
    suspend fun getAllTimestamps(): List<Long>
}
