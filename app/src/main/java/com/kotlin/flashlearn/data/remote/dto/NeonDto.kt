package com.kotlin.flashlearn.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Request body for Neon SQL over HTTP.
 */
data class NeonSqlRequest(
    @SerializedName("query")
    val query: String,
    @SerializedName("params")
    val params: List<Any> = emptyList()
)

/**
 * Response from Neon SQL query.
 */
data class NeonSqlResponse(
    @SerializedName("rows")
    val rows: List<List<Any?>>? = null,
    @SerializedName("fields")
    val fields: List<NeonField>? = null,
    @SerializedName("error")
    val error: NeonError? = null
)

data class NeonField(
    @SerializedName("name")
    val name: String,
    @SerializedName("dataTypeID")
    val dataTypeId: Int? = null
)

data class NeonError(
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
