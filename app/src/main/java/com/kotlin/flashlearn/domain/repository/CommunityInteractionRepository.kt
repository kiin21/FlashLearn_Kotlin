package com.kotlin.flashlearn.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing user interactions with community topics.
 * 
 * Two types of interactions:
 * 1. Bookmark (Private) - Save topics for later, shown in "Saved" tab in Community (previously Favorites)
 * 2. Upvote (Public) - Vote for topics, affects ranking
 * 
 * Storage:
 * - Bookmarks: users/{userId}/savedCommunityTopics
 * - Upvotes: users/{userId}/upvotedTopics
 */
interface CommunityInteractionRepository {
    
    // ==================== BOOKMARK (Private Save) ====================
    
    /**
     * Toggles the bookmark status of a topic (Save for later).
     * Bookmark = personal save, does NOT affect upvote count.
     * 
     * @return Result containing the new bookmark status (true = saved)
     */
    suspend fun toggleBookmark(userId: String, topicId: String): Result<Boolean>
    
    /**
     * Gets all bookmarked topic IDs for a user (one-time fetch).
     */
    suspend fun getBookmarkedTopicIdsOnce(userId: String): Result<List<String>>
    
    /**
     * Gets all bookmarked topic IDs as a Flow (real-time updates).
     */
    fun getBookmarkedTopicIds(userId: String): Flow<List<String>>
    
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
