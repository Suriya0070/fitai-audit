package com.jsf.app.pravin.ui.screens.achievements

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jsf.app.pravin.data.db.FitnessDatabase
import com.jsf.app.pravin.data.model.Achievement
import com.jsf.app.pravin.data.preferences.UserPreferences
import com.jsf.app.pravin.data.repository.FitnessRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AchievementCatalogItem(
    val type: String,
    val emoji: String,
    val title: String,
    val requirement: String,
    val earnedAt: Long? = null
)

class AchievementsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = FitnessDatabase.getInstance(application)
    private val repository = FitnessRepository(db)
    private val prefs = UserPreferences(application)

    private val catalog = listOf(
        Triple(Achievement.AchievementType.FIRST_WORKOUT,   "🏋️", "First Rep"         to "Complete your first workout"),
        Triple(Achievement.AchievementType.STREAK_7,        "🔥", "On Fire"            to "Maintain a 7-day workout streak"),
        Triple(Achievement.AchievementType.STREAK_30,       "⚡", "Unstoppable"        to "Maintain a 30-day workout streak"),
        Triple(Achievement.AchievementType.TOTAL_REPS_1000, "💪", "Rep Warrior"        to "Complete 1,000 total reps"),
        Triple(Achievement.AchievementType.TOTAL_REPS_10000,"🦾", "Rep Legend"         to "Complete 10,000 total reps"),
        Triple(Achievement.AchievementType.PERFECT_SCORE,   "⭐", "Perfect Form"       to "Achieve a form score of 95+"),
        Triple(Achievement.AchievementType.CHALLENGE_COMPLETE,"🏆","Challenge Accepted" to "Complete a Rep Battle challenge"),
        Triple(Achievement.AchievementType.FIRST_COMMUNITY, "🌐", "Social Butterfly"   to "Join your first community server"),
        Triple(Achievement.AchievementType.LIVE_WORKOUT,    "📺", "Live Legend"        to "Complete a live workout session"),
    )

    private val _items = MutableStateFlow<List<AchievementCatalogItem>>(emptyList())
    val items: StateFlow<List<AchievementCatalogItem>> = _items.asStateFlow()

    init {
        viewModelScope.launch {
            val userId = prefs.currentUserId.first()
            repository.getAchievementsForUser(userId).collect { earned ->
                val earnedByType = earned.associateBy { it.type }
                _items.value = catalog.map { (type, emoji, titleReq) ->
                    AchievementCatalogItem(
                        type = type,
                        emoji = emoji,
                        title = titleReq.first,
                        requirement = titleReq.second,
                        earnedAt = earnedByType[type]?.earnedAt
                    )
                }
            }
        }
    }
}
