package com.jsf.app.pravin.data.db

import androidx.room.*
import com.jsf.app.pravin.data.model.ServerMember
import kotlinx.coroutines.flow.Flow

@Dao
interface ServerMemberDao {
    @Query("SELECT * FROM server_members WHERE serverId = :serverId AND isActive = 1 ORDER BY joinedAt ASC")
    fun getMembersOfServer(serverId: String): Flow<List<ServerMember>>

    @Query("SELECT * FROM server_members WHERE userId = :userId AND isActive = 1")
    fun getServersForUser(userId: String): Flow<List<ServerMember>>

    @Query("SELECT * FROM server_members WHERE serverId = :serverId AND userId = :userId LIMIT 1")
    fun getMembership(serverId: String, userId: String): Flow<ServerMember?>

    @Query("SELECT role FROM server_members WHERE serverId = :serverId AND userId = :userId LIMIT 1")
    suspend fun getUserRole(serverId: String, userId: String): String?

    @Query("SELECT COUNT(*) FROM server_members WHERE serverId = :serverId AND isActive = 1")
    suspend fun getMemberCount(serverId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMember(member: ServerMember)

    @Query("UPDATE server_members SET isActive = 0 WHERE serverId = :serverId AND userId = :userId")
    suspend fun removeMember(serverId: String, userId: String)

    @Query("UPDATE server_members SET role = :role WHERE serverId = :serverId AND userId = :userId")
    suspend fun updateRole(serverId: String, userId: String, role: String)
}
