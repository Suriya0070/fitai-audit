package com.jsf.app.pravin.data.db

import androidx.room.*
import com.jsf.app.pravin.data.model.Server
import kotlinx.coroutines.flow.Flow

@Dao
interface ServerDao {
    @Query("SELECT * FROM servers ORDER BY name ASC")
    fun getAllServers(): Flow<List<Server>>

    @Query("SELECT * FROM servers WHERE isPublic = 1 ORDER BY memberCount DESC")
    fun getPublicServers(): Flow<List<Server>>

    @Query("SELECT * FROM servers WHERE id = :serverId LIMIT 1")
    fun getServerById(serverId: String): Flow<Server?>

    @Query("SELECT * FROM servers WHERE ownerId = :userId ORDER BY createdAt DESC")
    fun getServersByOwner(userId: String): Flow<List<Server>>

    @Query("SELECT * FROM servers WHERE name LIKE '%' || :query || '%' AND isPublic = 1")
    fun searchPublicServers(query: String): Flow<List<Server>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertServer(server: Server)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertServers(servers: List<Server>)

    @Delete
    suspend fun deleteServer(server: Server)

    @Query("UPDATE servers SET memberCount = :count, updatedAt = :updatedAt WHERE id = :serverId")
    suspend fun updateMemberCount(serverId: String, count: Int, updatedAt: Long)
}
