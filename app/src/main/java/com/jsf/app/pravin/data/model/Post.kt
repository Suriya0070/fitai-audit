package com.jsf.app.pravin.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "posts")
data class Post(
    @PrimaryKey val id: String,
    val channelId: String,
    val serverId: String,
    val authorId: String,
    val type: String = "TEXT",
    val content: String = "",
    val mediaUrl: String = "",
    val workoutSessionId: Long = 0L,
    val exerciseType: String = "",
    val repCount: Int = 0,
    val totalScore: Float = 0f,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
) {
    object PostType {
        const val TEXT = "TEXT"
        const val WORKOUT = "WORKOUT"
        const val PROGRESS_PHOTO = "PROGRESS_PHOTO"
        const val ACHIEVEMENT = "ACHIEVEMENT"
        const val MEAL = "MEAL"
    }
}
