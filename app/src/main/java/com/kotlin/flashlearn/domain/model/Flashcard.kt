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
    val synonyms: List<String> = emptyList(),
    val level: String = "", // VSTEP/CEFR level: "A1", "A2", "B1", "B2", "C1", "C2" (empty = unclassified)
    val createdAt: Long = System.currentTimeMillis()
)
