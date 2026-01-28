package com.kotlin.flashlearn.domain.usecase

import com.kotlin.flashlearn.domain.model.DailyWord
import com.kotlin.flashlearn.domain.repository.DailyWordRepository
import javax.inject.Inject

class GetTodayDailyWordUseCase @Inject constructor(
    private val repo: DailyWordRepository
) {
    suspend operator fun invoke(userId: String, dateKey: String): DailyWord? {
        return repo.getOrCreateTodayDailyWord(userId, dateKey)
    }
}
