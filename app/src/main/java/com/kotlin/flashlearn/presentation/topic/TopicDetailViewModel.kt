package com.kotlin.flashlearn.presentation.topic

import androidx.lifecycle.SavedStateHandle
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
 * ViewModel for TopicDetailScreen - manages topic details and vocabulary.
 */
@HiltViewModel
class TopicDetailViewModel @Inject constructor(
    private val topicRepository: TopicRepository,
    private val datamuseRepository: DatamuseRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val topicId: String = savedStateHandle.get<String>("topicId") ?: ""
    
    private val _uiState = MutableStateFlow(TopicDetailUiState())
    val uiState: StateFlow<TopicDetailUiState> = _uiState.asStateFlow()
    
    init {
        loadTopicDetail()
    }
    
    private fun loadTopicDetail() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            // Load topic info from Neon DB
            topicRepository.getTopicById(topicId)
                .onSuccess { topic ->
                    if (topic != null) {
                        _uiState.value = _uiState.value.copy(topic = topic)
                        // Load vocabulary from Datamuse based on topic name
                        loadVocabulary(topic.name)
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Topic not found"
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load topic"
                    )
                }
        }
    }
    
    private fun loadVocabulary(topicName: String) {
        viewModelScope.launch {
            datamuseRepository.getWordsByTopic(topicName)
                .onSuccess { words ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        vocabulary = words.filter { it.definition.isNotBlank() }.take(20)
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
    
    fun refresh() {
        loadTopicDetail()
    }
}

data class TopicDetailUiState(
    val isLoading: Boolean = false,
    val topic: Topic? = null,
    val vocabulary: List<VocabularyWord> = emptyList(),
    val error: String? = null
)
