package com.kotlin.flashlearn.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface UnsplashApi {
    @GET("search/photos")
    suspend fun searchPhotos(
        @Query("query") query: String,
        @Query("per_page") perPage: Int = 1,
        @Header("Authorization") authorization: String
    ): UnsplashResponseDto
}

data class UnsplashResponseDto(
    @SerializedName("results") val results: List<UnsplashPhotoDto>
)

data class UnsplashPhotoDto(
    @SerializedName("urls") val urls: UnsplashUrlsDto
)

data class UnsplashUrlsDto(
    @SerializedName("small") val small: String,
    @SerializedName("regular") val regular: String
)
