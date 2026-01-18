package com.kotlin.flashlearn.domain.usecase

import com.kotlin.flashlearn.domain.logic.SmartDistractorGenerator
import com.kotlin.flashlearn.domain.model.Flashcard
import com.kotlin.flashlearn.domain.model.ProficiencyLevel
import com.kotlin.flashlearn.domain.model.QuizMode
import com.kotlin.flashlearn.domain.model.QuizQuestion
import javax.inject.Inject

/**
 * Use case to generate the appropriate quiz question based on the user's proficiency score.
 */
class GenerateQuestionUseCase @Inject constructor(
    private val distractorGenerator: SmartDistractorGenerator
) {
    suspend operator fun invoke(
        flashcard: Flashcard,
        score: Int,
        cardPool: List<Flashcard>,
        mode: QuizMode = QuizMode.SPRINT
    ): QuizQuestion {

        if (mode == QuizMode.VSTEP_DRILL) {
            return when ((1..3).random()) {
                1 -> generateGapFill(flashcard, cardPool)
                2 -> generateSentenceBuilder(flashcard)
                else -> if (!flashcard.pronunciationUrl.isNullOrBlank()) {
                    QuizQuestion.Dictation(
                        flashcard = flashcard,
                        audioUrl = flashcard.pronunciationUrl
                    )
                } else {
                    generateGapFill(flashcard, cardPool)
                }
            }
        }

        return when (ProficiencyLevel.fromScore(score)) {
            ProficiencyLevel.NEW -> {
                return when ((1..2).random()) {
                    1 -> generateMultipleChoice(flashcard, cardPool)
                    else -> {
                        if (flashcard.word.length > 7) return generateMultipleChoice(
                            flashcard,
                            cardPool
                        )
                        return generateScramble(flashcard)
                    }
                }
            }

            ProficiencyLevel.FAMILIAR -> generateGapFill(flashcard, cardPool)
            ProficiencyLevel.MASTERED -> QuizQuestion.ExactTyping(
                flashcard = flashcard,
                hint = flashcard.word.firstOrNull()?.toString()
            )
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
        val sentence =
            card.exampleSentence.takeIf { it.isNotBlank() } ?: "${'$'}{card.word} is the answer."
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
            hint = card.word.slice(IntRange(0, 3))
        )
    }
}
