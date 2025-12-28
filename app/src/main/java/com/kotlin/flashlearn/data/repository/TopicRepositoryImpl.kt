package com.kotlin.flashlearn.data.repository

import com.kotlin.flashlearn.BuildConfig
import com.kotlin.flashlearn.data.remote.PostgresApi
import com.kotlin.flashlearn.data.remote.dto.PostgresSqlRequest
import com.kotlin.flashlearn.data.remote.dto.TopicDto
import com.kotlin.flashlearn.domain.model.Topic
import com.kotlin.flashlearn.domain.repository.TopicRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/**
 * Implementation of TopicRepository using Postgres SQL over HTTP API.
 */
@Singleton
class TopicRepositoryImpl @Inject constructor(
    private val postgresApi: PostgresApi
) : TopicRepository {
    
    companion object {
        private val CONNECTION_STRING = BuildConfig.NEON_CONNECTION_STRING
        private const val SELECT_COLUMNS = "id, name, description, icon_type, is_system_topic, is_public, created_by"
    }
    
    override suspend fun getPublicTopics(): Result<List<Topic>> {
        return try {
            val request = PostgresSqlRequest(
                query = """
                    SELECT $SELECT_COLUMNS FROM topics 
                    WHERE is_system_topic = true OR is_public = true
                    ORDER BY is_system_topic DESC, name ASC
                """.trimIndent()
            )
            executeAndMap(request)
        } catch (e: Exception) {
            handleException(e)
        }
    }
    
    override suspend fun getUserTopics(userId: String): Result<List<Topic>> {
        return try {
            val request = PostgresSqlRequest(
                query = "SELECT $SELECT_COLUMNS FROM topics WHERE created_by = \$1 ORDER BY name ASC",
                params = listOf(userId)
            )
            executeAndMap(request)
        } catch (e: Exception) {
            handleException(e)
        }
    }
    
    override suspend fun getVisibleTopics(userId: String?): Result<List<Topic>> {
        return try {
            val request = if (userId.isNullOrBlank()) {
                // Not logged in - only show public topics
                PostgresSqlRequest(
                    query = """
                        SELECT $SELECT_COLUMNS FROM topics 
                        WHERE is_system_topic = true OR is_public = true
                        ORDER BY is_system_topic DESC, name ASC
                    """.trimIndent()
                )
            } else {
                // Logged in - show public + user's private topics
                PostgresSqlRequest(
                    query = """
                        SELECT $SELECT_COLUMNS FROM topics 
                        WHERE is_system_topic = true OR is_public = true OR created_by = ${"$"}1
                        ORDER BY is_system_topic DESC, name ASC
                    """.trimIndent(),
                    params = listOf(userId)
                )
            }
            executeAndMap(request)
        } catch (e: Exception) {
            handleException(e)
        }
    }
    
    override suspend fun getTopicById(topicId: String): Result<Topic?> {
        return try {
            val request = PostgresSqlRequest(
                query = "SELECT $SELECT_COLUMNS FROM topics WHERE id = \$1",
                params = listOf(topicId)
            )
            val response = postgresApi.executeQuery(
                connectionString = CONNECTION_STRING,
                request = request
            )
            
            if (response.error != null) {
                return Result.failure(Exception(response.error.message))
            }
            
            val topic = response.rows?.firstOrNull()?.let { row ->
                TopicDto.fromRow(row).toDomain()
            }
            
            Result.success(topic)
        } catch (e: Exception) {
            handleException(e)
        }
    }
    
    override suspend fun createTopic(topic: Topic): Result<Topic> {
        return try {
            val id = if (topic.id.isBlank()) UUID.randomUUID().toString() else topic.id
            val request = PostgresSqlRequest(
                query = """
                    INSERT INTO topics (id, name, description, icon_type, is_system_topic, is_public, created_by)
                    VALUES ($1, $2, $3, $4, $5, $6, $7)
                    RETURNING $SELECT_COLUMNS
                """.trimIndent(),
                params = listOf(
                    id,
                    topic.name,
                    topic.description,
                    topic.iconType,
                    topic.isSystemTopic,
                    topic.isPublic,
                    topic.createdBy ?: ""
                )
            )
            val response = postgresApi.executeQuery(
                connectionString = CONNECTION_STRING,
                request = request
            )
            
            if (response.error != null) {
                return Result.failure(Exception(response.error.message))
            }
            
            val createdTopic = response.rows?.firstOrNull()?.let { row ->
                TopicDto.fromRow(row).toDomain()
            } ?: topic.copy(id = id)
            
            Result.success(createdTopic)
        } catch (e: Exception) {
            handleException(e)
        }
    }
    
    override suspend fun searchTopics(query: String, userId: String?): Result<List<Topic>> {
        return try {
            val request = if (userId.isNullOrBlank()) {
                PostgresSqlRequest(
                    query = """
                        SELECT $SELECT_COLUMNS FROM topics 
                        WHERE (is_system_topic = true OR is_public = true) AND name ILIKE $1
                        ORDER BY is_system_topic DESC, name ASC
                    """.trimIndent(),
                    params = listOf("%$query%")
                )
            } else {
                PostgresSqlRequest(
                    query = """
                        SELECT $SELECT_COLUMNS FROM topics 
                        WHERE (is_system_topic = true OR is_public = true OR created_by = $2) AND name ILIKE $1
                        ORDER BY is_system_topic DESC, name ASC
                    """.trimIndent(),
                    params = listOf("%$query%", userId)
                )
            }
            executeAndMap(request)
        } catch (e: Exception) {
            handleException(e)
        }
    }
    
    private suspend fun executeAndMap(request: PostgresSqlRequest): Result<List<Topic>> {
        val response = postgresApi.executeQuery(
            connectionString = CONNECTION_STRING,
            request = request
        )
        
        if (response.error != null) {
            return Result.failure(Exception(response.error.message))
        }
        
        val topics = response.rows?.map { row ->
            TopicDto.fromRow(row).toDomain()
        } ?: emptyList()
        
        return Result.success(topics)
    }
    
    private fun <T> handleException(e: Exception): Result<T> {
        e.printStackTrace()
        if (e is CancellationException) throw e
        return Result.failure(e)
    }
    
    override suspend fun deleteTopic(topicId: String): Result<Unit> {
        return try {
            val request = PostgresSqlRequest(
                query = "DELETE FROM topics WHERE id = \$1",
                params = listOf(topicId)
            )
            val response = postgresApi.executeQuery(
                connectionString = CONNECTION_STRING,
                request = request
            )
            
            if (response.error != null) {
                return Result.failure(Exception(response.error.message))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleException(e)
        }
    }

    private fun TopicDto.toDomain(): Topic {
        return Topic(
            id = id,
            name = name,
            description = description ?: "",
            iconType = iconType ?: "book",
            isSystemTopic = isSystemTopic,
            isPublic = isPublic,
            createdBy = createdBy
        )
    }
}
