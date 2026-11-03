package com.jsf.app.pravin.data.db

import androidx.room.*
import com.jsf.app.pravin.data.model.UserProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profiles WHERE id = 1")
    fun getProfile(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profiles WHERE uid = :uid LIMIT 1")
    fun getProfileByUid(uid: String): Flow<UserProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: UserProfile)

    @Query("UPDATE user_profiles SET totalXp = totalXp + :xp, level = :newLevel WHERE id = 1")
    suspend fun addXp(xp: Long, newLevel: Int)

    @Query("SELECT * FROM user_profiles WHERE id = 1 LIMIT 1")
    suspend fun getProfileSync(): UserProfile?
}
