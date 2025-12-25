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
     * Search topics by name (only public + user's own).
     */
    suspend fun searchTopics(query: String, userId: String?): Result<List<Topic>>
    
    // Legacy method for backward compatibility
    suspend fun getAllTopics(): Result<List<Topic>> = getPublicTopics()
}
