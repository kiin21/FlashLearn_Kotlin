package com.kotlin.flashlearn.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kotlin.flashlearn.domain.model.Flashcard

/**
 * Room entity for caching flashcards locally.
 * Mirrors the Flashcard domain model with Room annotations.
 */
@Entity(
    tableName = "flashcards",
    foreignKeys = [
        ForeignKey(
            entity = TopicEntity::class,
            parentColumns = ["id"],
            childColumns = ["topicId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["topicId"])]
)
data class FlashcardEntity(
    @PrimaryKey
    val id: String,
    val topicId: String,
    val word: String,
    val pronunciation: String,
    val partOfSpeech: String,
    val definition: String,
    val exampleSentence: String,
    val ipa: String,
    val imageUrl: String,
    val pronunciationUrl: String = "",
    val synonyms: List<String> = emptyList(),
    val createdAt: Long,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    fun toDomain(): Flashcard = Flashcard(
        id = id,
        topicId = topicId,
        word = word,
        pronunciation = pronunciation,
        partOfSpeech = partOfSpeech,
        definition = definition,
        exampleSentence = exampleSentence,
        ipa = ipa,
        imageUrl = imageUrl,
        synonyms = synonyms,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(flashcard: Flashcard): FlashcardEntity = FlashcardEntity(
            id = flashcard.id,
            topicId = flashcard.topicId,
            word = flashcard.word,
            pronunciation = flashcard.pronunciation,
            partOfSpeech = flashcard.partOfSpeech,
            definition = flashcard.definition,
            exampleSentence = flashcard.exampleSentence,
            ipa = flashcard.ipa,
            imageUrl = flashcard.imageUrl,
            synonyms = flashcard.synonyms,
            createdAt = flashcard.createdAt
        )
    }
}
