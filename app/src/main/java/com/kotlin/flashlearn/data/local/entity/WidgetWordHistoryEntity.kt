package com.kotlin.flashlearn.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "widget_word_history",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["userId", "flashcardId"], unique = true),
        Index(value = ["userId", "isCorrect"])
    ]
)
data class WidgetWordHistoryEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val flashcardId: String,

    val firstShownDate: String,
    val lastShownDate: String,
    val shownCount: Int = 1,

    val isCorrect: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
