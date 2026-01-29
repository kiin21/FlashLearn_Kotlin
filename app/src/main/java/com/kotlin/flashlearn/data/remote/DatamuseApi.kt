package com.kotlin.flashlearn.data.remote

import com.kotlin.flashlearn.data.remote.dto.WordSuggestionDto
import com.kotlin.flashlearn.data.remote.dto.WordWithDefinitionDto
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Datamuse API service for vocabulary search and suggestions.
 * Base URL: https://api.datamuse.com
 */
interface DatamuseApi {

    /**
     * Autocomplete suggestions as user types.
     * Example: /sug?s=voca → vocabulary, vocal, vocalize...
     */
    @GET("sug")
    suspend fun getSuggestions(
        @Query("s") prefix: String,
        @Query("max") max: Int = 10
    ): List<WordSuggestionDto>

    /**
     * Get words related to a topic with definitions.
     * Example: /words?topics=environment&md=d → pollution, ecosystem...
     */
    @GET("words")
    suspend fun getWordsByTopic(
        @Query("topics") topic: String,
        @Query("md") metadata: String = "d",
        @Query("max") max: Int = 20
    ): List<WordWithDefinitionDto>

    /**
     * Search words with spelling pattern and get definitions.
     * Example: /words?sp=bio*&md=d → biology, biodiversity...
     */
    @GET("words")
    suspend fun searchWords(
        @Query("sp") spelling: String,
        @Query("md") metadata: String = "d",
        @Query("max") max: Int = 20
    ): List<WordWithDefinitionDto>

    /**
     * Get words with similar meaning.
     * Example: /words?ml=happy&md=d → joyful, cheerful...
     */
    @GET("words")
    suspend fun getWordsByMeaning(
        @Query("ml") meaning: String,
        @Query("md") metadata: String = "d",
        @Query("max") max: Int = 20
    ): List<WordWithDefinitionDto>
}
