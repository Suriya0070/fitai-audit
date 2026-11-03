package com.jsf.app.pravin.data.db

import androidx.room.*
import com.jsf.app.pravin.data.model.LiveSession
import kotlinx.coroutines.flow.Flow

@Dao
interface LiveSessionDao {
    @Query("SELECT * FROM live_sessions WHERE serverId = :serverId ORDER BY scheduledAt DESC")
    fun getLiveSessionsForServer(serverId: String): Flow<List<LiveSession>>

    @Query("SELECT * FROM live_sessions WHERE serverId = :serverId AND status = 'LIVE' LIMIT 1")
    fun getActiveLiveSession(serverId: String): Flow<LiveSession?>

    @Query("SELECT * FROM live_sessions WHERE serverId = :serverId AND status = 'SCHEDULED' AND scheduledAt >= :now ORDER BY scheduledAt ASC")
    fun getUpcomingSessions(serverId: String, now: Long): Flow<List<LiveSession>>

    @Query("SELECT * FROM live_sessions WHERE id = :sessionId LIMIT 1")
    fun getLiveSessionById(sessionId: String): Flow<LiveSession?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLiveSession(session: LiveSession)

    @Query("UPDATE live_sessions SET status = :status, startedAt = :startedAt WHERE id = :sessionId")
    suspend fun startSession(sessionId: String, status: String, startedAt: Long)

    @Query("UPDATE live_sessions SET status = 'ENDED', endedAt = :endedAt WHERE id = :sessionId")
    suspend fun endSession(sessionId: String, endedAt: Long)

    @Query("UPDATE live_sessions SET participantCount = :count WHERE id = :sessionId")
    suspend fun updateParticipantCount(sessionId: String, count: Int)
}
