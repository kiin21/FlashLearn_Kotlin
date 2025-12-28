package com.kotlin.flashlearn.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path

interface FreeDictionaryApi {
    @GET("api/v2/entries/en/{word}")
    suspend fun getWordDetails(@Path("word") word: String): List<DictionaryEntryDto>
}

data class DictionaryEntryDto(
    @SerializedName("phonetic") val phonetic: String?,
    @SerializedName("phonetics") val phonetics: List<PhoneticDto>?
)

data class PhoneticDto(
    @SerializedName("text") val text: String?,
    @SerializedName("audio") val audio: String?
)
