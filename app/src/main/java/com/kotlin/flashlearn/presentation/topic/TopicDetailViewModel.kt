package com.kotlin.flashlearn.presentation.topic

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlin.flashlearn.domain.repository.FlashcardRepository
import com.kotlin.flashlearn.domain.repository.TopicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TopicDetailViewModel @Inject constructor(
    private val topicRepository: TopicRepository,
    private val flashcardRepository: FlashcardRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val topicId: String = savedStateHandle.get<String>("topicId").orEmpty()

    private val _state = MutableStateFlow(TopicDetailState())
    val state: StateFlow<TopicDetailState> = _state.asStateFlow()

    init {
        loadTopicAndCards()
    }

    private fun loadTopicAndCards() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            // Get topic details for title/description
            val topicResult = topicRepository.getTopicById(topicId)
            
            topicResult.fold(
                onSuccess = { topic ->
                    if (topic == null) {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = "Topic not found"
                        )
                        return@launch
                    }
                    
                    _state.value = _state.value.copy(
                        topicTitle = topic.name,
                        topicDescription = topic.description
                    )
                    
                    // Load flashcards from repository (backed by Datamuse API)
                    loadFlashcards()
                },
                onFailure = { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load topic"
                    )
                }
            )
        }
    }
    
    private suspend fun loadFlashcards() {
        val flashcardResult = flashcardRepository.getFlashcardsByTopicId(topicId)
        
        flashcardResult.fold(
            onSuccess = { flashcards ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    cards = flashcards,
                    error = null
                )
            },
            onFailure = { e ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    cards = emptyList(),
                    error = e.message ?: "Failed to load vocabulary"
                )
            }
        )
    }
    
    fun refreshCards() {
        loadTopicAndCards()
    }
}
