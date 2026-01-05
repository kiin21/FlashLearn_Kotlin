package com.kotlin.flashlearn.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing user interactions with topics.
 * 
 * Two types of interactions:
 * 1. Favorite (Private) - Save topics for later, shown in "Favorites" tab
 * 2. Upvote (Public) - Vote for topics, affects ranking
 * 
 * Storage:
 * - Favorites: users/{userId}/favoriteTopics
 * - Upvotes: users/{userId}/upvotedTopics
 */
interface FavoriteRepository {
    
    // ==================== FAVORITE (Private Save) ====================
    
    /**
     * Toggles the favorite status of a topic.
     * Favorite = personal save, does NOT affect upvote count.
     * 
     * @return Result containing the new favorite status (true = saved)
     */
    suspend fun toggleFavorite(userId: String, topicId: String): Result<Boolean>
    
    /**
     * Gets all favorited topic IDs for a user (one-time fetch).
     */
    suspend fun getFavoriteTopicIdsOnce(userId: String): Result<List<String>>
    
    /**
     * Gets all favorited topic IDs as a Flow (real-time updates).
     */
    fun getFavoriteTopicIds(userId: String): Flow<List<String>>
    
    // ==================== UPVOTE (Public Voting) ====================
    
    /**
     * Toggles the upvote status of a topic.
     * Upvote = public vote, DOES affect upvoteCount on topic.
     * 
     * @return Result containing the new upvote status (true = upvoted)
     */
    suspend fun toggleUpvote(userId: String, topicId: String): Result<Boolean>
    
    /**
     * Gets all upvoted topic IDs for a user (one-time fetch).
     */
    suspend fun getUpvotedTopicIdsOnce(userId: String): Result<List<String>>
    
    /**
     * Gets all upvoted topic IDs as a Flow (real-time updates).
     */
    fun getUpvotedTopicIds(userId: String): Flow<List<String>>
}
