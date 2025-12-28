package com.kotlin.flashlearn.domain.model

/**
 * Domain model representing a vocabulary topic.
 * 
 * Topic visibility:
 * - System topics (isSystemTopic=true): Visible to everyone
 * - User private topics (isPublic=false): Only visible to creator
 * - User shared topics (isPublic=true): Visible to everyone
 */
data class Topic(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val iconType: String = "book",
    val isSystemTopic: Boolean = false,
    val isPublic: Boolean = true,
    val createdBy: String? = null,
    val wordCount: Int = 0,
    val imageUrl: String? = null
) {
    /**
     * Returns true if this topic should be visible to the given user.
     */
    fun isVisibleTo(userId: String?): Boolean {
        return when {
            isSystemTopic -> true // System topics visible to all
            isPublic -> true // Public topics visible to all
            createdBy == userId -> true // Creator can see their own topics
            else -> false
        }
    }
    
    /**
     * Returns the visibility category for display purposes.
     */
    fun getCategory(): TopicCategory {
        return when {
            isSystemTopic -> TopicCategory.SYSTEM
            createdBy != null && !isPublic -> TopicCategory.PRIVATE
            createdBy != null && isPublic -> TopicCategory.SHARED
            else -> TopicCategory.SYSTEM
        }
    }
}

enum class TopicCategory {
    SYSTEM,   // Default app topics
    PRIVATE,  // User's private topics
    SHARED    // User's shared/community topics
}
