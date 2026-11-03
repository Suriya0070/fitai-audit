package com.jsf.app.pravin.domain

import java.util.Calendar

class ConsistencyTracker {
    fun calculateStreak(timestamps: List<Long>): Int {
        if (timestamps.isEmpty()) return 0
        val days = timestamps.map { toDayKey(it) }.toSortedSet()
        val today = toDayKey(System.currentTimeMillis())
        var streak = 0
        var current = today
        while (days.contains(current)) {
            streak++
            current--
        }
        // Also check if streak started yesterday (didn't workout today yet)
        if (streak == 0) {
            current = today - 1
            while (days.contains(current)) {
                streak++
                current--
            }
        }
        return streak
    }

    fun getAvatarLevel(streak: Int): Int = when {
        streak >= 30 -> 4
        streak >= 14 -> 3
        streak >= 7 -> 2
        streak >= 1 -> 1
        else -> 0
    }

    fun getAvatarTitle(level: Int): String = when (level) {
        0 -> "Beginner"
        1 -> "Warrior"
        2 -> "Athlete"
        3 -> "Champion"
        4 -> "Legend"
        else -> "Beginner"
    }

    fun getMotivationalMessage(streak: Int): String = when {
        streak == 0 -> "Start your journey today!"
        streak == 1 -> "Great start! Come back tomorrow!"
        streak < 7 -> "You're on a $streak-day streak! Keep it up!"
        streak < 14 -> "One week strong! Incredible dedication!"
        streak < 30 -> "Two weeks in! You're unstoppable!"
        else -> "30+ day streak! You're a Legend!"
    }

    private fun toDayKey(timestamp: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        return cal.get(Calendar.YEAR) * 366L + cal.get(Calendar.DAY_OF_YEAR)
    }
}
