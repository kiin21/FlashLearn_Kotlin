package com.kotlin.flashlearn.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kotlin.flashlearn.data.local.entity.UserStreakEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserStreakDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: UserStreakEntity)

    @Query("SELECT * FROM user_streak WHERE userId = :userId LIMIT 1")
    suspend fun get(userId: String): UserStreakEntity?

    @Query("SELECT * FROM user_streak WHERE userId = :userId LIMIT 1")
    fun observe(userId: String): Flow<UserStreakEntity?>
}