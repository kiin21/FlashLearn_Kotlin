package com.kotlin.flashlearn.presentation.topic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlin.flashlearn.domain.model.Topic
import com.kotlin.flashlearn.domain.model.VocabularyWord
import com.kotlin.flashlearn.domain.repository.DatamuseRepository
import com.kotlin.flashlearn.domain.repository.TopicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for TopicScreen - manages topic list and word fetching.
 */
@HiltViewModel
class TopicViewModel @Inject constructor(
    private val topicRepository: TopicRepository,
    private val datamuseRepository: DatamuseRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(TopicUiState())
    val uiState: StateFlow<TopicUiState> = _uiState.asStateFlow()
    
    private val _topicWords = MutableStateFlow<Map<String, List<VocabularyWord>>>(emptyMap())
    val topicWords: StateFlow<Map<String, List<VocabularyWord>>> = _topicWords.asStateFlow()
    
    init {
        loadTopics()
    }
    
    fun loadTopics() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            topicRepository.getAllTopics()
                .onSuccess { topics ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        topics = topics
                    )
                    // Load vocabulary for each topic from Datamuse
                    topics.forEach { topic ->
                        loadWordsForTopic(topic)
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
    
    private fun loadWordsForTopic(topic: Topic) {
        viewModelScope.launch {
            datamuseRepository.getWordsByTopic(topic.name)
                .onSuccess { words ->
                    val currentWords = _topicWords.value.toMutableMap()
                    currentWords[topic.id] = words
                    _topicWords.value = currentWords
                }
        }
    }
    
    fun searchTopics(query: String) {
        if (query.isBlank()) {
            loadTopics()
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            topicRepository.searchTopics(query)
                .onSuccess { topics ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        topics = topics
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
        }
    }
    
    fun getWordCountForTopic(topicId: String): Int {
        return _topicWords.value[topicId]?.size ?: 0
    }
}

data class TopicUiState(
    val isLoading: Boolean = false,
    val topics: List<Topic> = emptyList(),
    val error: String? = null
)
