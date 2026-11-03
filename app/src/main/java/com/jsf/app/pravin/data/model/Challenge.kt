package com.jsf.app.pravin.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "challenges")
data class Challenge(
    @PrimaryKey val id: String,
    val serverId: String,
    val createdBy: String,
    val name: String,
    val description: String = "",
    val rules: String = "",
    val targetExercise: String = "",
    val targetReps: Int = 0,
    val targetDays: Int = 30,
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long = 0L,
    val isActive: Boolean = true,
    val participantCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
