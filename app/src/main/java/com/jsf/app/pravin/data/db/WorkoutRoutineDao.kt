package com.jsf.app.pravin.data.db

import androidx.room.*
import com.jsf.app.pravin.data.model.WorkoutRoutine
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutRoutineDao {
    @Query("SELECT * FROM workout_routines WHERE serverId = :serverId AND isActive = 1 ORDER BY scheduledDate ASC")
    fun getRoutinesForServer(serverId: String): Flow<List<WorkoutRoutine>>

    @Query("SELECT * FROM workout_routines WHERE id = :routineId LIMIT 1")
    fun getRoutineById(routineId: String): Flow<WorkoutRoutine?>

    @Query("SELECT * FROM workout_routines WHERE serverId = :serverId AND scheduledDate >= :fromDate AND isActive = 1 ORDER BY scheduledDate ASC")
    fun getUpcomingRoutines(serverId: String, fromDate: Long): Flow<List<WorkoutRoutine>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRoutine(routine: WorkoutRoutine)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRoutines(routines: List<WorkoutRoutine>)

    @Query("UPDATE workout_routines SET isActive = 0 WHERE id = :routineId")
    suspend fun deactivateRoutine(routineId: String)

    @Delete
    suspend fun deleteRoutine(routine: WorkoutRoutine)
}
