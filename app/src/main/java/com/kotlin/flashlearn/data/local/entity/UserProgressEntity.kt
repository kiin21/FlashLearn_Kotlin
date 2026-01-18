package com.kotlin.flashlearn.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tracks user progress on flashcards.
 * Persists mastered/review status to survive app restarts.
 */
enum class ProgressStatus {
    LEARNING,
    REVIEW,
    MASTERED
}

@Entity(
    tableName = "user_progress",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["flashcardId"]),
        Index(value = ["userId", "flashcardId"], unique = true)
    ]
)
data class UserProgressEntity(
    @PrimaryKey
    val id: String, // Format: "{userId}_{flashcardId}"
    val userId: String,
    val flashcardId: String,
    val status: ProgressStatus,
    val proficiencyScore: Int = 0, // 0-2: New, 3-5: Familiar, 6+: Mastered
    val updatedAt: Long = System.currentTimeMillis(),
    val syncedToRemote: Boolean = false // Tracks if synced to NeonDB
)
