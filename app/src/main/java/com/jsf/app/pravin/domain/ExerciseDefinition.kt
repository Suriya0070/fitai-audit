package com.jsf.app.pravin.domain

enum class ExerciseCategory(val displayName: String, val emoji: String) {
    CHEST("Chest", "💪"),
    BACK("Back", "🏋️"),
    SHOULDERS("Shoulders", "🤸"),
    BICEPS("Biceps", "💪"),
    TRICEPS("Triceps", "🔱"),
    CORE("Core", "🧘"),
    LEGS("Legs", "🦵"),
    GLUTES("Glutes", "🍑"),
    CALVES("Calves", "🦶"),
    FULL_BODY("Full Body", "🔥")
}

enum class Difficulty(val displayName: String) {
    BEGINNER("Beginner"),
    INTERMEDIATE("Intermediate"),
    ADVANCED("Advanced")
}

enum class ScoreType {
    KNEE_FLEXION,
    ELBOW_PUSH,
    ELBOW_PULL,
    ELBOW_OVERHEAD,
    HIP_EXTENSION,
    HIP_HINGE,
    TRUNK_FLEXION,
    LEG_RAISE,
    STATIC_HOLD,
    DYNAMIC_CARDIO,
    CALF_RAISE
}

enum class MuscleGroup(val displayName: String) {
    CHEST("Chest"),
    BACK("Back"),
    SHOULDERS("Shoulders"),
    BICEPS("Biceps"),
    TRICEPS("Triceps"),
    ABS("Abs"),
    OBLIQUES("Obliques"),
    LOWER_BACK("Lower Back"),
    QUADS("Quads"),
    HAMSTRINGS("Hamstrings"),
    GLUTES("Glutes"),
    CALVES("Calves"),
    FULL_BODY("Full Body")
}

/** Angle measured at vertex [b] between rays b→a and b→c */
data class JointTriplet(val a: Int, val b: Int, val c: Int)

/**
 * Guards that both landmarks must be within [minDist, maxDist] normalized distance of each other.
 * Used to distinguish push-up variants by wrist spacing: diamond=close, normal=medium, wide=far.
 */
data class JointDistanceGuard(
    val joint1: Int,
    val joint2: Int,
    val minDist: Float = 0f,
    val maxDist: Float = 1f
)

data class RepConfig(
    val primaryJoints: JointTriplet,
    val mirrorJoints: JointTriplet? = null,
    val downThreshold: Float,
    val upThreshold: Float,
    /** true = "down" is high angle (arm extended), "up" is low angle (arm contracted) — e.g. curls */
    val invertPhase: Boolean = false,
    /** Optional guard: the specified joint pair must be within this normalized distance range */
    val distanceGuard: JointDistanceGuard? = null,
    /** Minimum milliseconds between counted reps — prevents noise/tremor from double-counting */
    val minRepIntervalMs: Long = 400L,
    /**
     * true (default) = average both sides — for bilateral symmetric exercises (squats, push-ups).
     * false = use the more bent/active side (min angle) — for alternating exercises (lunges,
     * high knees, mountain climbers) where one side leads and averaging dilutes the measurement.
     */
    val symmetric: Boolean = true
)

data class ExerciseDefinition(
    val name: String,
    val emoji: String,
    val category: ExerciseCategory,
    val primaryMuscle: String,
    val secondaryMuscles: List<String>,
    val difficulty: Difficulty,
    val isTimeBased: Boolean = false,
    val repConfig: RepConfig? = null,
    val formTips: List<String>,
    val scoreType: ScoreType,
    val musclesWorked: List<MuscleGroup>
)
