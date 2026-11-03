package com.jsf.app.pravin.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jsf.app.pravin.data.model.RepFormRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface RepFormRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: RepFormRecord): Long

    @Query("SELECT * FROM rep_form_records WHERE sessionId = :sessionId ORDER BY repNumber ASC")
    fun getRecordsForSession(sessionId: Long): Flow<List<RepFormRecord>>

    @Query("SELECT * FROM rep_form_records WHERE sessionId = :sessionId ORDER BY repNumber ASC")
    suspend fun getRecordsForSessionSync(sessionId: Long): List<RepFormRecord>

    @Query("DELETE FROM rep_form_records WHERE sessionId = :sessionId")
    suspend fun deleteForSession(sessionId: Long)
}
