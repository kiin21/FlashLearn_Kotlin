package com.kotlin.flashlearn.notification.daily

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.kotlin.flashlearn.notification.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class DailyReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val hour = inputData.getInt(KEY_HOUR, 20)
        val minute = inputData.getInt(KEY_MINUTE, 0)
        val title = inputData.getString(KEY_TITLE) ?: "FlashLearn"
        val body = inputData.getString(KEY_BODY) ?: "Đến giờ học rồi nè 👀"

        // Show local notification
        NotificationHelper.show(applicationContext, title, body)

        // Schedule next run
        DailyReminderScheduler.schedule(applicationContext, hour, minute, title, body)

        return Result.success(
            workDataOf(
                "scheduledNext" to true
            )
        )
    }

    companion object {
        const val KEY_HOUR = "hour"
        const val KEY_MINUTE = "minute"
        const val KEY_TITLE = "title"
        const val KEY_BODY = "body"
    }
}
