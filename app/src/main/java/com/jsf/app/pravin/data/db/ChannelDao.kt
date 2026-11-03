package com.jsf.app.pravin.data.db

import androidx.room.*
import com.jsf.app.pravin.data.model.Channel
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {
    @Query("SELECT * FROM channels WHERE serverId = :serverId ORDER BY position ASC")
    fun getChannelsForServer(serverId: String): Flow<List<Channel>>

    @Query("SELECT * FROM channels WHERE serverId = :serverId AND type = :type ORDER BY position ASC")
    fun getChannelsByType(serverId: String, type: String): Flow<List<Channel>>

    @Query("SELECT * FROM channels WHERE id = :channelId LIMIT 1")
    fun getChannelById(channelId: String): Flow<Channel?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChannel(channel: Channel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChannels(channels: List<Channel>)

    @Delete
    suspend fun deleteChannel(channel: Channel)

    @Query("DELETE FROM channels WHERE serverId = :serverId")
    suspend fun deleteAllChannelsForServer(serverId: String)
}
