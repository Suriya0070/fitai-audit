package com.jsf.app.pravin.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_sessions")
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String = "",
    val serverId: String = "",
    val routineId: String = "",
    val isPersonal: Boolean = true,
    val timestamp: Long = System.currentTimeMillis(),
    val exerciseType: String = "Squats",
    val durationSeconds: Int = 0,
    val repCount: Int = 0,
    val totalScore: Float = 0f,
    val accuracyScore: Float = 0f,
    val balanceScore: Float = 0f,
    val stabilityScore: Float = 0f,
    val symmetryScore: Float = 0f,
    val speedScore: Float = 0f,
    val injuryRisk: Float = 0f,
    val fatigueLevel: Float = 0f,
    val environmentQuality: Float = 100f
)
