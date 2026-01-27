package com.kotlin.flashlearn.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kotlin.flashlearn.domain.model.Topic


/**
 * Room entity for caching topics locally.
 * Enables offline-first architecture for topic listing.
 */
@Entity(tableName = "topics")
data class TopicEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val iconType: String,
    val isSystemTopic: Boolean,
    val isPublic: Boolean,
    val createdBy: String?,
    val wordCount: Int,
    val imageUrl: String?,
    val lastUpdated: Long = System.currentTimeMillis(),
    // New fields for Community feature

    val upvoteCount: Int = 0,
    val downloadCount: Int = 0,
    val creatorName: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    // Word-level data
    val wordLevels: List<String> = emptyList()
) {
    fun toDomain(): Topic = Topic(
        id = id,
        name = name,
        description = description,
        iconType = iconType,
        isSystemTopic = isSystemTopic,
        isPublic = isPublic,
        createdBy = createdBy,
        wordCount = wordCount,
        imageUrl = imageUrl,

        upvoteCount = upvoteCount,
        downloadCount = downloadCount,
        creatorName = creatorName,
        createdAt = createdAt,
        wordLevels = wordLevels
    )

    companion object {
        fun fromDomain(topic: Topic): TopicEntity = TopicEntity(
            id = topic.id,
            name = topic.name,
            description = topic.description,
            iconType = topic.iconType,
            isSystemTopic = topic.isSystemTopic,
            isPublic = topic.isPublic,
            createdBy = topic.createdBy,
            wordCount = topic.wordCount,
            imageUrl = topic.imageUrl,

            upvoteCount = topic.upvoteCount,
            downloadCount = topic.downloadCount,
            creatorName = topic.creatorName,
            createdAt = topic.createdAt,
            wordLevels = topic.wordLevels
        )
    }
}
