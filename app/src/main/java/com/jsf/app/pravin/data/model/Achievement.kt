package com.jsf.app.pravin.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "achievements")
data class Achievement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val type: String,
    val title: String,
    val description: String = "",
    val earnedAt: Long = System.currentTimeMillis(),
    val serverId: String = ""
) {
    object AchievementType {
        const val FIRST_WORKOUT = "FIRST_WORKOUT"
        const val STREAK_7 = "STREAK_7"
        const val STREAK_30 = "STREAK_30"
        const val TOTAL_REPS_1000 = "TOTAL_REPS_1000"
        const val TOTAL_REPS_10000 = "TOTAL_REPS_10000"
        const val CHALLENGE_COMPLETE = "CHALLENGE_COMPLETE"
        const val PERFECT_SCORE = "PERFECT_SCORE"
        const val FIRST_COMMUNITY = "FIRST_COMMUNITY"
        const val LIVE_WORKOUT = "LIVE_WORKOUT"
    }
}
