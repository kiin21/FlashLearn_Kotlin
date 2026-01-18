package com.kotlin.flashlearn.domain.logic

import com.kotlin.flashlearn.domain.model.Flashcard
import javax.inject.Inject
import kotlin.math.min

/**
 * Strategy interface for generating distractors (wrong answers) for Multiple Choice questions.
 */
interface DistractorStrategy {
    suspend fun generate(target: Flashcard, allCards: List<Flashcard>, count: Int): List<String>
}

/**
 * Priority A: Visual Similarity.
 * Finds words with Levenshtein Distance < 3.
 */
class LevenshteinDistractorStrategy @Inject constructor() : DistractorStrategy {
    override suspend fun generate(target: Flashcard, allCards: List<Flashcard>, count: Int): List<String> {
        return allCards
            .filter { it.id != target.id }
            .filter { calculateLevenshteinDistance(target.word, it.word) < 3 }
            .map { it.word }
            .shuffled()
            .take(count)
    }

    private fun calculateLevenshteinDistance(s1: String, s2: String): Int {
        val m = s1.length
        val n = s2.length
        val dp = Array(m + 1) { IntArray(n + 1) }

        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j

        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = min(
                    min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[m][n]
    }
}

/**
 * Priority B: Semantic Similarity.
 * Finds words sharing the same part of speech (or tags if available).
 */
class SemanticDistractorStrategy @Inject constructor() : DistractorStrategy {
    override suspend fun generate(target: Flashcard, allCards: List<Flashcard>, count: Int): List<String> {
        return allCards
            .filter { it.id != target.id }
            .filter { it.partOfSpeech == target.partOfSpeech } // Simple semantic check
            .map { it.word }
            .shuffled()
            .take(count)
    }
}

/**
 * Fallback: Random words from the deck.
 */
class RandomDistractorStrategy @Inject constructor() : DistractorStrategy {
    override suspend fun generate(target: Flashcard, allCards: List<Flashcard>, count: Int): List<String> {
        return allCards
            .filter { it.id != target.id }
            .map { it.word }
            .shuffled()
            .take(count)
    }
}

/**
 * Composite Generator that chains strategies to find the best distractors.
 */
class SmartDistractorGenerator @Inject constructor(
    private val levenshteinStrategy: LevenshteinDistractorStrategy,
    private val semanticStrategy: SemanticDistractorStrategy,
    private val randomStrategy: RandomDistractorStrategy
) {
    suspend fun getDistractors(target: Flashcard, pool: List<Flashcard>, count: Int = 3): List<String> {
        val distractors = mutableSetOf<String>()

        // 1. Try Levenshtein
        distractors.addAll(levenshteinStrategy.generate(target, pool, count))
        if (distractors.size >= count) return distractors.take(count).toList()

        // 2. Fill with Semantic
        val neededAfterLevenshtein = count - distractors.size
        distractors.addAll(semanticStrategy.generate(target, pool, neededAfterLevenshtein))
        if (distractors.size >= count) return distractors.take(count).toList()

        // 3. Fill with Random
        val neededAfterSemantic = count - distractors.size
        distractors.addAll(randomStrategy.generate(target, pool, neededAfterSemantic))

        return distractors.take(count).toList()
    }
}
