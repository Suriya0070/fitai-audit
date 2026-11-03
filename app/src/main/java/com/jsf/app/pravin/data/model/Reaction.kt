package com.jsf.app.pravin.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reactions",
    indices = [Index(value = ["postId", "userId", "emoji"], unique = true)]
)
data class Reaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val postId: String = "",
    val messageId: String = "",
    val userId: String,
    val emoji: String,
    val timestamp: Long = System.currentTimeMillis()
)
