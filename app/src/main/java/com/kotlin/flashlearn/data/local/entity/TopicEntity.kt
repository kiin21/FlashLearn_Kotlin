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
    val lastUpdated: Long = System.currentTimeMillis()
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
        imageUrl = imageUrl
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
            imageUrl = topic.imageUrl
        )
    }
}
