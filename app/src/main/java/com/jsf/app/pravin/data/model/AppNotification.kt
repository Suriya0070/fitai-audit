package com.jsf.app.pravin.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class AppNotification(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val type: String,
    val title: String,
    val body: String,
    val data: String = "{}",
    val isRead: Boolean = false,
    val serverId: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    object NotificationType {
        const val WORKOUT = "WORKOUT"
        const val ANNOUNCEMENT = "ANNOUNCEMENT"
        const val LIVE_WORKOUT = "LIVE_WORKOUT"
        const val CHALLENGE = "CHALLENGE"
        const val REACTION = "REACTION"
        const val COMMENT = "COMMENT"
        const val NEW_MEMBER = "NEW_MEMBER"
        const val ROUTINE_ASSIGNED = "ROUTINE_ASSIGNED"
        const val CHALLENGE_COMPLETE = "CHALLENGE_COMPLETE"
    }
}
