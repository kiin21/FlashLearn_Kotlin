package com.kotlin.flashlearn.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kotlin.flashlearn.data.local.entity.FlashcardEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for flashcard operations.
 * Provides reactive Flow for real-time UI updates.
 */
@Dao
interface FlashcardDao {

    @Query("SELECT * FROM flashcards WHERE topicId = :topicId ORDER BY createdAt ASC")
    fun getFlashcardsByTopicId(topicId: String): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards WHERE topicId = :topicId ORDER BY createdAt ASC")
    suspend fun getFlashcardsByTopicIdOnce(topicId: String): List<FlashcardEntity>

    @Query("SELECT * FROM flashcards WHERE id = :id")
    suspend fun getFlashcardById(id: String): FlashcardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcard(flashcard: FlashcardEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcards(flashcards: List<FlashcardEntity>)

    @Update
    suspend fun updateFlashcard(flashcard: FlashcardEntity)

    @Query("DELETE FROM flashcards WHERE id = :id")
    suspend fun deleteFlashcard(id: String)

    @Query("DELETE FROM flashcards WHERE id IN (:ids)")
    suspend fun deleteFlashcards(ids: List<String>)

    @Query("DELETE FROM flashcards WHERE topicId = :topicId")
    suspend fun deleteFlashcardsByTopicId(topicId: String)

    @Query("SELECT COUNT(*) FROM flashcards WHERE topicId = :topicId")
    suspend fun getFlashcardCountByTopicId(topicId: String): Int

    /**
     * Check if flashcards for a topic need enrichment (missing IPA or image).
     */
    @Query("SELECT * FROM flashcards WHERE topicId = :topicId AND (ipa = '' OR imageUrl = '') LIMIT :limit")
    suspend fun getFlashcardsNeedingEnrichment(
        topicId: String,
        limit: Int = 5
    ): List<FlashcardEntity>

    @Query(
        """
        SELECT f.id AS id,
               f.word AS word,
               f.level AS level,
               f.definition AS meaning,
               f.ipa AS ipa
        FROM flashcards f
        INNER JOIN topics t ON t.id = f.topicId
        WHERE t.isSystemTopic = 1
           OR t.createdBy = :userId
    """
    )
    suspend fun getDailyWordCandidates(userId: String): List<DailyWordCandidate>
}
