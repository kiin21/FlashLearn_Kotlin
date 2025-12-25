package com.kotlin.flashlearn.domain.repository

import com.kotlin.flashlearn.domain.model.Flashcard

/**
 * Repository interface for flashcard operations.
 * Follows Clean Architecture - domain layer defines the contract,
 * data layer provides the implementation.
 */
interface FlashcardRepository {
    /**
     * Gets all flashcards for a specific topic.
     * @param topicId The ID of the topic.
     * @return List of flashcards belonging to the topic.
     */
    suspend fun getFlashcardsByTopicId(topicId: String): Result<List<Flashcard>>
    
    /**
     * Marks a flashcard as mastered (user clicked "Got It").
     * @param flashcardId The ID of the flashcard.
     * @param userId The ID of the current user.
     */
    suspend fun markFlashcardAsMastered(flashcardId: String, userId: String): Result<Unit>
    
    /**
     * Marks a flashcard for review (user clicked "Study Again").
     * @param flashcardId The ID of the flashcard.
     * @param userId The ID of the current user.
     */
    suspend fun markFlashcardForReview(flashcardId: String, userId: String): Result<Unit>
}
