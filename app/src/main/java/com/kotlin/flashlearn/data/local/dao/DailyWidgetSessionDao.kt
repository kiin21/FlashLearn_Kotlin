package com.kotlin.flashlearn.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kotlin.flashlearn.data.local.entity.DailyWidgetSessionEntity

@Dao
interface DailyWidgetSessionDao {

    @Query("SELECT * FROM daily_widget_session WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): DailyWidgetSessionEntity?

    @Query("SELECT * FROM daily_widget_session WHERE userId = :userId AND date = :date LIMIT 1")
    suspend fun getByUserAndDate(userId: String, date: String): DailyWidgetSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DailyWidgetSessionEntity)

    @Query("DELETE FROM daily_widget_session WHERE userId = :userId")
    suspend fun clearByUser(userId: String)
}