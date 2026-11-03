package com.jsf.app.pravin.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "routine_exercises")
data class RoutineExercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineId: String,
    val exerciseName: String,
    val sets: Int = 3,
    val targetReps: Int = 10,
    val targetDurationSeconds: Int = 0,
    val restSeconds: Int = 60,
    val instructions: String = "",
    val position: Int = 0,
    val isTimeBased: Boolean = false
)
