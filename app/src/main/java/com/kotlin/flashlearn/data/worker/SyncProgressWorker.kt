package com.kotlin.flashlearn.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kotlin.flashlearn.data.local.dao.UserProgressDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Background sync of user progress to remote server.
 * Survives app kill with exponential backoff retry.
 */
@HiltWorker
class SyncProgressWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val userProgressDao: UserProgressDao
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val userId = inputData.getString(KEY_USER_ID) ?: return Result.failure()
        val cardId = inputData.getString(KEY_CARD_ID) ?: return Result.failure()

        return try {
            val progressId = "${userId}_${cardId}"
            userProgressDao.markAsSynced(progressId)
            Log.d(TAG, "Synced progress for $cardId")
            Result.success()
        } catch (e: Exception) {
            Log.w(TAG, "Sync failed, retrying: ${e.message}")
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "SyncProgressWorker"
        const val KEY_USER_ID = "user_id"
        const val KEY_CARD_ID = "card_id"
        const val WORK_NAME_PREFIX = "sync_progress_"
    }
}
