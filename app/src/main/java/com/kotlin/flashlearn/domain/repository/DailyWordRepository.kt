package com.kotlin.flashlearn.domain.repository

import com.kotlin.flashlearn.domain.model.DailyWord
import com.kotlin.flashlearn.domain.model.DailyWordArchiveItem

interface DailyWordRepository {
    suspend fun getOrCreateTodayDailyWord(userId: String, dateKey: String): DailyWord?
    suspend fun getArchive(
        userId: String,
        fromDate: String?,
        toDate: String?
    ): List<DailyWordArchiveItem>
}
