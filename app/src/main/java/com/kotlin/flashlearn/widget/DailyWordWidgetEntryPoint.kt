package com.kotlin.flashlearn.widget

import com.kotlin.flashlearn.data.local.prefs.LastUserPrefs
import com.kotlin.flashlearn.domain.usecase.GetTodayDailyWordUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DailyWordWidgetEntryPoint {
    fun lastUserPrefs(): LastUserPrefs
    fun getTodayDailyWordUseCase(): GetTodayDailyWordUseCase
}
