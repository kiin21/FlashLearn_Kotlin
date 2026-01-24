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
     * Saves flashcards for a topic (used when creating a new topic with selected words).
     * @param topicId The ID of the topic.
     * @param flashcards List of flashcards to save.
     */
    suspend fun saveFlashcardsForTopic(topicId: String, flashcards: List<Flashcard>): Result<Unit>
    
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

    /**
    * Get flashcard by id.
    * @param cardId The ID of the flashcard.
    * @return flashcard data.
    */
    suspend fun getFlashcardById(cardId: String): Result<Flashcard?>

    /**
     * Deletes a list of flashcards by their IDs.
     * @param flashcardIds The list of flashcard IDs to delete.
     */
    suspend fun deleteFlashcards(flashcardIds: List<String>): Result<Unit>

    /**
     * Gets the proficiency score for a flashcard.
     * @param flashcardId The ID of the flashcard.
     * @param userId The ID of the current user.
     * @return The proficiency score (0 if not found).
     */
    suspend fun getProficiencyScore(flashcardId: String, userId: String): Result<Int>

    /**
     * Updates the proficiency score for a flashcard.
     * @param flashcardId The ID of the flashcard.
     * @param userId The ID of the current user.
     * @param newScore The new proficiency score.
     */
    suspend fun updateProficiencyScore(flashcardId: String, userId: String, newScore: Int): Result<Unit>

    /**
     * Enriches a flashcard with IPA and Image data.
     * @param card The flashcard to enrich.
     * @param force If true, fetches new data even if existing data is present.
     * @return The enriched flashcard.
     */
    suspend fun enrichFlashcard(card: Flashcard, force: Boolean = false): Flashcard
}
