package com.jsf.app.pravin.data.db

import androidx.room.*
import com.jsf.app.pravin.data.model.Invite
import kotlinx.coroutines.flow.Flow

@Dao
interface InviteDao {
    @Query("SELECT * FROM invites WHERE serverId = :serverId AND isRevoked = 0 ORDER BY createdAt DESC")
    fun getInvitesForServer(serverId: String): Flow<List<Invite>>

    @Query("SELECT * FROM invites WHERE id = :code LIMIT 1")
    suspend fun getInviteByCode(code: String): Invite?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertInvite(invite: Invite)

    @Query("UPDATE invites SET isRevoked = 1 WHERE id = :code")
    suspend fun revokeInvite(code: String)

    @Query("UPDATE invites SET useCount = useCount + 1 WHERE id = :code")
    suspend fun incrementUseCount(code: String)

    @Query("DELETE FROM invites WHERE serverId = :serverId AND (expiresAt > 0 AND expiresAt < :now)")
    suspend fun deleteExpiredInvites(serverId: String, now: Long)
}
