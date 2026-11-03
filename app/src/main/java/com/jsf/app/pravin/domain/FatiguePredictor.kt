package com.jsf.app.pravin.domain

import kotlin.math.max

class FatiguePredictor {
    private val sessionScores = ArrayDeque<Float>(60)

    fun update(score: Float) {
        sessionScores.addLast(score)
        if (sessionScores.size > 60) sessionScores.removeFirst()
    }

    fun predict(): FatigueReport {
        if (sessionScores.size < 10) {
            return FatigueReport(0f, 0f, "Keep going! You're doing great.")
        }

        val recentAvg = sessionScores.takeLast(10).average().toFloat()
        val earlyAvg = sessionScores.take(minOf(10, sessionScores.size)).average().toFloat()
        val scoreDrop = max(0f, earlyAvg - recentAvg)

        val fatigueLevel = (scoreDrop / 30f).coerceIn(0f, 1f)

        val trendSlope = calculateTrend()
        val injuryRisk = when {
            fatigueLevel > 0.7f && trendSlope < -2f -> 0.8f
            fatigueLevel > 0.5f -> 0.5f
            trendSlope < -3f -> 0.4f
            else -> fatigueLevel * 0.3f
        }.coerceIn(0f, 1f)

        val recommendation = when {
            injuryRisk > 0.7f -> "High injury risk detected! Consider stopping and resting."
            injuryRisk > 0.4f -> "Moderate fatigue detected. Slow down and focus on form."
            fatigueLevel > 0.5f -> "You're getting tired. Take a 30-second break."
            else -> "Great form! Keep pushing!"
        }

        return FatigueReport(fatigueLevel, injuryRisk, recommendation)
    }

    private fun calculateTrend(): Float {
        if (sessionScores.size < 5) return 0f
        val n = sessionScores.size
        val x = (0 until n).map { it.toFloat() }
        val y = sessionScores.toList()
        val sumX = x.sum(); val sumY = y.sum()
        val sumXY = x.zip(y).sumOf { (a, b) -> (a * b).toDouble() }.toFloat()
        val sumX2 = x.sumOf { (it * it).toDouble() }.toFloat()
        return if (n * sumX2 - sumX * sumX != 0f)
            (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX)
        else 0f
    }

    fun reset() = sessionScores.clear()
}
