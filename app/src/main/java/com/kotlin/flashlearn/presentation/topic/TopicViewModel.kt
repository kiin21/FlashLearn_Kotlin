package com.kotlin.flashlearn.presentation.topic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.kotlin.flashlearn.domain.model.Topic
import com.kotlin.flashlearn.domain.model.TopicCategory
import com.kotlin.flashlearn.domain.repository.AuthRepository
import com.kotlin.flashlearn.domain.repository.FlashcardRepository
import com.kotlin.flashlearn.domain.repository.UserRepository
import com.kotlin.flashlearn.domain.repository.TopicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.kotlin.flashlearn.R

/**
 * Filter options for Topic Screen.
 */
enum class TopicFilter(val resId: Int) {
    ALL(R.string.filter_all),
    FAVORITES(R.string.favorites),
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
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(TopicUiState())
    val uiState: StateFlow<TopicUiState> = _uiState.asStateFlow()
    
    // Word count for each topic (loaded from FlashcardRepository)
    private val _topicWordCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val topicWordCounts: StateFlow<Map<String, Int>> = _topicWordCounts.asStateFlow()
    
    // Progress for each topic: Map<topicId, Pair<masteredCount, totalCount>>
    private val _topicProgress = MutableStateFlow<Map<String, Pair<Int, Int>>>(emptyMap())
    val topicProgress: StateFlow<Map<String, Pair<Int, Int>>> = _topicProgress.asStateFlow()
    
    val currentUserId: String?
        get() = authRepository.getSignedInUser()?.userId ?: firebaseAuth.currentUser?.uid
    
    init {
        loadTopics()
    }
    
    fun loadTopics() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            // Fetch topics
            topicRepository.getVisibleTopics(currentUserId)
                .onSuccess { topics ->
                    // Categorize topics
                    val systemTopics = topics.filter { it.getCategory() == TopicCategory.SYSTEM }
                    val myTopics = topics.filter { 
                        it.createdBy == currentUserId 
                    }
                    
                    // Fetch user favorites
                    val likedIds = currentUserId?.let { userId ->
                        userRepository.getUser(userId)?.likedTopicIds?.toSet()
                    } ?: emptySet()
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        allTopics = topics,
                        systemTopics = systemTopics,
                        myTopics = myTopics,
                        likedTopicIds = likedIds
                    )
                    
                    // Apply current filter and search
                    applyFilterAndSearch()
                    
                    // Load word count for each topic from FlashcardRepository
                    topics.forEach { topic ->
                        loadWordCountForTopic(topic.id)
                        loadProgressForTopic(topic.id)
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
     * Loads the progress (mastered/total) for a specific topic.
     */
    private fun loadProgressForTopic(topicId: String) {
        val userId = currentUserId ?: return
        
        viewModelScope.launch {
            flashcardRepository.getTopicProgress(topicId, userId)
                .onSuccess { (masteredCount, totalCount) ->
                    _topicProgress.value = _topicProgress.value + (topicId to Pair(masteredCount, totalCount))
                }
                .onFailure { error ->
                    // Silently fail - progress is optional
                }
        }
    }
    
    /**
     * Toggles the favorite status of a topic.
     */
    fun toggleTopicLike(topicId: String) {
        val userId = currentUserId ?: return
        val currentLikedIds = _uiState.value.likedTopicIds
        val isLiked = topicId in currentLikedIds
        
        // Optimistic update
        val newLikedIds = if (isLiked) {
            currentLikedIds - topicId
        } else {
            currentLikedIds + topicId
        }
        
        _uiState.value = _uiState.value.copy(likedTopicIds = newLikedIds)
        applyFilterAndSearch() // Re-apply filter if viewing favorites
        
        viewModelScope.launch {
            try {
                userRepository.toggleTopicLike(userId, topicId, !isLiked)
            } catch (e: Exception) {
                // Revert on failure
                _uiState.value = _uiState.value.copy(likedTopicIds = currentLikedIds)
                applyFilterAndSearch()
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
            TopicFilter.FAVORITES -> state.allTopics.filter { it.id in state.likedTopicIds }
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
    val likedTopicIds: Set<String> = emptySet(),
    val searchQuery: String = "",
    val activeFilter: TopicFilter = TopicFilter.ALL,
    val error: String? = null
)
