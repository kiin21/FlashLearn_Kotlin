package com.kotlin.flashlearn.data.remote.dto

import com.google.gson.annotations.SerializedName

data class DictionaryEntryDto(
    @SerializedName("word")
    val word: String,
    @SerializedName("phonetic")
    val phonetic: String? = null,
    @SerializedName("phonetics")
    val phonetics: List<PhoneticDto>? = null,
    @SerializedName("meanings")
    val meanings: List<MeaningDto>? = null
)

data class PhoneticDto(
    @SerializedName("text")
    val text: String? = null,
    @SerializedName("audio")
    val audio: String? = null
)

data class MeaningDto(
    @SerializedName("partOfSpeech")
    val partOfSpeech: String,
    @SerializedName("definitions")
    val definitions: List<DictionaryDefinitionDto>
)

data class DictionaryDefinitionDto(
    @SerializedName("definition")
    val definition: String,
    @SerializedName("example")
    val example: String? = null
)
