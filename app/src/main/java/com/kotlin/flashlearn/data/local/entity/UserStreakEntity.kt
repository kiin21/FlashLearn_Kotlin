package com.kotlin.flashlearn.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_streak")
data class UserStreakEntity(
    @PrimaryKey val userId: String,
    val currentStreak: Int = 0,
    val lastActiveDate: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)