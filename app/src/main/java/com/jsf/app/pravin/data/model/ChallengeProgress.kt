package com.jsf.app.pravin.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "challenge_progress",
    indices = [Index(value = ["challengeId", "userId"], unique = true)]
)
data class ChallengeProgress(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val challengeId: String,
    val userId: String,
    val completedDays: Int = 0,
    val totalReps: Int = 0,
    val currentStreak: Int = 0,
    val lastActivityDate: Long = 0L,
    val isCompleted: Boolean = false
)
