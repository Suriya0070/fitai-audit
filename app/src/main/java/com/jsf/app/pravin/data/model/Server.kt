package com.jsf.app.pravin.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "servers")
data class Server(
    @PrimaryKey val id: String,
    val name: String,
    val description: String = "",
    val iconUrl: String = "",
    val bannerUrl: String = "",
    val ownerId: String,
    val isPublic: Boolean = true,
    val memberCount: Int = 0,
    val categories: String = "[]",
    val tags: String = "[]",
    val rules: String = "",
    val welcomeMessage: String = "",
    val accentColor: String = "#00D4FF",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
