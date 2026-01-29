package com.kotlin.flashlearn.workers

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DailyWordScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val UNIQUE_DAILY_WORD = "DAILY_WORD_MIDNIGHT_0005"
    }

    fun scheduleDaily() {
        val zone = ZoneId.of("Asia/Ho_Chi_Minh")
        val now = ZonedDateTime.now(zone)

        val target = LocalTime.of(0, 5)
        var next = now.with(target)
        if (!next.isAfter(now)) next = next.plusDays(1)

        val delay = Duration.between(now, next)

        val req = PeriodicWorkRequestBuilder<DailyWordWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_DAILY_WORD,
            ExistingPeriodicWorkPolicy.UPDATE,
            req
        )
    }
}