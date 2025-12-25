package com.kotlin.flashlearn.domain.model

/**
 * Domain model representing a vocabulary topic.
 */
data class Topic(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val iconType: String = "book",
    val isSystemTopic: Boolean = true,
    val createdBy: String? = null,
    val wordCount: Int = 0 // Loaded from Datamuse API
)
