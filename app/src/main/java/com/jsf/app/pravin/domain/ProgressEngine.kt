package com.jsf.app.pravin.domain

import com.jsf.app.pravin.data.model.WorkoutSession

data class PersonalRecord(
    val exerciseName: String,
    val bestScore: Float,
    val bestReps: Int,
    val totalSessions: Int,
    val lastTimestamp: Long
)

class ProgressEngine {
    fun generateReport(sessions: List<WorkoutSession>): ProgressReport {
        if (sessions.isEmpty()) return ProgressReport()

        val totalSessions = sessions.size
        val avgScore = sessions.map { it.totalScore }.average().toFloat()

        val weeklyTrend = if (sessions.size >= 2) {
            val recent = sessions.take(minOf(3, sessions.size)).map { it.totalScore }.average()
            val older = sessions.drop(minOf(3, sessions.size)).take(3).map { it.totalScore }.average()
            if (older > 0) ((recent - older) / older * 100).toFloat() else 0f
        } else 0f

        val avgMetrics = mapOf(
            "Accuracy" to sessions.map { it.accuracyScore }.average().toFloat(),
            "Balance" to sessions.map { it.balanceScore }.average().toFloat(),
            "Stability" to sessions.map { it.stabilityScore }.average().toFloat(),
            "Symmetry" to sessions.map { it.symmetryScore }.average().toFloat(),
            "Speed" to sessions.map { it.speedScore }.average().toFloat()
        )

        val bestMetric = avgMetrics.maxByOrNull { it.value }?.key ?: "Accuracy"
        val weakestMetric = avgMetrics.minByOrNull { it.value }?.key ?: "Speed"

        val improvement = if (sessions.size >= 2) {
            val first = sessions.last().totalScore
            val latest = sessions.first().totalScore
            latest - first
        } else 0f

        return ProgressReport(
            weeklyTrend = weeklyTrend,
            bestMetric = bestMetric,
            weakestMetric = weakestMetric,
            totalSessions = totalSessions,
            avgScore = avgScore,
            improvement = improvement
        )
    }

    fun getWeeklyScores(sessions: List<WorkoutSession>): List<Pair<String, Float>> {
        val grouped = sessions.groupBy { session ->
            val dayMs = 24 * 60 * 60 * 1000L
            val daysSinceEpoch = session.timestamp / dayMs
            daysSinceEpoch
        }
        return grouped.entries
            .sortedBy { it.key }
            .takeLast(7)
            .map { entry ->
                val dayLabel = getDayLabel(entry.key)
                val avgScore = entry.value.map { it.totalScore }.average().toFloat()
                Pair(dayLabel, avgScore)
            }
    }

    private fun getDayLabel(daysSinceEpoch: Long): String {
        val days = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = daysSinceEpoch * 24 * 60 * 60 * 1000L
        return days[cal.get(java.util.Calendar.DAY_OF_WEEK) - 1]
    }

    fun getExerciseScores(sessions: List<WorkoutSession>, exerciseName: String): List<Pair<String, Float>> =
        sessions
            .filter { it.exerciseType == exerciseName }
            .sortedBy { it.timestamp }
            .takeLast(10)
            .mapIndexed { idx, s -> "#${idx + 1}" to s.totalScore }

    fun getPersonalRecords(sessions: List<WorkoutSession>): Map<String, PersonalRecord> =
        sessions.groupBy { it.exerciseType }.mapValues { (name, es) ->
            PersonalRecord(
                exerciseName = name,
                bestScore = es.maxOf { it.totalScore },
                bestReps = es.maxOf { it.repCount },
                totalSessions = es.size,
                lastTimestamp = es.maxOf { it.timestamp }
            )
        }

    fun getMuscleGroupVolume(sessions: List<WorkoutSession>): Map<MuscleGroup, Int> {
        val map = mutableMapOf<MuscleGroup, Int>()
        sessions.forEach { session ->
            val exercise = ExerciseLibrary.getByName(session.exerciseType) ?: return@forEach
            val reps = maxOf(session.repCount, 1)
            exercise.musclesWorked.forEach { muscle ->
                map[muscle] = (map[muscle] ?: 0) + reps
            }
        }
        return map
    }

    fun getCalendarData(sessions: List<WorkoutSession>): Map<Long, Float> {
        val dayMs = 86_400_000L
        val cutoff = System.currentTimeMillis() - 35 * dayMs
        return sessions
            .filter { it.timestamp >= cutoff }
            .groupBy { it.timestamp / dayMs }
            .mapValues { (_, daySessions) -> daySessions.map { it.totalScore }.average().toFloat() }
    }

    fun getExerciseMetrics(sessions: List<WorkoutSession>, exerciseName: String): List<Pair<String, Float>> {
        val filtered = sessions.filter { it.exerciseType == exerciseName }
        if (filtered.isEmpty()) return emptyList()
        return listOf(
            "Accuracy"  to filtered.map { it.accuracyScore }.average().toFloat(),
            "Balance"   to filtered.map { it.balanceScore }.average().toFloat(),
            "Stability" to filtered.map { it.stabilityScore }.average().toFloat(),
            "Symmetry"  to filtered.map { it.symmetryScore }.average().toFloat(),
            "Speed"     to filtered.map { it.speedScore }.average().toFloat()
        )
    }
}
