package com.kotlin.flashlearn.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlin.flashlearn.data.local.dao.UserStreakDao
import com.kotlin.flashlearn.data.local.prefs.LastUserPrefs
import com.kotlin.flashlearn.domain.model.DailyWord
import com.kotlin.flashlearn.domain.repository.AuthRepository
import com.kotlin.flashlearn.domain.repository.FlashcardRepository
import com.kotlin.flashlearn.domain.usecase.GetTodayDailyWordUseCase
import com.kotlin.flashlearn.utils.DateKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ContinueLearningData(
    val topicId: String,
    val topicName: String,
    val progress: Float, // 0.0 to 1.0
    val masteredCount: Int,
    val totalCount: Int
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userStreakDao: UserStreakDao,
    private val topicRepository: com.kotlin.flashlearn.domain.repository.TopicRepository,
    private val flashcardRepository: FlashcardRepository,
    private val getTodayDailyWord: GetTodayDailyWordUseCase,
    private val lastUserPrefs: LastUserPrefs,
) : ViewModel() {

    private val _streakDays = MutableStateFlow(0)
    val streakDays = _streakDays.asStateFlow()

    private val _dailyWord = MutableStateFlow<DailyWord?>(null)
    val dailyWord = _dailyWord.asStateFlow()

    private val _recommendedTopics =
        MutableStateFlow<List<com.kotlin.flashlearn.domain.model.Topic>>(emptyList())
    val recommendedTopics = _recommendedTopics.asStateFlow()

    private val _continueLearningData = MutableStateFlow<ContinueLearningData?>(null)
    val continueLearningData = _continueLearningData.asStateFlow()

    init {
        loadStreak()
        loadDailyWord()
        fetchRecommendedTopics()
        fetchContinueLearningTopic()
    }

    private fun loadStreak() {
        viewModelScope.launch {
            val user = authRepository.getSignedInUser()
            if (user == null) {
                _streakDays.value = 0
                return@launch
            }

            val entity = userStreakDao.getByUserId(user.userId)

            _streakDays.value = entity?.currentStreak ?: 0
        }
    }

    private fun fetchRecommendedTopics() {
        viewModelScope.launch {
            try {
                topicRepository.getTopRecommendedTopics()
                    .onSuccess { topics ->
                        _recommendedTopics.value = topics
                    }
                    .onFailure { e ->
                        e.printStackTrace()
                    }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Fetches the topic with the highest progress that is not yet complete (< 100%).
     * This will be displayed in the "Continue Learning" section.
     */
    private fun fetchContinueLearningTopic() {
        viewModelScope.launch {
            try {
                val user = authRepository.getSignedInUser() ?: return@launch
                val userId = user.userId

                // Get all visible topics for the user
                topicRepository.getVisibleTopics(userId)
                    .onSuccess { topics ->
                        if (topics.isEmpty()) {
                            _continueLearningData.value = null
                            return@onSuccess
                        }

                        // Calculate progress for each topic and find the one with highest incomplete progress
                        val topicsWithProgress = topics.mapNotNull { topic ->
                            val progressResult =
                                flashcardRepository.getTopicProgress(topic.id, userId)
                            progressResult.getOrNull()?.let { (masteredCount, totalCount) ->
                                if (totalCount > 0) {
                                    val progress = masteredCount.toFloat() / totalCount.toFloat()
                                    Triple(topic, progress, Pair(masteredCount, totalCount))
                                } else null
                            }
                        }

                        // Find topic with highest progress that is not 100% complete
                        val bestTopic = topicsWithProgress
                            .filter { (_, progress, _) -> progress < 1.0f } // Not complete
                            .maxByOrNull { (_, progress, _) -> progress }

                        if (bestTopic != null) {
                            val (topic, progress, counts) = bestTopic
                            _continueLearningData.value = ContinueLearningData(
                                topicId = topic.id,
                                topicName = topic.name,
                                progress = progress,
                                masteredCount = counts.first,
                                totalCount = counts.second
                            )
                        } else {
                            // If all topics are complete, show the first topic with progress
                            val anyTopic = topicsWithProgress.firstOrNull()
                            if (anyTopic != null) {
                                val (topic, progress, counts) = anyTopic
                                _continueLearningData.value = ContinueLearningData(
                                    topicId = topic.id,
                                    topicName = topic.name,
                                    progress = progress,
                                    masteredCount = counts.first,
                                    totalCount = counts.second
                                )
                            } else {
                                _continueLearningData.value = null
                            }
                        }
                    }
                    .onFailure { e ->
                        e.printStackTrace()
                        _continueLearningData.value = null
                    }
            } catch (e: Exception) {
                e.printStackTrace()
                _continueLearningData.value = null
            }
        }
    }

    fun refreshStreak() = loadStreak()

    private fun loadDailyWord() {
        viewModelScope.launch {
            val user = authRepository.getSignedInUser()
            if (user == null) {
                _dailyWord.value = null
                return@launch
            }
            lastUserPrefs.setLastUserId(user.userId)

            val today = DateKey.today()
            _dailyWord.value = getTodayDailyWord(user.userId, today)
        }
    }
}
