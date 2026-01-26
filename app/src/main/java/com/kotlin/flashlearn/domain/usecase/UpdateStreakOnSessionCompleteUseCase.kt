package com.kotlin.flashlearn.domain.usecase

import com.kotlin.flashlearn.data.local.dao.UserStreakDao
import com.kotlin.flashlearn.data.local.entity.UserStreakEntity
import com.kotlin.flashlearn.domain.model.StreakResult
import com.kotlin.flashlearn.utils.LocalDateKey
import javax.inject.Inject

class UpdateStreakOnSessionCompleteUseCase @Inject constructor(
    private val userStreakDao: UserStreakDao
) {
    suspend operator fun invoke(userId: String): StreakResult {
        val today = LocalDateKey.today()
        val yesterday = LocalDateKey.yesterday()

        val streak = userStreakDao.getByUserId(userId)

        if (streak == null || streak.lastActiveDate == null) {
            userStreakDao.insert(
                UserStreakEntity(
                    userId = userId,
                    currentStreak = 1,
                    lastActiveDate = today
                )
            )
            return StreakResult(didIncrement = true, current = 1)
        }

        if (streak.lastActiveDate == today) {
            return StreakResult(didIncrement = false, current = streak.currentStreak)
        }

        val newCurrent =
            if (streak.lastActiveDate == yesterday) streak.currentStreak + 1
            else 1

        userStreakDao.update(
            userId = userId,
            currentStreak = newCurrent,
            lastActiveDate = today
        )

        return StreakResult(didIncrement = true, current = newCurrent)
    }
}
