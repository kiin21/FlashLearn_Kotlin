package com.kotlin.flashlearn.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kotlin.flashlearn.data.local.entity.ProgressStatus
import com.kotlin.flashlearn.data.local.entity.UserProgressEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for user progress tracking.
 * Persists mastered/review status to survive app restarts.
 */
@Dao
interface UserProgressDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: UserProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(progressList: List<UserProgressEntity>)

    @Query("SELECT * FROM user_progress WHERE userId = :userId")
    fun getProgressByUser(userId: String): Flow<List<UserProgressEntity>>

    @Query("SELECT * FROM user_progress WHERE userId = :userId AND flashcardId = :flashcardId")
    suspend fun getProgress(userId: String, flashcardId: String): UserProgressEntity?

    @Query("SELECT flashcardId FROM user_progress WHERE userId = :userId AND status = :status")
    suspend fun getFlashcardIdsByStatus(userId: String, status: ProgressStatus): List<String>

    @Query("SELECT flashcardId FROM user_progress WHERE userId = :userId AND status = 'MASTERED'")
    fun getMasteredFlashcardIds(userId: String): Flow<List<String>>

    @Query("SELECT flashcardId FROM user_progress WHERE userId = :userId AND status = 'REVIEW'")
    fun getReviewFlashcardIds(userId: String): Flow<List<String>>

    @Query("UPDATE user_progress SET syncedToRemote = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)

    @Query("SELECT * FROM user_progress WHERE syncedToRemote = 0 LIMIT :limit")
    suspend fun getUnsyncedProgress(limit: Int = 50): List<UserProgressEntity>

    @Query("DELETE FROM user_progress WHERE userId = :userId AND flashcardId = :flashcardId")
    suspend fun deleteProgress(userId: String, flashcardId: String)

    @Query("DELETE FROM user_progress WHERE userId = :userId")
    suspend fun deleteAllProgressForUser(userId: String)
}
