package com.kotlin.flashlearn.presentation.topic

import com.kotlin.flashlearn.domain.model.Flashcard

data class TopicDetailState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val topicTitle: String = "",
    val topicDescription: String = "",
    val cards: List<Flashcard> = emptyList()
)