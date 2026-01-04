package com.kotlin.flashlearn.presentation.quiz

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlin.flashlearn.domain.model.Flashcard
import com.kotlin.flashlearn.domain.model.QuizQuestion
import com.kotlin.flashlearn.domain.repository.AuthRepository
import com.kotlin.flashlearn.domain.repository.FlashcardRepository
import com.kotlin.flashlearn.domain.usecase.GenerateQuestionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QuizViewModel @Inject constructor(
    private val flashcardRepository: FlashcardRepository,
    private val authRepository: AuthRepository,
    private val generateQuestionUseCase: GenerateQuestionUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val topicId: String = savedStateHandle.get<String>("topicId") ?: ""
    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState = _uiState.asStateFlow()

    private var flashcards: List<Flashcard> = emptyList()
    private var currentCardIndex = 0

    init {
        loadFlashcards()
    }

    private fun loadFlashcards() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            flashcardRepository.getFlashcardsByTopicId(topicId).fold(
                onSuccess = { cards ->
                    flashcards = cards.shuffled()
                    if (flashcards.isNotEmpty()) {
                        loadNextQuestion()
                    } else {
                        _uiState.update { it.copy(isLoading = false, error = "No flashcards found") }
                    }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
            )
        }
    }

    private fun loadNextQuestion() {
        if (currentCardIndex >= flashcards.size) {
            // Session complete
            // TODO: Navigate to summary
            return
        }

        val card = flashcards[currentCardIndex]
        val userId = authRepository.getSignedInUser()?.userId ?: return

        viewModelScope.launch {
            val score = flashcardRepository.getProficiencyScore(card.id, userId).getOrDefault(0)
            val question = generateQuestionUseCase(card, score, flashcards)
            
            _uiState.update {
                it.copy(
                    isLoading = false,
                    currentQuestion = question,
                    isAnswerCorrect = null,
                    showFeedback = false
                )
            }
        }
    }

    fun submitAnswer(answer: String) {
        val state = _uiState.value
        val question = state.currentQuestion ?: return
        if (state.showFeedback) return // Already answered

        val isCorrect = validateAnswer(question, answer)
        val userId = authRepository.getSignedInUser()?.userId ?: return

        viewModelScope.launch {
            // Update UI immediately
            _uiState.update {
                it.copy(
                    isAnswerCorrect = isCorrect,
                    showFeedback = true
                )
            }

            // Update Logic
            val currentScore = flashcardRepository.getProficiencyScore(question.flashcard.id, userId).getOrDefault(0)
            val newScore = if (isCorrect) currentScore + 1 else 0 // Reset on error as per PRD
            
            flashcardRepository.updateProficiencyScore(question.flashcard.id, userId, newScore)

            // Wait and advance
            delay(1500)
            currentCardIndex++
            loadNextQuestion()
        }
    }

    private fun validateAnswer(question: QuizQuestion, input: String): Boolean {
        return when (question) {
            is QuizQuestion.MultipleChoice -> input == question.flashcard.word
            is QuizQuestion.Scramble -> input.equals(question.flashcard.word, ignoreCase = true)
            is QuizQuestion.ExactTyping -> input.trim() == question.flashcard.word // Strict
        }
    }
}
