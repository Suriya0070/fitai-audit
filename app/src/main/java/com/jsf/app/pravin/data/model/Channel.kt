package com.jsf.app.pravin.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "channels")
data class Channel(
    @PrimaryKey val id: String,
    val serverId: String,
    val name: String,
    val type: String = "TEXT",
    val description: String = "",
    val position: Int = 0,
    val isPublic: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    object ChannelType {
        const val TEXT = "TEXT"
        const val ANNOUNCEMENTS = "ANNOUNCEMENTS"
        const val WORKOUT = "WORKOUT"
        const val NUTRITION = "NUTRITION"
        const val LIVE = "LIVE"
        const val LEADERBOARD = "LEADERBOARD"
        const val CHALLENGES = "CHALLENGES"
        const val PROGRESS = "PROGRESS"
    }
}
