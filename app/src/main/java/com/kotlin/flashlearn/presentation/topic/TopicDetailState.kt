package com.kotlin.flashlearn.presentation.topic

import com.kotlin.flashlearn.domain.model.Flashcard

data class TopicDetailState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val topicTitle: String = "",
    val topicDescription: String = "",
    val cards: List<Flashcard> = emptyList(),
    val displayedCards: List<Flashcard> = emptyList(),
    val searchQuery: String = "",
    val isSelectionMode: Boolean = false,
    val selectedCardIds: Set<String> = emptySet(),
    val isOwner: Boolean = false,
    val imageUrl: String = "",
    val isPublic: Boolean = false,
    val isSystemTopic: Boolean = false
)