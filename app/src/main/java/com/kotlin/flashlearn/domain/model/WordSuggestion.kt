package com.kotlin.flashlearn.domain.model

/**
 * Domain model for word suggestion from autocomplete.
 */
data class WordSuggestion(
    val word: String,
    val score: Int = 0
)

/**
 * Domain model for vocabulary word with definition.
 */
data class VocabularyWord(
    val word: String,
    val partOfSpeech: String = "",
    val definition: String = "",
    val score: Int = 0
)
