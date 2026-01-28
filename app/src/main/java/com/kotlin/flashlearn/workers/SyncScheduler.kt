package com.kotlin.flashlearn.workers

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val UNIQUE_ONE_TIME = "SYNC_FIREBASE_TO_ROOM_ONCE"
        private const val UNIQUE_PERIODIC = "SYNC_FIREBASE_TO_ROOM_DAILY"
    }

    private val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun enqueueOneTimeSync(userId: String) {
        val data = Data.Builder()
            .putString(SyncFirebaseToRoomWorker.KEY_USER_ID, userId)
            .build()

        val req = OneTimeWorkRequestBuilder<SyncFirebaseToRoomWorker>()
            .setConstraints(constraints)
            .setInputData(data)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(UNIQUE_ONE_TIME, ExistingWorkPolicy.REPLACE, req)
    }

    fun scheduleDailySync(userId: String) {
        val data = Data.Builder()
            .putString(SyncFirebaseToRoomWorker.KEY_USER_ID, userId)
            .build()

        val req = PeriodicWorkRequestBuilder<SyncFirebaseToRoomWorker>(1, TimeUnit.DAYS)
            .setConstraints(constraints)
            .setInputData(data)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(UNIQUE_PERIODIC, ExistingPeriodicWorkPolicy.UPDATE, req)
    }
}
