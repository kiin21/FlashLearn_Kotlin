package com.kotlin.flashlearn.domain.model

/**
 * Quiz configuration passed from UI to control generation strategy.
 */
enum class QuizMode {
    SPRINT,      // Quick Sprint: 15 mastered cards with weighted question generation
    CUSTOM       // Custom: User-selected cards with weighted question generation
}

data class QuizConfig(
    val mode: QuizMode = QuizMode.SPRINT,
    val questionCount: Int = 15,
    val selectedFlashcardIds: List<String> = emptyList()  // For CUSTOM mode
)

data class QuizResult(
    val flashcard: Flashcard,
    val isCorrect: Boolean
)
