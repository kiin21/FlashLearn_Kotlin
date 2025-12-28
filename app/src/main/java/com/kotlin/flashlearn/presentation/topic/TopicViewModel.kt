package com.kotlin.flashlearn.presentation.topic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.kotlin.flashlearn.domain.model.Flashcard
import com.kotlin.flashlearn.domain.model.Topic
import com.kotlin.flashlearn.domain.model.TopicCategory
import com.kotlin.flashlearn.domain.repository.FlashcardRepository
import com.kotlin.flashlearn.domain.repository.TopicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for TopicScreen - manages topic list with visibility awareness.
 */
@HiltViewModel
class TopicViewModel @Inject constructor(
    private val topicRepository: TopicRepository,
    private val flashcardRepository: FlashcardRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(TopicUiState())
    val uiState: StateFlow<TopicUiState> = _uiState.asStateFlow()
    
    // Word count for each topic (loaded from FlashcardRepository)
    private val _topicWordCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val topicWordCounts: StateFlow<Map<String, Int>> = _topicWordCounts.asStateFlow()
    
    val currentUserId: String?
        get() = firebaseAuth.currentUser?.uid
    
    init {
        loadTopics()
    }
    
    fun loadTopics() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            topicRepository.getVisibleTopics(currentUserId)
                .onSuccess { topics ->
                    // Categorize topics
                    val systemTopics = topics.filter { it.getCategory() == TopicCategory.SYSTEM }
                    val myTopics = topics.filter { 
                        it.getCategory() == TopicCategory.PRIVATE && it.createdBy == currentUserId 
                    }
                    val sharedTopics = topics.filter { 
                        it.getCategory() == TopicCategory.SHARED && it.createdBy == currentUserId 
                    }
                    val communityTopics = topics.filter {
                        it.getCategory() == TopicCategory.SHARED && it.createdBy != currentUserId
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        allTopics = topics,
                        systemTopics = systemTopics,
                        myTopics = myTopics,
                        mySharedTopics = sharedTopics,
                        communityTopics = communityTopics
                    )
                    
                    // Load word count for each topic from FlashcardRepository
                    topics.forEach { topic ->
                        loadWordCountForTopic(topic.id)
                    }
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load topics"
                    )
                }
        }
    }
    
    private fun loadWordCountForTopic(topicId: String) {
        viewModelScope.launch {
            flashcardRepository.getFlashcardsByTopicId(topicId)
                .onSuccess { flashcards ->
                    val currentCounts = _topicWordCounts.value.toMutableMap()
                    currentCounts[topicId] = flashcards.size
                    _topicWordCounts.value = currentCounts
                }
        }
    }
    

    fun getWordCountForTopic(topicId: String): Int {
        return _topicWordCounts.value[topicId] ?: 0
    }
    
    fun isLoggedIn(): Boolean = currentUserId != null
    
    fun deleteTopic(topicId: String) {
        viewModelScope.launch {
            val result = topicRepository.deleteTopic(topicId)
            if (result.isSuccess) {
                // Determine if we need to remove from cache or just reload
                loadTopics() // Simple reload to refresh the list
            } else {
                 _uiState.value = _uiState.value.copy(
                    error = result.exceptionOrNull()?.message ?: "Failed to delete topic"
                )
            }
        }
    }
}

data class TopicUiState(
    val isLoading: Boolean = false,
    val allTopics: List<Topic> = emptyList(),
    val systemTopics: List<Topic> = emptyList(),
    val myTopics: List<Topic> = emptyList(),
    val mySharedTopics: List<Topic> = emptyList(),
    val communityTopics: List<Topic> = emptyList(),
    val error: String? = null
)
