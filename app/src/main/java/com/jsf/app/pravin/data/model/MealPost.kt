package com.jsf.app.pravin.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meal_posts")
data class MealPost(
    @PrimaryKey val id: String,
    val channelId: String,
    val serverId: String,
    val authorId: String,
    val mealType: String = "MEAL",
    val description: String = "",
    val imageUrl: String = "",
    val aiAnalysis: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val likeCount: Int = 0,
    val commentCount: Int = 0
) {
    object MealType {
        const val BREAKFAST = "BREAKFAST"
        const val LUNCH = "LUNCH"
        const val DINNER = "DINNER"
        const val SNACK = "SNACK"
        const val PRE_WORKOUT = "PRE_WORKOUT"
        const val POST_WORKOUT = "POST_WORKOUT"
        const val MEAL = "MEAL"
    }
}
