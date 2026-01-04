package com.kotlin.flashlearn.domain.usecase

import com.kotlin.flashlearn.domain.logic.SmartDistractorGenerator
import com.kotlin.flashlearn.domain.model.Flashcard
import com.kotlin.flashlearn.domain.model.ProficiencyLevel
import com.kotlin.flashlearn.domain.model.QuizQuestion
import javax.inject.Inject

/**
 * Use case to generate the appropriate quiz question based on the user's proficiency score.
 */
class GenerateQuestionUseCase @Inject constructor(
    private val distractorGenerator: SmartDistractorGenerator
) {
    suspend operator fun invoke(flashcard: Flashcard, score: Int, cardPool: List<Flashcard>): QuizQuestion {
        return when (ProficiencyLevel.fromScore(score)) {
            ProficiencyLevel.NEW -> {
                val distractors = distractorGenerator.getDistractors(flashcard, cardPool)
                val options = (distractors + flashcard.word).shuffled()
                val correctIndex = options.indexOf(flashcard.word)
                
                QuizQuestion.MultipleChoice(
                    flashcard = flashcard,
                    options = options,
                    correctOptionIndex = correctIndex
                )
            }
            ProficiencyLevel.FAMILIAR -> {
                QuizQuestion.Scramble(
                    flashcard = flashcard,
                    shuffledLetters = flashcard.word.toList().shuffled()
                )
            }
            ProficiencyLevel.MASTERED -> {
                QuizQuestion.ExactTyping(
                    flashcard = flashcard,
                    hint = flashcard.word.firstOrNull()?.toString()
                )
            }
        }
    }
}
