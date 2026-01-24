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
class CardDetailViewModel @Inject constructor(
    private val flashcardRepository: FlashcardRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val cardId: String = savedStateHandle.get<String>("cardId").orEmpty()

    private val _state = MutableStateFlow(CardDetailState())
    val state: StateFlow<CardDetailState> = _state.asStateFlow()

    init {
        loadCard()
    }

    private fun loadCard() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            val result = flashcardRepository.getFlashcardById(cardId)

            result.fold(
                onSuccess = { card ->
                    if (card != null) {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            flashcard = card,
                            error = null
                        )
                        enrichCard(card)
                    } else {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            flashcard = null,
                            error = "Card not found"
                        )
                    }
                },
                onFailure = { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        flashcard = null,
                        error = e.message ?: "Unknown error"
                    )
                }
            )
        }
    }



    private fun enrichCard(card: com.kotlin.flashlearn.domain.model.Flashcard) {
        if (card.imageUrl.isBlank() || card.ipa.isBlank()) {
            viewModelScope.launch {
                try {
                    val enrichedCard = flashcardRepository.enrichFlashcard(card, false)
                    _state.value = _state.value.copy(flashcard = enrichedCard)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun regenerateImage() {
        val currentCard = _state.value.flashcard ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                val enrichedCard = flashcardRepository.enrichFlashcard(currentCard, force = true)
                _state.value = _state.value.copy(flashcard = enrichedCard, isLoading = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = "Failed to regenerate image: ${e.message}")
            }
        }
    }

    fun flip() {
        _state.value = _state.value.copy(isFlipped = !_state.value.isFlipped)
    }
}
