package com.jsf.app.pravin.domain

object LevelUtil {
    // Level n requires n² × 100 XP total (Level 1=0, Level 2=100, Level 3=400, Level 4=900...)
    fun levelFromXp(xp: Long): Int = (1 + Math.sqrt((xp / 100.0))).toInt().coerceIn(1, 100)

    fun xpForLevel(n: Int): Long = ((n - 1).toLong() * (n - 1) * 100L)

    fun progressToNextLevel(xp: Long): Float {
        val level = levelFromXp(xp)
        val low = xpForLevel(level)
        val high = xpForLevel(level + 1)
        return if (high == low) 1f else ((xp - low).toFloat() / (high - low)).coerceIn(0f, 1f)
    }

    fun xpForWorkout(score: Float, streak: Int): Long {
        var xp = 50L
        if (score >= 95f) xp += 50
        else if (score >= 80f) xp += 20
        if (streak >= 30) xp += 100
        else if (streak >= 7) xp += 30
        return xp
    }
}
