package com.kotlin.flashlearn.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlin.flashlearn.data.local.dao.UserStreakDao
import com.kotlin.flashlearn.data.local.prefs.LastUserPrefs
import com.kotlin.flashlearn.domain.model.DailyWord
import com.kotlin.flashlearn.domain.repository.AuthRepository
import com.kotlin.flashlearn.domain.usecase.GetTodayDailyWordUseCase
import com.kotlin.flashlearn.utils.DateKey
import com.kotlin.flashlearn.workers.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userStreakDao: UserStreakDao,
    private val topicRepository: com.kotlin.flashlearn.domain.repository.TopicRepository,
    private val getTodayDailyWord: GetTodayDailyWordUseCase,
    private val lastUserPrefs: LastUserPrefs,
) : ViewModel() {

    private val _streakDays = MutableStateFlow(0)
    val streakDays = _streakDays.asStateFlow()

    private val _dailyWord = MutableStateFlow<DailyWord?>(null)
    val dailyWord = _dailyWord.asStateFlow()

    private val _recommendedTopics = MutableStateFlow<List<com.kotlin.flashlearn.domain.model.Topic>>(emptyList())
    val recommendedTopics = _recommendedTopics.asStateFlow()

    init {
        loadStreak()
        loadDailyWord()
        fetchRecommendedTopics()
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
