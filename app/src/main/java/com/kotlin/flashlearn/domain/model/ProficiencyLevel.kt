package com.kotlin.flashlearn.domain.model

/**
 * Represents the user's mastery level of a specific word.
 * Determined by the proficiency_score.
 */
enum class ProficiencyLevel {
    NEW,      // Score 0-2: Multiple Choice (Recognition)
    FAMILIAR, // Score 3-5: Scramble (Construction)
    MASTERED; // Score 6+: Exact Typing (Recall)

    companion object {
        fun fromScore(score: Int): ProficiencyLevel = when (score) {
            in 0..2 -> NEW
            in 3..5 -> FAMILIAR
            else -> MASTERED
        }
    }
}
