package com.kotlin.flashlearn.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kotlin.flashlearn.data.sync.SyncRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

class SyncFirebaseToRoomWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val syncRepository: SyncRepository,
) : CoroutineWorker(appContext, params) {

    companion object {
        const val KEY_USER_ID = "KEY_USER_ID"
    }

    override suspend fun doWork(): Result {
        val userId = inputData.getString(KEY_USER_ID).orEmpty()
        if (userId.isBlank()) {
            return Result.success()
        }

        return runCatching {
            syncRepository.syncAll(userId)
            Result.success()
        }.getOrElse {
            Result.retry()
        }
    }
}
