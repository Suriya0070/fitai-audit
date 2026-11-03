package com.jsf.app.pravin.domain

data class Landmark(
    val x: Float,
    val y: Float,
    val z: Float,
    val visibility: Float
)

data class PoseResult(
    val landmarks: List<Landmark>,
    val isBodyFullyVisible: Boolean
) {
    companion object {
        val EMPTY = PoseResult(emptyList(), false)
    }

    fun getLandmark(index: Int): Landmark? = landmarks.getOrNull(index)

    // Key landmark indices (MediaPipe BlazePose 33 landmarks)
    fun nose() = getLandmark(0)
    fun leftShoulder() = getLandmark(11)
    fun rightShoulder() = getLandmark(12)
    fun leftElbow() = getLandmark(13)
    fun rightElbow() = getLandmark(14)
    fun leftWrist() = getLandmark(15)
    fun rightWrist() = getLandmark(16)
    fun leftHip() = getLandmark(23)
    fun rightHip() = getLandmark(24)
    fun leftKnee() = getLandmark(25)
    fun rightKnee() = getLandmark(26)
    fun leftAnkle() = getLandmark(27)
    fun rightAnkle() = getLandmark(28)
    fun leftHeel() = getLandmark(29)
    fun rightHeel() = getLandmark(30)
    fun leftToe() = getLandmark(31)
    fun rightToe() = getLandmark(32)
}

data class WorkoutScore(
    val accuracy: Float = 0f,
    val balance: Float = 0f,
    val stability: Float = 0f,
    val symmetry: Float = 0f,
    val speed: Float = 0f
) {
    val overall: Float get() = (accuracy * 0.35f + balance * 0.2f + stability * 0.2f + symmetry * 0.15f + speed * 0.1f)
}

data class EnvironmentQuality(
    val brightness: Float = 1f,
    val sharpness: Float = 1f,
    val bodyVisibility: Float = 1f,
    val cameraAngle: Float = 1f,
    val overall: Float = 1f,
    val feedback: String = "Environment looks good!"
)

data class FatigueReport(
    val fatigueLevel: Float = 0f,
    val injuryRisk: Float = 0f,
    val recommendation: String = "Keep going!"
)

data class ProgressReport(
    val weeklyTrend: Float = 0f,
    val bestMetric: String = "Accuracy",
    val weakestMetric: String = "Speed",
    val totalSessions: Int = 0,
    val avgScore: Float = 0f,
    val improvement: Float = 0f
)
