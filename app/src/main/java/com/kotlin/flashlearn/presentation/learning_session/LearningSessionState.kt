package com.kotlin.flashlearn.presentation.learning_session

import com.kotlin.flashlearn.domain.model.Flashcard

/**
 * UI State for Learning Session screen.
 * Follows Unidirectional Data Flow (UDF) pattern.
 */
data class LearningSessionState(
    val isLoading: Boolean = false,
    val sessionQueue: List<Flashcard> = emptyList(),
    val initialCardCount: Int = 0,
    val completedCardCount: Int = 0, // Total cards reviewed (swiped left or right)
    val masteredCardCount: Int = 0, // Only cards swiped right (remembered)
    val isCardFlipped: Boolean = false,
    val error: String? = null,
    val previousState: LearningSessionState? = null // For Undo
) {
    val currentCard: Flashcard?
        get() = sessionQueue.firstOrNull()

    val progress: Float
        get() = if (initialCardCount == 0) 0f else completedCardCount.toFloat() / initialCardCount.toFloat()

    val isSessionComplete: Boolean
        get() = sessionQueue.isEmpty() && initialCardCount > 0

    val progressText: String
        get() = "$completedCardCount OF $initialCardCount"
}
