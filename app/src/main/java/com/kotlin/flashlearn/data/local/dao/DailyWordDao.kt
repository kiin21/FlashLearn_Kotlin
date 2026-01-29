package com.kotlin.flashlearn.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kotlin.flashlearn.data.local.entity.DailyWordHistoryEntity

@Dao
interface DailyWordDao {

    @Query(
        """
        SELECT * FROM daily_word_history
        WHERE userId = :userId AND dateKey = :dateKey
        LIMIT 1
    """
    )
    suspend fun getTodayEntry(userId: String, dateKey: String): DailyWordHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DailyWordHistoryEntity)

    @Query(
        """
        SELECT wordId FROM daily_word_history
        WHERE userId = :userId
    """
    )
    suspend fun getAllShownWordIds(userId: String): List<String>

    @Query(
        """
        SELECT * FROM daily_word_history
        WHERE userId = :userId
        ORDER BY dateKey DESC
    """
    )
    suspend fun getArchive(userId: String): List<DailyWordHistoryEntity>

    @Query(
        """
        SELECT * FROM daily_word_history
        WHERE userId = :userId
          AND (:fromDate IS NULL OR dateKey >= :fromDate)
          AND (:toDate IS NULL OR dateKey <= :toDate)
        ORDER BY dateKey DESC
    """
    )
    suspend fun getArchiveFiltered(
        userId: String,
        fromDate: String?,
        toDate: String?
    ): List<DailyWordHistoryEntity>

    @Query(
        """
        DELETE FROM daily_word_history
        WHERE userId = :userId
    """
    )
    suspend fun clearForUser(userId: String)
}
