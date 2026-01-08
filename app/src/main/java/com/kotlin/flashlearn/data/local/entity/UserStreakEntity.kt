package com.kotlin.flashlearn.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_streak")
data class UserStreakEntity(
    @PrimaryKey
    val userId: String,
    val current: Int = 0,
    val best: Int = 0,
    val lastActiveDate: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)