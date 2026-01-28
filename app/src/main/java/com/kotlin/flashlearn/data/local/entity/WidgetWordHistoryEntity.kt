package com.kotlin.flashlearn.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "daily_word_history",
    indices = [
        Index(value = ["userId", "dateKey"], unique = true),
        Index(value = ["userId", "wordId"], unique = true)
    ]
)
data class DailyWordHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val dateKey: String,
    val wordId: String
)
