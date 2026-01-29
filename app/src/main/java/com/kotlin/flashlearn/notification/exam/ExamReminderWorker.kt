package com.kotlin.flashlearn.notification.exam

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kotlin.flashlearn.notification.NotificationHelper
import com.kotlin.flashlearn.presentation.noti.ExamDatePrefs
import com.kotlin.flashlearn.utils.DateUtils
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ExamReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val hour = inputData.getInt(KEY_HOUR, 7)
        val minute = inputData.getInt(KEY_MINUTE, 0)
        val title = inputData.getString(KEY_TITLE) ?: "FlashLearn"

        val msg = buildMessage(applicationContext)

        NotificationHelper.show(applicationContext, title, msg)

        // schedule next run
        ExamReminderScheduler.schedule(applicationContext, hour, minute, title)

        return Result.success()
    }

    private fun buildMessage(context: Context): String {
        val examMillis = ExamDatePrefs.get(context)

        return if (examMillis != null) {
            val daysLeft = DateUtils.daysBetweenTodayAnd(examMillis)

            when {
                daysLeft <= 0 -> "Your VSTEP exam is today. Good luck!"
                else -> "Only $daysLeft days left until the VSTEP exam! Start studying now!"
            }
        } else {
            "Haven't set a test goal yet? Set one now to track your progress!"
        }
    }

    companion object {
        const val KEY_HOUR = "hour"
        const val KEY_MINUTE = "minute"
        const val KEY_TITLE = "title"
    }
}
