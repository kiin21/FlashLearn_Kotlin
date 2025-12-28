package com.kotlin.flashlearn.presentation.learning_session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlin.flashlearn.domain.repository.FlashcardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Learning Session screen.
 * Follows Google's recommended ViewModel pattern with:
 * - StateFlow for UI state (survives configuration changes)
 * - Channel for one-time events (navigation, toasts)
 * - Business logic handled in ViewModel, not in UI layer
 */
@HiltViewModel
class LearningSessionViewModel @Inject constructor(
    private val flashcardRepository: FlashcardRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val topicId: String = savedStateHandle.get<String>("topicId") ?: ""

    private val _state = MutableStateFlow(LearningSessionState())
    val state = _state.asStateFlow()

    private val _uiEvent = Channel<LearningSessionUiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        loadFlashcards()
    }

    /**
     * Loads flashcards for the given topic from FlashcardRepository (backed by Datamuse API).
     */
    private fun loadFlashcards() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            flashcardRepository.getFlashcardsByTopicId(topicId).fold(
                onSuccess = { flashcards ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            flashcards = flashcards,
                            currentCardIndex = 0
                        )
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error.message
                        )
                    }
                    _uiEvent.send(LearningSessionUiEvent.ShowError(error.message ?: "Failed to load flashcards"))
                }
            )
        }
    }

    /**
     * Flips the current card to show definition or word.
     */
    fun flipCard() {
        _state.update { it.copy(isCardFlipped = !it.isCardFlipped) }
    }

    /**
     * Handles "Got It" button click - marks card as mastered and moves to next.
     */
    fun onGotIt(userId: String) {
        viewModelScope.launch {
            val currentCard = _state.value.currentCard ?: return@launch
            
            // Mark as mastered
            flashcardRepository.markFlashcardAsMastered(currentCard.id, userId)
            
            _state.update {
                it.copy(
                    masteredCardIds = it.masteredCardIds + currentCard.id
                )
            }
            
            moveToNextCard()
        }
    }

    /**
     * Handles "Study Again" button click - marks for review and moves to next.
     */
    fun onStudyAgain(userId: String) {
        viewModelScope.launch {
            val currentCard = _state.value.currentCard ?: return@launch
            
            // Mark for review
            flashcardRepository.markFlashcardForReview(currentCard.id, userId)
            
            moveToNextCard()
        }
    }

    /**
     * Moves to the next flashcard or completes session.
     */
    private suspend fun moveToNextCard() {
        _state.update {
            it.copy(
                currentCardIndex = it.currentCardIndex + 1,
                isCardFlipped = false // Reset flip state
            )
        }

        // Check if session is complete
        if (_state.value.isSessionComplete) {
            _uiEvent.send(LearningSessionUiEvent.SessionComplete)
        }
    }

    /**
     * Handles swipe gesture to move to next card.
     */
    fun onSwipeNext(userId: String) {
        // For swipe, we treat it as "Study Again" by default
        onStudyAgain(userId)
    }

    /**
     * Exits the learning session.
     */
    fun exitSession() {
        viewModelScope.launch {
            _uiEvent.send(LearningSessionUiEvent.NavigateToHome)
        }
    }
}
