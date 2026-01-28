package com.kotlin.flashlearn.data.sync

interface SyncRepository {
    suspend fun syncAll(userId: String)
}
