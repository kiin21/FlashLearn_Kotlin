package com.kotlin.flashlearn.domain.repository

import com.kotlin.flashlearn.domain.model.Topic

/**
 * Repository interface for topic operations.
 */
interface TopicRepository {
    /**
     * Get all topics (system + user-created).
     */
    suspend fun getAllTopics(): Result<List<Topic>>
    
    /**
     * Get topics created by a specific user.
     */
    suspend fun getUserTopics(userId: String): Result<List<Topic>>
    
    /**
     * Get a single topic by ID.
     */
    suspend fun getTopicById(topicId: String): Result<Topic?>
    
    /**
     * Create a new user topic.
     */
    suspend fun createTopic(topic: Topic): Result<Topic>
    
    /**
     * Search topics by name.
     */
    suspend fun searchTopics(query: String): Result<List<Topic>>
}
