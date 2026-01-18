package com.kotlin.flashlearn.presentation.quiz

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlin.flashlearn.domain.model.Flashcard
import com.kotlin.flashlearn.domain.model.QuizQuestion
import com.kotlin.flashlearn.domain.model.QuizResult
import com.kotlin.flashlearn.domain.model.QuizConfig
import com.kotlin.flashlearn.domain.model.QuizMode
import com.kotlin.flashlearn.domain.repository.AuthRepository
import com.kotlin.flashlearn.domain.repository.FlashcardRepository
import com.kotlin.flashlearn.domain.usecase.GenerateQuestionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
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
    private val initialMode: QuizMode = savedStateHandle.get<String>("mode")?.let {
        runCatching { QuizMode.valueOf(it) }.getOrDefault(QuizMode.SPRINT)
    } ?: QuizMode.SPRINT
    private val initialCount: Int = savedStateHandle.get<Int>("count") ?: 10
    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<QuizUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private var flashcards: List<Flashcard> = emptyList()
    private var currentCardIndex = 0
    private var totalQuestions = 0
    private var currentStreak = 0
    private val quizResults = mutableListOf<QuizResult>()
    private var quizConfig: QuizConfig = QuizConfig(initialMode, initialCount)

    val currentMode: QuizMode
        get() = quizConfig.mode

    init {
        startQuiz(quizConfig)
    }

    fun startQuiz(config: QuizConfig) {
        quizConfig = config
        currentCardIndex = 0
        currentStreak = 0
        quizResults.clear()
        loadFlashcards()
    }

    private fun loadFlashcards() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    isCompleted = false,
                    results = emptyList(),
                    currentStreak = 0
                )
            }
            flashcardRepository.getFlashcardsByTopicId(topicId).fold(
                onSuccess = { cards ->
                    flashcards = cards.shuffled()
                    if (flashcards.isNotEmpty()) {
                        totalQuestions = minOf(quizConfig.questionCount, flashcards.size)
                        loadNextQuestion()
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "No flashcards found"
                            )
                        }
                    }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
            )
        }
    }

    private fun loadNextQuestion() {
        if (totalQuestions == 0 || currentCardIndex >= totalQuestions) {
            viewModelScope.launch {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentQuestion = null,
                        isCompleted = true,
                        results = quizResults.toList(),
                        currentIndex = currentCardIndex,
                        totalQuestions = totalQuestions,
                        currentStreak = currentStreak
                    )
                }
                _uiEvent.emit(QuizUiEvent.NavigateToSummary(topicId))
            }
            return
        }

        val card = flashcards[currentCardIndex]
        val userId = authRepository.getSignedInUser()?.userId ?: return

        viewModelScope.launch {
            val score = flashcardRepository.getProficiencyScore(card.id, userId).getOrDefault(0)
            val question = generateQuestionUseCase(card, score, flashcards, quizConfig.mode)

            _uiState.update {
                it.copy(
                    isLoading = false,
                    currentQuestion = question,
                    isAnswerCorrect = null,
                    showFeedback = false,
                    error = null,
                    currentIndex = currentCardIndex,
                    totalQuestions = totalQuestions,
                    currentStreak = currentStreak,
                    results = quizResults.toList()
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

        currentStreak = if (isCorrect) currentStreak + 1 else 0
        quizResults.add(QuizResult(question.flashcard, isCorrect))

        viewModelScope.launch {
            // Update UI immediately
            _uiState.update {
                it.copy(
                    isAnswerCorrect = isCorrect,
                    showFeedback = true,
                    currentStreak = currentStreak,
                    results = quizResults.toList()
                )
            }

            // Update Logic
            val currentScore =
                flashcardRepository.getProficiencyScore(question.flashcard.id, userId)
                    .getOrDefault(0)
            val newScore = if (isCorrect) currentScore + 1 else 0 // Reset on error as per PRD

            flashcardRepository.updateProficiencyScore(question.flashcard.id, userId, newScore)
        }
    }

    fun continueToNext() {
        currentCardIndex++
        loadNextQuestion()
    }

    private fun validateAnswer(question: QuizQuestion, input: String): Boolean {
        return when (question) {
            is QuizQuestion.MultipleChoice -> input == question.flashcard.word
            is QuizQuestion.Scramble -> input.equals(question.flashcard.word, ignoreCase = true)
            is QuizQuestion.ExactTyping -> input.equals(question.flashcard.word, ignoreCase = true)
            is QuizQuestion.ContextualGapFill -> {
                val correct = question.options.getOrNull(question.correctOptionIndex)
                input.equals(correct, ignoreCase = true)
            }

            is QuizQuestion.SentenceBuilder -> input.trim()
                .equals(question.correctSentence.trim(), ignoreCase = true)

            is QuizQuestion.Dictation -> input.trim()
                .equals(question.flashcard.word, ignoreCase = true)
        }
    }

    fun restartQuiz() {
        currentCardIndex = 0
        currentStreak = 0
        quizResults.clear()
        loadNextQuestion()
    }
}

sealed class QuizUiEvent {
    data class NavigateToSummary(val topicId: String) : QuizUiEvent()
    data class ShowError(val message: String) : QuizUiEvent()
}
