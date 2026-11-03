package com.jsf.app.pravin.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey val id: String,
    val channelId: String,
    val serverId: String,
    val authorId: String,
    val content: String,
    val attachmentUrl: String = "",
    val attachmentType: String = "",
    val replyToId: String = "",
    val isEdited: Boolean = false,
    val isDeleted: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val editedAt: Long = 0L
) {
    object AttachmentType {
        const val IMAGE = "IMAGE"
        const val VIDEO = "VIDEO"
        const val WORKOUT_RESULT = "WORKOUT_RESULT"
        const val MEAL = "MEAL"
    }
}
