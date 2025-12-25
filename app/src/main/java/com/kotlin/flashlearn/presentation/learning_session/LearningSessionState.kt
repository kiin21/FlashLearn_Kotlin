package com.kotlin.flashlearn.presentation.learning_session

import com.kotlin.flashlearn.domain.model.Flashcard

/**
 * UI State for Learning Session screen.
 * Follows Unidirectional Data Flow (UDF) pattern.
 */
data class LearningSessionState(
    val isLoading: Boolean = false,
    val flashcards: List<Flashcard> = emptyList(),
    val currentCardIndex: Int = 0,
    val isCardFlipped: Boolean = false,
    val masteredCardIds: Set<String> = emptySet(),
    val error: String? = null
) {
    val currentCard: Flashcard?
        get() = flashcards.getOrNull(currentCardIndex)
    
    val progress: Float
        get() = if (flashcards.isEmpty()) 0f else (currentCardIndex + 1).toFloat() / flashcards.size.toFloat()
    
    val isSessionComplete: Boolean
        get() = currentCardIndex >= flashcards.size && flashcards.isNotEmpty()
    
    val progressText: String
        get() = "${currentCardIndex + 1} OF ${flashcards.size}"
}
