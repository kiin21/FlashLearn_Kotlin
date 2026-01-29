package com.kotlin.flashlearn.domain.usecase

import com.kotlin.flashlearn.domain.logic.SmartDistractorGenerator
import com.kotlin.flashlearn.domain.model.Flashcard
import com.kotlin.flashlearn.domain.model.ProficiencyLevel
import com.kotlin.flashlearn.domain.model.QuizQuestion
import javax.inject.Inject

/**
 * Use case to generate quiz questions with weighted random selection based on proficiency level.
 *
 * Question Type Pools by Proficiency:
 * - NEW (score 0-2): MultipleChoice, Scramble
 * - FAMILIAR (score 3-5): ContextualGapFill
 * - MASTERED (score 6+): ExactTyping, SentenceBuilder
 *
 * Weighted Distribution:
 * - NEW: 100% from NEW pool
 * - FAMILIAR: 60% NEW pool, 40% FAMILIAR pool
 * - MASTERED: 10% NEW pool, 40% FAMILIAR pool, 50% MASTERED pool
 */
class GenerateQuestionUseCase @Inject constructor(
    private val distractorGenerator: SmartDistractorGenerator
) {
    /**
     * Generates a question based on the flashcard's proficiency score.
     * Uses weighted random selection to ensure appropriate difficulty.
     */
    suspend operator fun invoke(
        flashcard: Flashcard,
        score: Int,
        cardPool: List<Flashcard>
    ): QuizQuestion {
        val proficiencyLevel = ProficiencyLevel.fromScore(score)

        return when (proficiencyLevel) {
            ProficiencyLevel.NEW -> {
                // 100% NEW types: MultipleChoice or Scramble
                generateNewLevelQuestion(flashcard, cardPool)
            }

            ProficiencyLevel.FAMILIAR -> {
                // 60% NEW, 40% FAMILIAR
                val random = (1..100).random()
                when {
                    random <= 60 -> generateNewLevelQuestion(flashcard, cardPool)
                    else -> generateFamiliarLevelQuestion(flashcard, cardPool)
                }
            }

            ProficiencyLevel.MASTERED -> {
                // 10% NEW, 40% FAMILIAR, 50% MASTERED
                val random = (1..100).random()
                when {
                    random <= 10 -> generateNewLevelQuestion(flashcard, cardPool)
                    random <= 50 -> generateFamiliarLevelQuestion(flashcard, cardPool)
                    else -> generateMasteredLevelQuestion(flashcard, cardPool)
                }
            }
        }
    }

    /**
     * Generates questions for NEW level: MultipleChoice or Scramble
     */
    private suspend fun generateNewLevelQuestion(
        flashcard: Flashcard,
        cardPool: List<Flashcard>
    ): QuizQuestion {
        // Check constraints: Scramble requires word length <= 7
        val canUseScramble = flashcard.word.length <= 7

        return if (canUseScramble && (1..2).random() == 1) {
            generateScramble(flashcard)
        } else {
            generateMultipleChoice(flashcard, cardPool)
        }
    }

    /**
     * Generates questions for FAMILIAR level: ContextualGapFill
     */
    private suspend fun generateFamiliarLevelQuestion(
        flashcard: Flashcard,
        cardPool: List<Flashcard>
    ): QuizQuestion {
        // Check constraints: GapFill requires exampleSentence
        return if (flashcard.exampleSentence.isNotBlank()) {
            generateGapFill(flashcard, cardPool)
        } else {
            // Fallback to NEW level question
            generateNewLevelQuestion(flashcard, cardPool)
        }
    }

    /**
     * Generates questions for MASTERED level: ExactTyping, SentenceBuilder, or Dictation
     */
    private suspend fun generateMasteredLevelQuestion(
        flashcard: Flashcard,
        cardPool: List<Flashcard>
    ): QuizQuestion {
        // Check constraints: SentenceBuilder requires exampleSentence
        val canUseSentenceBuilder = flashcard.exampleSentence.isNotBlank()

        // Weighted random: 40% ExactTyping, 30% SentenceBuilder (if available), 30% Dictation
        val random = (1..100).random()
        return when {
            random <= 40 -> generateExactTyping(flashcard)
            random <= 70 && canUseSentenceBuilder -> generateSentenceBuilder(flashcard)
            random <= 70 -> generateDictation(flashcard) // Fallback if no exampleSentence
            else -> generateDictation(flashcard)
        }
    }

    private suspend fun generateMultipleChoice(
        card: Flashcard,
        pool: List<Flashcard>
    ): QuizQuestion.MultipleChoice {
        val distractors = distractorGenerator.getDistractors(card, pool)
        val options = (distractors + card.word).shuffled()
        val correctIndex = options.indexOf(card.word)

        return QuizQuestion.MultipleChoice(
            flashcard = card,
            options = options,
            correctOptionIndex = correctIndex
        )
    }

    private suspend fun generateScramble(card: Flashcard): QuizQuestion.Scramble {
        val word = card.word
        val scrambleChars: List<Char> = word.toList().shuffled()
        return QuizQuestion.Scramble(
            card, scrambleChars
        )
    }

    private suspend fun generateGapFill(
        card: Flashcard,
        pool: List<Flashcard>
    ): QuizQuestion.ContextualGapFill {
        val baseSentence = card.exampleSentence.takeIf { it.isNotBlank() }
            ?: "Definition: ${'$'}{card.definition}\nWord: _______"

        val blankedSentence = baseSentence.replace(card.word, "_______", ignoreCase = true)
        val distractors = distractorGenerator.getDistractors(card, pool)
        val options = (distractors + card.word).shuffled()

        return QuizQuestion.ContextualGapFill(
            flashcard = card,
            sentenceWithBlank = blankedSentence,
            options = options,
            correctOptionIndex = options.indexOf(card.word)
        )
    }

    private fun generateSentenceBuilder(card: Flashcard): QuizQuestion.SentenceBuilder {
        val sentence = card.exampleSentence.takeIf { it.isNotBlank() }
            ?: "The word ${card.word} is used here."
        val segments = sentence.split(" ").shuffled()

        return QuizQuestion.SentenceBuilder(
            flashcard = card,
            scrambledSegments = segments,
            correctSentence = sentence
        )
    }

    private fun generateExactTyping(card: Flashcard): QuizQuestion.ExactTyping {
        return QuizQuestion.ExactTyping(
            flashcard = card,
            hint = card.word.firstOrNull()?.toString() ?: ""
        )
    }

    private fun generateDictation(card: Flashcard): QuizQuestion.Dictation {
        return QuizQuestion.Dictation(
            flashcard = card
        )
    }
}
