package com.kotlin.flashlearn.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kotlin.flashlearn.data.local.entity.WidgetWordHistoryEntity

@Dao
interface WidgetWordHistoryDao {

    @Query("SELECT 1 FROM widget_word_history WHERE userId = :userId AND flashcardId = :flashcardId LIMIT 1")
    suspend fun exists(userId: String, flashcardId: String): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: WidgetWordHistoryEntity)

    @Query("SELECT flashcardId FROM widget_word_history WHERE userId = :userId")
    suspend fun getAllShownIds(userId: String): List<String>

    @Query("SELECT flashcardId FROM widget_word_history WHERE userId = :userId AND isCorrect = 1")
    suspend fun getAllCorrectIds(userId: String): List<String>

    @Query("DELETE FROM widget_word_history WHERE userId = :userId")
    suspend fun clearByUser(userId: String)
}
