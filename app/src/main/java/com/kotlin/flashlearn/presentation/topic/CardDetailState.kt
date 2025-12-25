package com.kotlin.flashlearn.presentation.topic

import com.kotlin.flashlearn.domain.model.Flashcard

data class CardDetailState(
    val isLoading: Boolean = true,
    val flashcard: Flashcard? = null,
    val isFlipped: Boolean = false,
    val error: String? = null
)