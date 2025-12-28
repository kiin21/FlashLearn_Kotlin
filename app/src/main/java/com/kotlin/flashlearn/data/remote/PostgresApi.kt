package com.kotlin.flashlearn.data.remote

import com.kotlin.flashlearn.data.remote.dto.PostgresSqlRequest
import com.kotlin.flashlearn.data.remote.dto.PostgresSqlResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Postgres SQL over HTTP API.
 * Uses database credentials for authentication.
 * Base URL: https://ep-lively-frost-a1zoz3tj.ap-southeast-1.aws.neon.tech/
 */
interface PostgresApi {
    
    /**
     * Execute SQL query.
     * Neon-Array-Mode: true returns rows as arrays instead of objects (saves bandwidth)
     */
    @POST("sql")
    suspend fun executeQuery(
        @Header("Neon-Connection-String") connectionString: String,
        @Header("Content-Type") contentType: String = "application/json",
        @Header("Neon-Array-Mode") arrayMode: String = "true",
        @Body request: PostgresSqlRequest
    ): PostgresSqlResponse
}
