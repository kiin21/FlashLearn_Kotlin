package com.kotlin.flashlearn.domain.usecase

import com.kotlin.flashlearn.domain.model.DailyWordArchiveItem
import com.kotlin.flashlearn.domain.repository.DailyWordRepository
import javax.inject.Inject

class GetDailyWordArchiveUseCase @Inject constructor(
    private val repo: DailyWordRepository
) {
    suspend operator fun invoke(
        userId: String,
        fromDate: String?,
        toDate: String?
    ): List<DailyWordArchiveItem> {
        return repo.getArchive(userId, fromDate, toDate)
    }
}
