package com.kotlin.flashlearn.data.remote

import com.kotlin.flashlearn.data.remote.dto.DictionaryEntryDto
import retrofit2.http.GET
import retrofit2.http.Path

interface FreeDictionaryApi {
    @GET("api/v2/entries/en/{word}")
    suspend fun getWordDetails(@Path("word") word: String): List<DictionaryEntryDto>
}
