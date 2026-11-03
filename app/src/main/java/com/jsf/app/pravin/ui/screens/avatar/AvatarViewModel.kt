package com.jsf.app.pravin.ui.screens.avatar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jsf.app.pravin.data.db.FitnessDatabase
import com.jsf.app.pravin.data.repository.FitnessRepository
import com.jsf.app.pravin.domain.ConsistencyTracker
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class AvatarViewModel(application: Application) : AndroidViewModel(application) {

    private val db = FitnessDatabase.getInstance(application)
    private val repository = FitnessRepository(db)
    private val consistencyTracker = ConsistencyTracker()

    private val _streak = MutableStateFlow(0)
    val streak: StateFlow<Int> = _streak.asStateFlow()

    private val _avatarLevel = MutableStateFlow(0)
    val avatarLevel: StateFlow<Int> = _avatarLevel.asStateFlow()

    private val _motivationalMessage = MutableStateFlow("")
    val motivationalMessage: StateFlow<String> = _motivationalMessage.asStateFlow()

    private val _weeklyActivity = MutableStateFlow<List<Boolean>>(List(7) { false })
    val weeklyActivity: StateFlow<List<Boolean>> = _weeklyActivity.asStateFlow()

    private val _allTimestamps = MutableStateFlow<List<Long>>(emptyList())
    val allTimestamps: StateFlow<List<Long>> = _allTimestamps.asStateFlow()

    init {
        viewModelScope.launch {
            refreshData()
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            val timestamps = repository.getAllTimestamps()
            _allTimestamps.value = timestamps

            val s = consistencyTracker.calculateStreak(timestamps)
            _streak.value = s
            _avatarLevel.value = consistencyTracker.getAvatarLevel(s)
            _motivationalMessage.value = consistencyTracker.getMotivationalMessage(s)
            _weeklyActivity.value = calculateWeeklyActivity(timestamps)
        }
    }

    private fun calculateWeeklyActivity(timestamps: List<Long>): List<Boolean> {
        val activeDays = timestamps.map { toDayKey(it) }.toSet()
        val today = toDayKey(System.currentTimeMillis())
        return (6 downTo 0).map { daysBack ->
            activeDays.contains(today - daysBack)
        }
    }

    private fun toDayKey(timestamp: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        return cal.get(Calendar.YEAR) * 366L + cal.get(Calendar.DAY_OF_YEAR)
    }
}
