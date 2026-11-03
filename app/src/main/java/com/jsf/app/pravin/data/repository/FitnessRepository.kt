package com.jsf.app.pravin.data.repository

import com.jsf.app.pravin.data.db.FitnessDatabase
import com.jsf.app.pravin.data.model.Achievement
import com.jsf.app.pravin.data.model.UserProfile
import com.jsf.app.pravin.data.model.WorkoutSession
import kotlinx.coroutines.flow.Flow

class FitnessRepository(private val db: FitnessDatabase) {
    private val profileDao = db.userProfileDao()
    private val sessionDao = db.workoutSessionDao()
    private val achievementDao = db.achievementDao()

    val userProfile: Flow<UserProfile?> = profileDao.getProfile()
    val allSessions: Flow<List<WorkoutSession>> = sessionDao.getAllSessions()
    fun recentSessions(n: Int): Flow<List<WorkoutSession>> = sessionDao.getRecentSessions(n)

    suspend fun saveProfile(profile: UserProfile) = profileDao.upsertProfile(profile)
    suspend fun saveSession(session: WorkoutSession): Long = sessionDao.insertSession(session)
    suspend fun getSessionsSince(since: Long) = sessionDao.getSessionsSince(since)
    suspend fun getAllTimestamps() = sessionDao.getAllTimestamps()

    fun getAchievementsForUser(userId: String): Flow<List<Achievement>> =
        achievementDao.getAchievementsForUser(userId)

    suspend fun getAchievementByType(userId: String, type: String): Achievement? =
        achievementDao.getAchievementByType(userId, type)

    suspend fun insertAchievement(achievement: Achievement) =
        achievementDao.insertAchievement(achievement)
}
