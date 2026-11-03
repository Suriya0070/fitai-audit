package com.jsf.app.pravin.data.db

import androidx.room.*
import com.jsf.app.pravin.data.model.ServerRole
import kotlinx.coroutines.flow.Flow

@Dao
interface ServerRoleDao {
    @Query("SELECT * FROM server_roles WHERE serverId = :serverId ORDER BY position ASC")
    fun getRolesForServer(serverId: String): Flow<List<ServerRole>>

    @Query("SELECT * FROM server_roles WHERE serverId = :serverId AND name = :roleName LIMIT 1")
    suspend fun getRoleByName(serverId: String, roleName: String): ServerRole?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRole(role: ServerRole)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRoles(roles: List<ServerRole>)

    @Delete
    suspend fun deleteRole(role: ServerRole)

    @Query("DELETE FROM server_roles WHERE serverId = :serverId")
    suspend fun deleteRolesForServer(serverId: String)
}
