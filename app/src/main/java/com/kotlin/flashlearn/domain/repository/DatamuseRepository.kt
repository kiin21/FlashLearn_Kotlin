package com.kotlin.flashlearn.domain.repository

import com.kotlin.flashlearn.domain.model.VocabularyWord
import com.kotlin.flashlearn.domain.model.WordSuggestion

/**
 * Repository interface for Datamuse vocabulary operations.
 */
interface DatamuseRepository {
    /**
     * Get autocomplete suggestions as user types.
     * @param prefix The characters user has typed so far.
     */
    suspend fun getAutocompleteSuggestions(prefix: String): Result<List<WordSuggestion>>

    /**
     * Get vocabulary words related to a topic.
     * @param topic The topic keyword (e.g., "environment", "technology").
     */
    suspend fun getWordsByTopic(topic: String): Result<List<VocabularyWord>>

    /**
     * Search words by spelling pattern.
     * @param pattern Spelling pattern with wildcards (e.g., "bio*", "*tion").
     */
    suspend fun searchWords(pattern: String): Result<List<VocabularyWord>>

    /**
     * Get words with similar meaning.
     * @param meaning The meaning/concept to search for.
     */
    suspend fun getWordsByMeaning(meaning: String): Result<List<VocabularyWord>>
}
