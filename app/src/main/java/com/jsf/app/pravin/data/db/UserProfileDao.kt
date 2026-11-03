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
}
