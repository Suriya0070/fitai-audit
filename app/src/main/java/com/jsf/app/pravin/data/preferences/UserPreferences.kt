package com.jsf.app.pravin.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserPreferences(private val context: Context) {
    companion object {
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val SELECTED_EXERCISE = stringPreferencesKey("selected_exercise")
        val CURRENT_USER_ID = stringPreferencesKey("current_user_id")
        val FITNESS_GOAL = stringPreferencesKey("fitness_goal")
        val WEEKLY_GOAL_SESSIONS = intPreferencesKey("weekly_goal_sessions")
    }

    val isOnboardingDone: Flow<Boolean> = context.dataStore.data.map { it[ONBOARDING_DONE] ?: false }
    val selectedExercise: Flow<String> = context.dataStore.data.map { it[SELECTED_EXERCISE] ?: "Squats" }
    val currentUserId: Flow<String> = context.dataStore.data.map { it[CURRENT_USER_ID] ?: "" }
    val fitnessGoal: Flow<String> = context.dataStore.data.map { it[FITNESS_GOAL] ?: "Stay Active" }
    val weeklyGoalSessions: Flow<Int> = context.dataStore.data.map { it[WEEKLY_GOAL_SESSIONS] ?: 3 }

    suspend fun setOnboardingDone(done: Boolean) {
        context.dataStore.edit { it[ONBOARDING_DONE] = done }
    }

    suspend fun setSelectedExercise(exercise: String) {
        context.dataStore.edit { it[SELECTED_EXERCISE] = exercise }
    }

    suspend fun setCurrentUserId(uid: String) {
        context.dataStore.edit { it[CURRENT_USER_ID] = uid }
    }

    suspend fun setFitnessGoal(goal: String) {
        context.dataStore.edit { it[FITNESS_GOAL] = goal }
    }

    suspend fun setWeeklyGoalSessions(sessions: Int) {
        context.dataStore.edit { it[WEEKLY_GOAL_SESSIONS] = sessions }
    }
}
