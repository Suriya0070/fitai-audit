package com.jsf.app.pravin.data.db

import androidx.room.*
import com.jsf.app.pravin.data.model.RoutineExercise
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineExerciseDao {
    @Query("SELECT * FROM routine_exercises WHERE routineId = :routineId ORDER BY position ASC")
    fun getExercisesForRoutine(routineId: String): Flow<List<RoutineExercise>>

    @Query("SELECT * FROM routine_exercises WHERE routineId = :routineId ORDER BY position ASC")
    suspend fun getExercisesForRoutineSync(routineId: String): List<RoutineExercise>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExercise(exercise: RoutineExercise)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExercises(exercises: List<RoutineExercise>)

    @Delete
    suspend fun deleteExercise(exercise: RoutineExercise)

    @Query("DELETE FROM routine_exercises WHERE routineId = :routineId")
    suspend fun deleteExercisesForRoutine(routineId: String)
}
