package com.jsf.app.pravin.data.db

import androidx.room.*
import com.jsf.app.pravin.data.model.Message
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE channelId = :channelId AND isDeleted = 0 ORDER BY timestamp ASC")
    fun getMessagesForChannel(channelId: String): Flow<List<Message>>

    @Query("SELECT * FROM messages WHERE channelId = :channelId AND isDeleted = 0 ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentMessages(channelId: String, limit: Int = 50): Flow<List<Message>>

    @Query("SELECT * FROM messages WHERE id = :messageId LIMIT 1")
    suspend fun getMessageById(messageId: String): Message?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMessage(message: Message)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMessages(messages: List<Message>)

    @Query("UPDATE messages SET isDeleted = 1 WHERE id = :messageId")
    suspend fun deleteMessage(messageId: String)

    @Query("UPDATE messages SET content = :content, isEdited = 1, editedAt = :editedAt WHERE id = :messageId")
    suspend fun editMessage(messageId: String, content: String, editedAt: Long)

    @Query("DELETE FROM messages WHERE channelId = :channelId")
    suspend fun clearChannel(channelId: String)
}
