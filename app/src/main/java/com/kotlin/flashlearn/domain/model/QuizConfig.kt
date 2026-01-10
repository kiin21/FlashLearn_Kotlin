package com.kotlin.flashlearn.domain.model

/**
 * Quiz configuration passed from UI to control generation strategy.
 */
enum class QuizMode {
    SPRINT,
    VSTEP_DRILL
}

data class QuizConfig(
    val mode: QuizMode = QuizMode.SPRINT,
    val questionCount: Int = 10
)

data class QuizResult(
    val flashcard: Flashcard,
    val isCorrect: Boolean
)
