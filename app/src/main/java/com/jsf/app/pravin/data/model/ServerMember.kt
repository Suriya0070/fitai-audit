package com.jsf.app.pravin.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "server_members",
    indices = [Index(value = ["serverId", "userId"], unique = true)]
)
data class ServerMember(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val serverId: String,
    val userId: String,
    val role: String = ServerRole.MEMBER,
    val joinedAt: Long = System.currentTimeMillis(),
    val nickname: String = "",
    val isActive: Boolean = true
) {
    object Role {
        const val OWNER = "OWNER"
        const val ADMIN = "ADMIN"
        const val MODERATOR = "MODERATOR"
        const val COACH = "COACH"
        const val MEMBER = "MEMBER"
    }
}
