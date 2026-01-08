package com.kotlin.flashlearn.notification.daily

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object DailyReminderScheduler {

    private const val UNIQUE_WORK_NAME = "daily_reminder_work"

    /**
     * Schedule a local notification at [hour]:[minute] every day.
     * It uses OneTimeWork + self-reschedule to be closer to exact time than PeriodicWork.
     */
    fun schedule(
        context: Context,
        hour: Int,
        minute: Int,
        title: String = "FlashLearn",
        body: String = "Đến giờ học rồi nè 👀",
    ) {
        val appContext = context.applicationContext

        val delayMs = computeDelayMs(hour, minute)

        val input = workDataOf(
            DailyReminderWorker.KEY_HOUR to hour,
            DailyReminderWorker.KEY_MINUTE to minute,
            DailyReminderWorker.KEY_TITLE to title,
            DailyReminderWorker.KEY_BODY to body
        )

        val request = OneTimeWorkRequestBuilder<DailyReminderWorker>()
            .setInitialDelay(delayMs, java.util.concurrent.TimeUnit.MILLISECONDS)
            .setInputData(input)
            .addTag(UNIQUE_WORK_NAME)
            .build()

        WorkManager.getInstance(appContext)
            .enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.REPLACE, // replace to update time/message
                request
            )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context.applicationContext)
            .cancelUniqueWork(UNIQUE_WORK_NAME)
    }

    fun isScheduled(context: Context, onResult: (Boolean) -> Unit) {
        WorkManager.getInstance(context.applicationContext)
            .getWorkInfosForUniqueWorkLiveData(UNIQUE_WORK_NAME)
            .observeForever { list ->
                val active = list?.any { !it.state.isFinished } == true
                onResult(active)
            }
    }

    private fun computeDelayMs(hour: Int, minute: Int): Long {
        val zone = ZoneId.systemDefault()
        val now = LocalDateTime.now(zone)

        val todayTarget = LocalDateTime.of(LocalDate.now(zone), LocalTime.of(hour, minute))
        val next = if (now.isBefore(todayTarget)) todayTarget else todayTarget.plusDays(1)

        val d = Duration.between(now, next)
        val ms = d.toMillis()

        // just in case
        return if (ms < 0) 0 else ms
    }
}
