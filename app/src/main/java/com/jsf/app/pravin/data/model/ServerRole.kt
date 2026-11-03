package com.jsf.app.pravin.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "server_roles",
    indices = [Index(value = ["serverId", "name"], unique = true)]
)
data class ServerRole(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val serverId: String,
    val name: String,
    val color: String = "#FFFFFF",
    val permissions: String = "[]",
    val position: Int = 0,
    val isDefault: Boolean = false
) {
    companion object {
        const val OWNER = "OWNER"
        const val ADMIN = "ADMIN"
        const val MODERATOR = "MODERATOR"
        const val COACH = "COACH"
        const val MEMBER = "MEMBER"
    }

    object Permission {
        const val MANAGE_SERVER = "MANAGE_SERVER"
        const val MANAGE_CHANNELS = "MANAGE_CHANNELS"
        const val MANAGE_MEMBERS = "MANAGE_MEMBERS"
        const val MANAGE_ROLES = "MANAGE_ROLES"
        const val MANAGE_WORKOUTS = "MANAGE_WORKOUTS"
        const val MANAGE_CHALLENGES = "MANAGE_CHALLENGES"
        const val CREATE_WORKOUTS = "CREATE_WORKOUTS"
        const val START_LIVE_SESSION = "START_LIVE_SESSION"
        const val POST_ANNOUNCEMENTS = "POST_ANNOUNCEMENTS"
        const val KICK_MEMBERS = "KICK_MEMBERS"
        const val BAN_MEMBERS = "BAN_MEMBERS"
        const val DELETE_MESSAGES = "DELETE_MESSAGES"
        const val SEND_MESSAGES = "SEND_MESSAGES"
        const val JOIN_WORKOUTS = "JOIN_WORKOUTS"
        const val UPLOAD_PROGRESS = "UPLOAD_PROGRESS"
        const val UPLOAD_MEALS = "UPLOAD_MEALS"
    }
}
