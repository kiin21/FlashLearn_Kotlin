package com.kotlin.flashlearn.domain.model

/**
 * Sealed class representing the different types of questions in the adaptive quiz.
 * Follows the Open/Closed Principle - new question types can be added here.
 */
sealed class QuizQuestion {
    abstract val flashcard: Flashcard

    /**
     * Level 1: Multiple Choice (Recognition)
     * Used for NEW words (Score 0-2).
     */
    data class MultipleChoice(
        override val flashcard: Flashcard,
        val options: List<String>, // 1 correct + 3 distractors
        val correctOptionIndex: Int
    ) : QuizQuestion()

    /**
     * Level 2: Scrambled Letters (Construction)
     * Used for FAMILIAR words (Score 3-5).
     */
    data class Scramble(
        override val flashcard: Flashcard,
        val shuffledLetters: List<Char>
    ) : QuizQuestion()

    /**
     * Level 3: Type-in Answer (Total Recall)
     * Used for MASTERED words (Score 6+).
     */
    data class ExactTyping(
        override val flashcard: Flashcard,
        val hint: String? = null
    ) : QuizQuestion()

    // --- New VSTEP-inspired types ---
    /**
     * Writing skill: reorder segments to build the sentence.
     */
    data class SentenceBuilder(
        override val flashcard: Flashcard,
        val scrambledSegments: List<String>,
        val correctSentence: String
    ) : QuizQuestion()

    /**
     * Reading skill: choose the correct word to fill the blank.
     */
    data class ContextualGapFill(
        override val flashcard: Flashcard,
        val sentenceWithBlank: String,
        val options: List<String>,
        val correctOptionIndex: Int
    ) : QuizQuestion()

    /**
     * Listening skill: listen and type the word.
     * Uses Android TTS (Text-to-Speech) to pronounce the word.
     */
    data class Dictation(
        override val flashcard: Flashcard
    ) : QuizQuestion()
}
