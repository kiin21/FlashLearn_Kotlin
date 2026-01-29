package com.kotlin.flashlearn.presentation.quiz

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlin.flashlearn.domain.model.Flashcard
import com.kotlin.flashlearn.domain.model.QuizConfig
import com.kotlin.flashlearn.domain.model.QuizMode
import com.kotlin.flashlearn.domain.model.QuizQuestion
import com.kotlin.flashlearn.domain.model.QuizResult
import com.kotlin.flashlearn.domain.repository.AuthRepository
import com.kotlin.flashlearn.domain.repository.FlashcardRepository
import com.kotlin.flashlearn.domain.usecase.GenerateQuestionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject

@HiltViewModel
class QuizViewModel @Inject constructor(
    private val flashcardRepository: FlashcardRepository,
    private val authRepository: AuthRepository,
    private val generateQuestionUseCase: GenerateQuestionUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val TAG = "QuizViewModel"
    }

    private val topicId: String = savedStateHandle.get<String>("topicId") ?: ""
    private val initialMode: QuizMode = savedStateHandle.get<String>("mode")?.let {
        runCatching { QuizMode.valueOf(it) }.getOrDefault(QuizMode.SPRINT)
    } ?: QuizMode.SPRINT
    private val initialCount: Int = savedStateHandle.get<Int>("count") ?: 10
    private val initialSelectedIds: List<String> =
        savedStateHandle.get<String>("selectedIds")?.let { encoded ->
            Log.d(TAG, "Raw selectedIds param: $encoded")
            try {
                val decoded = URLDecoder.decode(encoded, StandardCharsets.UTF_8.toString())
                Log.d(TAG, "Decoded selectedIds: $decoded")
                if (decoded.isBlank()) emptyList() else decoded.split(",")
                    .filter { id -> id.isNotBlank() }
            } catch (e: Exception) {
                Log.e(TAG, "Error decoding selectedIds", e)
                emptyList()
            }
        } ?: emptyList()

    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<QuizUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private var flashcards: List<Flashcard> = emptyList()
    private var currentCardIndex = 0
    private var totalQuestions = 0
    private var currentStreak = 0
    private val quizResults = mutableListOf<QuizResult>()
    private var quizConfig: QuizConfig = QuizConfig(initialMode, initialCount, initialSelectedIds)

    val currentMode: QuizMode
        get() = quizConfig.mode

    init {
        Log.d(
            TAG,
            "QuizViewModel initialized - Mode: $initialMode, Count: $initialCount, SelectedIds: $initialSelectedIds"
        )
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

            val userId = authRepository.getSignedInUser()?.userId
            if (userId == null) {
                _uiState.update { it.copy(isLoading = false, error = "User not authenticated") }
                return@launch
            }

            flashcardRepository.getFlashcardsByTopicId(topicId).fold(
                onSuccess = { cards ->
                    // Filter cards based on quiz mode
                    flashcards = when (quizConfig.mode) {
                        QuizMode.SPRINT -> {
                            // Get mastered card IDs for this topic
                            val allCardIds = cards.map { it.id }
                            val masteredIds = flashcardRepository.getMasteredFlashcardIdsFromList(
                                userId,
                                allCardIds
                            ).getOrDefault(emptyList())

                            // Filter to only mastered cards
                            val masteredCards = cards.filter { it.id in masteredIds }

                            Log.d(TAG, "SPRINT mode - Total mastered cards: ${masteredCards.size}")

                            if (masteredCards.isEmpty()) {
                                emptyList()
                            } else if (masteredCards.size < 15) {
                                // If less than 15 mastered cards, use all of them
                                Log.d(
                                    TAG,
                                    "Using all ${masteredCards.size} mastered cards (less than 15)"
                                )
                                masteredCards
                            } else {
                                // If 15 or more, get proficiency scores and sort by weakest first
                                val cardsWithScores = masteredCards.map { card ->
                                    val score =
                                        flashcardRepository.getProficiencyScore(card.id, userId)
                                            .getOrDefault(0)
                                    card to score
                                }

                                // Sort by proficiency score (ascending = weakest first)
                                val sortedCards = cardsWithScores
                                    .sortedBy { it.second }
                                    .take(15)
                                    .map { it.first }

                                Log.d(
                                    TAG,
                                    "Selected 15 weakest cards from ${masteredCards.size} mastered cards"
                                )
                                sortedCards
                            }
                        }

                        QuizMode.CUSTOM -> {
                            // Use selected flashcard IDs
                            Log.d(
                                TAG,
                                "CUSTOM mode - Selected IDs: ${quizConfig.selectedFlashcardIds}"
                            )
                            Log.d(TAG, "CUSTOM mode - Available cards: ${cards.size}")
                            if (quizConfig.selectedFlashcardIds.isEmpty()) {
                                Log.d(TAG, "No IDs selected, using all cards shuffled")
                                cards.shuffled()
                            } else {
                                val filtered =
                                    cards.filter { it.id in quizConfig.selectedFlashcardIds }
                                Log.d(TAG, "Filtered to ${filtered.size} cards")
                                filtered
                            }
                        }
                    }

                    if (flashcards.isNotEmpty()) {
                        totalQuestions = flashcards.size
                        loadNextQuestion()
                    } else {
                        val errorMsg = when (quizConfig.mode) {
                            QuizMode.SPRINT -> "No mastered flashcards found. Complete a learning session first!"
                            QuizMode.CUSTOM -> "No flashcards selected"
                        }
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = errorMsg
                            )
                        }
                        // Emit event to show toast and navigate back
                        _uiEvent.emit(QuizUiEvent.ShowError(errorMsg))
                        _uiEvent.emit(QuizUiEvent.NavigateBack)
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
            val question = generateQuestionUseCase(card, score, flashcards)

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
            // Update proficiency score FIRST and wait for it to complete
            val currentScore =
                flashcardRepository.getProficiencyScore(question.flashcard.id, userId)
                    .getOrDefault(0)
            // Correct: +1, Wrong: -2 (minimum 0) - gentler penalty to preserve MASTERED status
            val newScore = if (isCorrect) {
                currentScore + 1
            } else {
                maxOf(0, currentScore - 2)
            }

            flashcardRepository.updateProficiencyScore(question.flashcard.id, userId, newScore)

            // Update UI AFTER score is persisted
            _uiState.update {
                it.copy(
                    isAnswerCorrect = isCorrect,
                    showFeedback = true,
                    currentStreak = currentStreak,
                    results = quizResults.toList()
                )
            }
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
    data object NavigateBack : QuizUiEvent()
}
