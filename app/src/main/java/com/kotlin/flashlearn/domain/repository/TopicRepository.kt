package com.kotlin.flashlearn.domain.repository

import com.kotlin.flashlearn.domain.model.Topic

/**
 * Repository interface for topic operations.
 */
interface TopicRepository {
    /**
     * Get all public topics (system + user shared).
     */
    suspend fun getPublicTopics(): Result<List<Topic>>
    
    /**
     * Get topics created by a specific user (both private and public).
     */
    suspend fun getUserTopics(userId: String): Result<List<Topic>>
    
    /**
     * Get all topics visible to a user (public + their private).
     */
    suspend fun getVisibleTopics(userId: String?): Result<List<Topic>>
    
    /**
     * Get a single topic by ID.
     */
    suspend fun getTopicById(topicId: String): Result<Topic?>
    
    /**
     * Create a new user topic.
     */
    suspend fun createTopic(topic: Topic): Result<Topic>
    
    /**
     * Updates an existing topic.
     */
    suspend fun updateTopic(topic: Topic): Result<Topic>
    
    /**
     * Search topics by name (only public + user's own).
     */
    suspend fun searchTopics(query: String, userId: String?): Result<List<Topic>>
    
    // Legacy method for backward compatibility
    suspend fun getAllTopics(): Result<List<Topic>> = getPublicTopics()
    
    /**
     * Deletes a topic by its ID.
     */
    suspend fun deleteTopic(topicId: String): Result<Unit>

    /**
     * Regenerates the topic image by fetching a new one from Pixabay.
     */
    suspend fun regenerateTopicImage(topicId: String): Result<String>
    
    /**
     * Clones a topic (and its flashcards) to the user's collection.
     * Used for "Save to My Topics" feature in Community.
     * 
     * @param originalTopicId The topic to clone
     * @param targetUserId The user who will own the cloned topic
     * @param targetUserName The display name to set as creator
     * @return The newly created topic
     */
    suspend fun cloneTopicToUser(
        originalTopicId: String, 
        targetUserId: String,
        targetUserName: String
    ): Result<Topic>
    /**
     * Get top recommended public topics based on upvote count.
     */
    suspend fun getTopRecommendedTopics(limit: Int = 5): Result<List<Topic>>
}
