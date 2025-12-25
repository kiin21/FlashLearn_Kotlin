package com.kotlin.flashlearn.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO for Datamuse autocomplete suggestion response.
 */
data class WordSuggestionDto(
    @SerializedName("word")
    val word: String,
    @SerializedName("score")
    val score: Int = 0
)

/**
 * DTO for Datamuse word with definition response.
 * The defs field contains definitions in format: "part_of_speech\tdefinition"
 */
data class WordWithDefinitionDto(
    @SerializedName("word")
    val word: String,
    @SerializedName("score")
    val score: Int = 0,
    @SerializedName("defs")
    val defs: List<String>? = null,
    @SerializedName("tags")
    val tags: List<String>? = null
) {
    /**
     * Parse definitions into structured format.
     * Datamuse returns defs like ["n\tthe definition", "v\tverb meaning"]
     */
    fun getParsedDefinitions(): List<ParsedDefinition> {
        return defs?.mapNotNull { def ->
            val parts = def.split("\t", limit = 2)
            if (parts.size == 2) {
                ParsedDefinition(
                    partOfSpeech = parsePartOfSpeech(parts[0]),
                    definition = parts[1]
                )
            } else null
        } ?: emptyList()
    }
    
    private fun parsePartOfSpeech(code: String): String {
        return when (code.lowercase()) {
            "n" -> "NOUN"
            "v" -> "VERB"
            "adj" -> "ADJECTIVE"
            "adv" -> "ADVERB"
            else -> code.uppercase()
        }
    }
}

data class ParsedDefinition(
    val partOfSpeech: String,
    val definition: String
)
