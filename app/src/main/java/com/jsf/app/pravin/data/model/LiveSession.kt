package com.jsf.app.pravin.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "live_sessions")
data class LiveSession(
    @PrimaryKey val id: String,
    val serverId: String,
    val channelId: String,
    val routineId: String = "",
    val hostId: String,
    val title: String,
    val status: String = "SCHEDULED",
    val scheduledAt: Long = 0L,
    val startedAt: Long = 0L,
    val endedAt: Long = 0L,
    val participantCount: Int = 0
) {
    object Status {
        const val SCHEDULED = "SCHEDULED"
        const val LIVE = "LIVE"
        const val ENDED = "ENDED"
    }
}
