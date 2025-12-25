package com.kotlin.flashlearn.presentation.topic

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlin.flashlearn.domain.repository.FlashcardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TopicDetailViewModel @Inject constructor(
    private val flashcardRepository: FlashcardRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val topicId: String = savedStateHandle.get<String>("topicId").orEmpty()

    private val _state = MutableStateFlow(TopicDetailState())
    val state: StateFlow<TopicDetailState> = _state.asStateFlow()

    init {
        loadCards()
    }

    private fun loadCards() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            val result = flashcardRepository.getFlashcardsByTopicId(topicId)
            result.fold(
                onSuccess = { cards ->
                    _state.value = _state.value.copy(isLoading = false, cards = cards, error = null)
                },
                onFailure = { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        cards = emptyList(),
                        error = e.message ?: "Unknown error"
                    )
                }
            )
        }
    }
}
