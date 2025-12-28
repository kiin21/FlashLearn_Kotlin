package com.kotlin.flashlearn.data.repository

import com.kotlin.flashlearn.BuildConfig
import com.kotlin.flashlearn.data.remote.NeonSqlApi
import com.kotlin.flashlearn.data.remote.dto.NeonSqlRequest
import com.kotlin.flashlearn.data.remote.dto.TopicDto
import com.kotlin.flashlearn.domain.model.Topic
import com.kotlin.flashlearn.domain.repository.TopicRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/**
 * Implementation of TopicRepository using Neon SQL over HTTP API.
 */
@Singleton
class TopicRepositoryImpl @Inject constructor(
    private val neonSqlApi: NeonSqlApi
) : TopicRepository {
    
    companion object {
        private val CONNECTION_STRING = BuildConfig.NEON_CONNECTION_STRING
        private const val SELECT_COLUMNS = "id, name, description, icon_type, is_system_topic, is_public, created_by"
    }
    
    override suspend fun getPublicTopics(): Result<List<Topic>> {
        return try {
            val request = NeonSqlRequest(
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
            val request = NeonSqlRequest(
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
                NeonSqlRequest(
                    query = """
                        SELECT $SELECT_COLUMNS FROM topics 
                        WHERE is_system_topic = true OR is_public = true
                        ORDER BY is_system_topic DESC, name ASC
                    """.trimIndent()
                )
            } else {
                // Logged in - show public + user's private topics
                NeonSqlRequest(
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
            val request = NeonSqlRequest(
                query = "SELECT $SELECT_COLUMNS FROM topics WHERE id = \$1",
                params = listOf(topicId)
            )
            val response = neonSqlApi.executeQuery(
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
            val request = NeonSqlRequest(
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
            val response = neonSqlApi.executeQuery(
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
                NeonSqlRequest(
                    query = """
                        SELECT $SELECT_COLUMNS FROM topics 
                        WHERE (is_system_topic = true OR is_public = true) AND name ILIKE $1
                        ORDER BY is_system_topic DESC, name ASC
                    """.trimIndent(),
                    params = listOf("%$query%")
                )
            } else {
                NeonSqlRequest(
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
    
    private suspend fun executeAndMap(request: NeonSqlRequest): Result<List<Topic>> {
        val response = neonSqlApi.executeQuery(
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
