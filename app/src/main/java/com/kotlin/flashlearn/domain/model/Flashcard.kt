package com.kotlin.flashlearn.domain.model

/**
 * Domain model representing a flashcard in the learning system.
 * Each flashcard belongs to a specific topic and contains vocabulary information.
 */
data class Flashcard(
    val id: String = "",
    val topicId: String = "",
    val word: String = "",
    val pronunciation: String = "",
    val partOfSpeech: String = "", // e.g., "NOUN", "VERB", "ADJECTIVE"
    val definition: String = "",
    val exampleSentence: String = "",
    val ipa: String = "",
    val imageUrl: String = "",
    val pronunciationUrl: String? = null,
    val synonyms: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)
