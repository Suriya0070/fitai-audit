package com.jsf.app.pravin.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_routines")
data class WorkoutRoutine(
    @PrimaryKey val id: String,
    val serverId: String,
    val createdBy: String,
    val name: String,
    val description: String = "",
    val scheduledDate: Long = 0L,
    val scheduledTime: String = "",
    val difficulty: String = "Beginner",
    val targetMuscleGroups: String = "",
    val estimatedDuration: Int = 30,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
