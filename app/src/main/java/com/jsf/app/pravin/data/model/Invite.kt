package com.jsf.app.pravin.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "invites")
data class Invite(
    @PrimaryKey val id: String,
    val serverId: String,
    val createdBy: String,
    val maxUses: Int = 0,
    val useCount: Int = 0,
    val expiresAt: Long = 0L,
    val isRevoked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
