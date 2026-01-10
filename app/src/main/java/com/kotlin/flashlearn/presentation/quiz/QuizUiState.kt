package com.kotlin.flashlearn.presentation.quiz

import com.kotlin.flashlearn.domain.model.QuizQuestion
import com.kotlin.flashlearn.domain.model.QuizResult

data class QuizUiState(
    val isLoading: Boolean = false,
    val currentQuestion: QuizQuestion? = null,
    val isAnswerCorrect: Boolean? = null, // null = not answered, true = correct, false = wrong
    val showFeedback: Boolean = false,
    val error: String? = null,
    val currentIndex: Int = 0,
    val totalQuestions: Int = 0,
    val currentStreak: Int = 0,
    val results: List<QuizResult> = emptyList(),
    val isCompleted: Boolean = false
)
