package com.kotlin.flashlearn.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "daily_widget_session",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["userId", "date"], unique = true)
    ]
)
data class DailyWidgetSessionEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val date: String,
    val currentFlashcardId: String?,
    val attemptedIdsJson: String = "[]",
    val isRevealed: Boolean = false,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val updatedAt: Long = System.currentTimeMillis()
)