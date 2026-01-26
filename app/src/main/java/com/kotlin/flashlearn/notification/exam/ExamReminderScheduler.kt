package com.kotlin.flashlearn.notification.exam

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

object ExamReminderScheduler {

    private const val UNIQUE_WORK_NAME = "exam_reminder_work"

    /**
     * Schedule exam reminder at [hour]:[minute] every day.
     * Uses OneTimeWork + self-reschedule like DailyReminderScheduler.
     */
    fun schedule(
        context: Context,
        hour: Int,
        minute: Int,
        title: String = "FlashLearn",
    ) {
        val appContext = context.applicationContext
        val delayMs = computeDelayMs(hour, minute)

        val input = workDataOf(
            ExamReminderWorker.KEY_HOUR to hour,
            ExamReminderWorker.KEY_MINUTE to minute,
            ExamReminderWorker.KEY_TITLE to title
        )

        val req = OneTimeWorkRequestBuilder<ExamReminderWorker>()
            .setInitialDelay(delayMs, java.util.concurrent.TimeUnit.MILLISECONDS)
            .setInputData(input)
            .addTag(UNIQUE_WORK_NAME)
            .build()

        WorkManager.getInstance(appContext)
            .enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                req
            )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context.applicationContext)
            .cancelUniqueWork(UNIQUE_WORK_NAME)
    }

    private fun computeDelayMs(hour: Int, minute: Int): Long {
        val zone = ZoneId.systemDefault()
        val now = LocalDateTime.now(zone)
        val todayTarget = LocalDateTime.of(LocalDate.now(zone), LocalTime.of(hour, minute))
        val next = if (now.isBefore(todayTarget)) todayTarget else todayTarget.plusDays(1)
        val ms = Duration.between(now, next).toMillis()
        return if (ms < 0) 0 else ms
    }
}
