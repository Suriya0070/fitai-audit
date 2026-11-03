package com.jsf.app.pravin.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "nutrition_logs",
    indices = [Index("userId"), Index("timestamp")]
)
data class NutritionLog(
    @PrimaryKey val id: String,
    val userId: String,
    val mealType: String = MealType.MEAL,
    val description: String = "",
    val imageUrl: String = "",
    val aiStatus: String = AiStatus.NONE,
    val recognizedFoods: String = "[]",     // JSON array ["chicken", "rice"]
    val estimatedCalories: Int = 0,
    val estimatedProteinG: Float = 0f,
    val estimatedCarbsG: Float = 0f,
    val estimatedFatG: Float = 0f,
    val userOverrideCalories: Int = 0,
    val trainerVisible: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
) {
    val effectiveCalories: Int
        get() = if (userOverrideCalories > 0) userOverrideCalories else estimatedCalories

    object MealType {
        const val BREAKFAST = "BREAKFAST"
        const val LUNCH = "LUNCH"
        const val DINNER = "DINNER"
        const val SNACK = "SNACK"
        const val PRE_WORKOUT = "PRE_WORKOUT"
        const val POST_WORKOUT = "POST_WORKOUT"
        const val MEAL = "MEAL"

        val all = listOf(BREAKFAST, LUNCH, DINNER, SNACK, PRE_WORKOUT, POST_WORKOUT)
        fun label(type: String): String = when (type) {
            BREAKFAST -> "Breakfast"
            LUNCH -> "Lunch"
            DINNER -> "Dinner"
            SNACK -> "Snack"
            PRE_WORKOUT -> "Pre-Workout"
            POST_WORKOUT -> "Post-Workout"
            else -> "Meal"
        }
    }

    object AiStatus {
        const val NONE = "none"
        const val ANALYZING = "analyzing"
        const val COMPLETE = "complete"
        const val FAILED = "failed"
    }
}
