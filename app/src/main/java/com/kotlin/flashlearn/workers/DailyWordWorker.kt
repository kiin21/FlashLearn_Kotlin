package com.kotlin.flashlearn.workers

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kotlin.flashlearn.data.local.prefs.LastUserPrefs
import com.kotlin.flashlearn.domain.usecase.GetTodayDailyWordUseCase
import com.kotlin.flashlearn.utils.DateKey
import com.kotlin.flashlearn.widget.DailyWordWidget
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class DailyWordWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val lastUserPrefs: LastUserPrefs,
    private val getTodayDailyWord: GetTodayDailyWordUseCase,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val userId = lastUserPrefs.getLastUserId()
            ?: return Result.success()

        val today = DateKey.today()
        getTodayDailyWord(userId, today)

        DailyWordWidget().updateAll(applicationContext)

        return Result.success()
    }
}
