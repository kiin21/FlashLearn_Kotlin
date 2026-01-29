package com.kotlin.flashlearn.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kotlin.flashlearn.data.local.entity.UserStreakEntity

@Dao
interface UserStreakDao {

    @Query("SELECT * FROM user_streak WHERE userId = :userId LIMIT 1")
    suspend fun getByUserId(userId: String): UserStreakEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: UserStreakEntity)

    suspend fun insert(
        userId: String,
        currentStreak: Int,
        best: Int,
        lastActiveDate: String
    ) = insert(
        UserStreakEntity(
            userId = userId,
            currentStreak = currentStreak,
            lastActiveDate = lastActiveDate
        )
    )

    @Query(
        """
        UPDATE user_streak 
        SET currentStreak = :currentStreak,
            lastActiveDate = :lastActiveDate,
            updatedAt = :updatedAt
        WHERE userId = :userId
    """
    )
    suspend fun update(
        userId: String,
        currentStreak: Int,
        lastActiveDate: String,
        updatedAt: Long = System.currentTimeMillis()
    )
}
