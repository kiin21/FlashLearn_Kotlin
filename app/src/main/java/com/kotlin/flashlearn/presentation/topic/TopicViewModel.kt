package com.kotlin.flashlearn.presentation.topic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
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

import com.kotlin.flashlearn.R

/**
 * Filter options for Topic Screen.
 */
enum class TopicFilter(val resId: Int) {
    ALL(R.string.filter_all),
    SYSTEM(R.string.filter_system),
    MY_TOPICS(R.string.filter_my_topics)
}

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
                        it.createdBy == currentUserId 
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        allTopics = topics,
                        systemTopics = systemTopics,
                        myTopics = myTopics
                    )
                    
                    // Apply current filter and search
                    applyFilterAndSearch()
                    
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
    
    /**
     * Updates search query and filters topics.
     */
    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        applyFilterAndSearch()
    }
    
    /**
     * Updates active filter and filters topics.
     */
    fun updateFilter(filter: TopicFilter) {
        _uiState.value = _uiState.value.copy(activeFilter = filter)
        applyFilterAndSearch()
    }
    
    /**
     * Applies both search query and filter to produce displayed topics.
     */
    private fun applyFilterAndSearch() {
        val state = _uiState.value
        val query = state.searchQuery.lowercase().trim()
        
        // Step 1: Apply filter
        val filteredByCategory = when (state.activeFilter) {
            TopicFilter.ALL -> state.allTopics
            TopicFilter.SYSTEM -> state.systemTopics
            TopicFilter.MY_TOPICS -> state.myTopics
        }
        
        // Step 2: Apply search
        val filteredBySearch = if (query.isBlank()) {
            filteredByCategory
        } else {
            filteredByCategory.filter { topic ->
                topic.name.lowercase().contains(query) ||
                topic.description.lowercase().contains(query) ||
                topic.creatorName.lowercase().contains(query)
            }
        }
        
        _uiState.value = state.copy(displayedTopics = filteredBySearch)
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
    val displayedTopics: List<Topic> = emptyList(),
    val searchQuery: String = "",
    val activeFilter: TopicFilter = TopicFilter.ALL,
    val error: String? = null
)
