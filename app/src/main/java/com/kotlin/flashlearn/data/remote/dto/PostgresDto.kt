package com.kotlin.flashlearn.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Request body for Postgres SQL over HTTP.
 */
data class PostgresSqlRequest(
    @SerializedName("query")
    val query: String,
    @SerializedName("params")
    val params: List<Any> = emptyList()
)

/**
 * Response from Postgres SQL query.
 */
data class PostgresSqlResponse(
    @SerializedName("rows")
    val rows: List<List<Any?>>? = null,
    @SerializedName("fields")
    val fields: List<PostgresField>? = null,
    @SerializedName("error")
    val error: PostgresError? = null
)

data class PostgresField(
    @SerializedName("name")
    val name: String,
    @SerializedName("dataTypeID")
    val dataTypeId: Int? = null
)

data class PostgresError(
    @SerializedName("message")
    val message: String
)

/**
 * DTO for Topic from database.
 * Column order: id, name, description, icon_type, is_system_topic, is_public, created_by
 */
data class TopicDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val iconType: String? = null,
    val isSystemTopic: Boolean = false,
    val isPublic: Boolean = true,
    val createdBy: String? = null
) {
    companion object {
        /**
         * Parse a row from SQL result to TopicDto.
         * Column order: id, name, description, icon_type, is_system_topic, is_public, created_by
         */
        fun fromRow(row: List<Any?>): TopicDto {
            return TopicDto(
                id = row.getOrNull(0)?.toString() ?: "",
                name = row.getOrNull(1)?.toString() ?: "",
                description = row.getOrNull(2)?.toString(),
                iconType = row.getOrNull(3)?.toString(),
                isSystemTopic = (row.getOrNull(4) as? Boolean) ?: false,
                isPublic = (row.getOrNull(5) as? Boolean) ?: true,
                createdBy = row.getOrNull(6)?.toString()
            )
        }
    }
}

/**
 * DTO for Flashcard from database.
 * Column order: id, topic_id, word, pronunciation, part_of_speech, definition, example_sentence
 */
data class FlashcardDto(
    val id: String,
    val topicId: String,
    val word: String,
    val pronunciation: String? = null,
    val partOfSpeech: String? = null,
    val definition: String? = null,
    val exampleSentence: String? = null,
    val ipa: String? = null,
    val imageUrl: String? = null
) {
    companion object {
        /**
         * Parse a row from SQL result to FlashcardDto.
         * Column order: id, topic_id, word, pronunciation, part_of_speech, definition, example_sentence, ipa, image_url
         */
        fun fromRow(row: List<Any?>): FlashcardDto {
            return FlashcardDto(
                id = row.getOrNull(0)?.toString() ?: "",
                topicId = row.getOrNull(1)?.toString() ?: "",
                word = row.getOrNull(2)?.toString() ?: "",
                pronunciation = row.getOrNull(3)?.toString(),
                partOfSpeech = row.getOrNull(4)?.toString(),
                definition = row.getOrNull(5)?.toString(),
                exampleSentence = row.getOrNull(6)?.toString(),
                ipa = row.getOrNull(7)?.toString(),
                imageUrl = row.getOrNull(8)?.toString()
            )
        }
    }
    
    fun toDomain(): com.kotlin.flashlearn.domain.model.Flashcard {
        return com.kotlin.flashlearn.domain.model.Flashcard(
            id = id,
            topicId = topicId,
            word = word,
            pronunciation = pronunciation ?: "",
            partOfSpeech = partOfSpeech ?: "",
            definition = definition ?: "",
            exampleSentence = exampleSentence ?: "",
            ipa = ipa ?: "",
            imageUrl = imageUrl ?: ""
        )
    }
}
