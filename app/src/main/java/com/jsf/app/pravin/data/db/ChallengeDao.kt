package com.jsf.app.pravin.data.db

import androidx.room.*
import com.jsf.app.pravin.data.model.Challenge
import com.jsf.app.pravin.data.model.ChallengeProgress
import kotlinx.coroutines.flow.Flow

@Dao
interface ChallengeDao {
    @Query("SELECT * FROM challenges WHERE serverId = :serverId AND isActive = 1 ORDER BY createdAt DESC")
    fun getChallengesForServer(serverId: String): Flow<List<Challenge>>

    @Query("SELECT * FROM challenges WHERE id = :challengeId LIMIT 1")
    fun getChallengeById(challengeId: String): Flow<Challenge?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChallenge(challenge: Challenge)

    @Query("UPDATE challenges SET isActive = 0 WHERE id = :challengeId")
    suspend fun deactivateChallenge(challengeId: String)

    @Query("UPDATE challenges SET participantCount = participantCount + 1 WHERE id = :challengeId")
    suspend fun incrementParticipant(challengeId: String)

    // ChallengeProgress queries
    @Query("SELECT * FROM challenge_progress WHERE challengeId = :challengeId ORDER BY totalReps DESC")
    fun getLeaderboard(challengeId: String): Flow<List<ChallengeProgress>>

    @Query("SELECT * FROM challenge_progress WHERE challengeId = :challengeId AND userId = :userId LIMIT 1")
    fun getUserProgress(challengeId: String, userId: String): Flow<ChallengeProgress?>

    @Query("SELECT * FROM challenge_progress WHERE userId = :userId")
    fun getUserChallenges(userId: String): Flow<List<ChallengeProgress>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgress(progress: ChallengeProgress)

    @Query("""
        UPDATE challenge_progress
        SET completedDays = :completedDays, totalReps = :totalReps,
            currentStreak = :streak, lastActivityDate = :lastActivity, isCompleted = :completed
        WHERE challengeId = :challengeId AND userId = :userId
    """)
    suspend fun updateProgress(
        challengeId: String, userId: String,
        completedDays: Int, totalReps: Int, streak: Int,
        lastActivity: Long, completed: Boolean
    )
}
