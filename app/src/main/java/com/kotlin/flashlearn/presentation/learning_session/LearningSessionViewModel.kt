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
     * Loads flashcards for the given topic.
     */
    private fun loadFlashcards() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            flashcardRepository.getFlashcardsByTopicId(topicId).fold(
                onSuccess = { flashcards ->
                    val sessionCards = flashcards.getOrNull(0)
                    _state.update {
                        it.copy(
                            isLoading = false,
                            sessionQueue = sessionCards,
                            initialCardCount = sessionCards.size,
                            completedCardCount = 0,
                            previousState = null
                        )
                    }
                    
                    // Enrich the first card
                    sessionCards.firstOrNull()?.let { enrichCard(it) }
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

    private fun enrichCard(card: com.kotlin.flashlearn.domain.model.Flashcard) {
        if (card.imageUrl.isBlank() || card.ipa.isBlank()) {
            viewModelScope.launch {
                try {
                    // Cast to implementation to access enrichFlashcard if it's not in interface
                    val enrichedCard = (flashcardRepository as? com.kotlin.flashlearn.data.repository.FlashcardRepositoryImpl)?.enrichFlashcard(card) 
                        ?: return@launch

                    _state.update { currentState ->
                        val updatedQueue = currentState.sessionQueue.map { 
                            if (it.id == enrichedCard.id) enrichedCard else it 
                        }
                        currentState.copy(sessionQueue = updatedQueue)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * Flips the current card.
     */
    fun flipCard() {
        _state.update { it.copy(isCardFlipped = !it.isCardFlipped) }
    }

    /**
     * Swipe Right - "Remembered"
     * Card is removed from the session stack.
     */
    fun onSwipeRight() {
        val currentState = _state.value
        val currentCard = currentState.currentCard ?: return

        // Save state for undo (clearing nested previousState to limit history)
        val stateToSave = currentState.copy(previousState = null)

        val newQueue = currentState.sessionQueue.drop(1)
        
        _state.update {
            it.copy(
                sessionQueue = newQueue,
                completedCardCount = it.completedCardCount + 1,
                isCardFlipped = false,
                previousState = stateToSave
            )
        }

        checkSessionCompletion(newQueue)
        
        // Enrich next card
        newQueue.firstOrNull()?.let { enrichCard(it) }
    }

    /**
     * Swipe Left - "Not Remembered"
     * Card is re-queued to the end of the stack.
     */
    fun onSwipeLeft() {
        val currentState = _state.value
        val currentCard = currentState.currentCard ?: return

        // Save state for undo
        val stateToSave = currentState.copy(previousState = null)

        val newQueue = currentState.sessionQueue.drop(1) + currentCard

        _state.update {
            it.copy(
                sessionQueue = newQueue,
                isCardFlipped = false,
                previousState = stateToSave
            )
        }
        
        // Enrich next card
        newQueue.firstOrNull()?.let { enrichCard(it) }
    }

    /**
     * Undo the last action.
     */
    fun onUndo() {
        _state.update { currentState ->
            currentState.previousState ?: currentState
        }
    }

    private fun checkSessionCompletion(queue: List<com.kotlin.flashlearn.domain.model.Flashcard>) {
        if (queue.isEmpty()) {
            viewModelScope.launch {
                _uiEvent.send(LearningSessionUiEvent.SessionComplete)
            }
        }
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
