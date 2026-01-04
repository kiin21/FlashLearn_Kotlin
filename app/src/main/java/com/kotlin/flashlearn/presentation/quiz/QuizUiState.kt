package com.kotlin.flashlearn.presentation.quiz

import com.kotlin.flashlearn.domain.model.QuizQuestion

data class QuizUiState(
    val isLoading: Boolean = false,
    val currentQuestion: QuizQuestion? = null,
    val isAnswerCorrect: Boolean? = null, // null = not answered, true = correct, false = wrong
    val showFeedback: Boolean = false,
    val error: String? = null
)
