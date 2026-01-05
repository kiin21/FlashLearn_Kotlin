package com.kotlin.flashlearn.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing user favorites (upvotes).
 * Favorites are stored in Firestore under users/{userId}/favoriteTopics.
 * 
 * Rules:
 * - One vote per user per topic
 * - Toggling favorite also updates the topic's upvoteCount
 */
interface FavoriteRepository {
    
    /**
     * Adds a topic to user's favorites.
     * Also increments the topic's upvoteCount.
     * 
     * @param userId The current user's ID
     * @param topicId The topic ID to favorite
     * @return Result indicating success or failure
     */
    suspend fun addFavorite(userId: String, topicId: String): Result<Unit>
    
    /**
     * Removes a topic from user's favorites.
     * Also decrements the topic's upvoteCount.
     * 
     * @param userId The current user's ID
     * @param topicId The topic ID to unfavorite
     * @return Result indicating success or failure
     */
    suspend fun removeFavorite(userId: String, topicId: String): Result<Unit>
    
    /**
     * Toggles the favorite status of a topic.
     * If favorited, removes it. If not, adds it.
     * 
     * @param userId The current user's ID
     * @param topicId The topic ID to toggle
     * @return Result containing the new favorite status (true = favorited)
     */
    suspend fun toggleFavorite(userId: String, topicId: String): Result<Boolean>
    
    /**
     * Checks if a topic is favorited by the user.
     * 
     * @param userId The current user's ID
     * @param topicId The topic ID to check
     * @return Result containing true if favorited, false otherwise
     */
    suspend fun isFavorited(userId: String, topicId: String): Result<Boolean>
    
    /**
     * Gets all favorited topic IDs for a user as a Flow.
     * Updates in real-time when favorites change.
     * 
     * @param userId The current user's ID
     * @return Flow of list of topic IDs
     */
    fun getFavoriteTopicIds(userId: String): Flow<List<String>>
    
    /**
     * Gets all favorited topic IDs for a user (one-time fetch).
     * 
     * @param userId The current user's ID
     * @return Result containing list of topic IDs
     */
    suspend fun getFavoriteTopicIdsOnce(userId: String): Result<List<String>>
}
