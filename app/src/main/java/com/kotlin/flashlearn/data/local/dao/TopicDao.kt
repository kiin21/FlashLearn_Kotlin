package com.kotlin.flashlearn.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kotlin.flashlearn.data.local.entity.TopicEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for topic operations.
 * Provides reactive Flow for real-time UI updates.
 */
@Dao
interface TopicDao {

    @Query("SELECT * FROM topics ORDER BY name ASC")
    fun getAllTopics(): Flow<List<TopicEntity>>

    @Query("SELECT * FROM topics ORDER BY name ASC")
    suspend fun getAllTopicsOnce(): List<TopicEntity>

    @Query("SELECT * FROM topics WHERE isSystemTopic = 1 ORDER BY name ASC")
    fun getSystemTopics(): Flow<List<TopicEntity>>

    @Query("SELECT * FROM topics WHERE createdBy = :userId ORDER BY name ASC")
    fun getUserTopics(userId: String): Flow<List<TopicEntity>>

    @Query("SELECT * FROM topics WHERE id = :id")
    suspend fun getTopicById(id: String): TopicEntity?

    @Query("SELECT * FROM topics WHERE name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    suspend fun searchTopics(query: String): List<TopicEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopic(topic: TopicEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopics(topics: List<TopicEntity>)

    @Update
    suspend fun updateTopic(topic: TopicEntity)

    @Query("DELETE FROM topics WHERE id = :id")
    suspend fun deleteTopic(id: String)

    @Query("DELETE FROM topics")
    suspend fun deleteAllTopics()
}
