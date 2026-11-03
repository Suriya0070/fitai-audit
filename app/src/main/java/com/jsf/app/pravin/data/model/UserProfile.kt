package com.jsf.app.pravin.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val uid: String = "",
    val name: String = "",
    val age: Int = 25,
    val heightCm: Float = 170f,
    val weightKg: Float = 70f,
    val fitnessLevel: String = "Beginner",
    val gender: String = "Other",
    val bio: String = "",
    val avatarUrl: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
