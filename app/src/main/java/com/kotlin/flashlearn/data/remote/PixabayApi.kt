package com.kotlin.flashlearn.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface PixabayApi {
    @GET("api/")
    suspend fun searchImages(
        @Query("key") apiKey: String,
        @Query("q") query: String,
        @Query("image_type") imageType: String = "photo",
        @Query("per_page") perPage: Int = 3
    ): PixabayResponse
}

data class PixabayResponse(
    val hits: List<PixabayImage>
)

data class PixabayImage(
    @SerializedName("webformatURL")
    val webformatUrl: String,
    @SerializedName("largeImageURL")
    val largeImageUrl: String,
    val tags: String
)
