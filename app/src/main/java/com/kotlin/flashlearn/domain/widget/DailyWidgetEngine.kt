package com.kotlin.flashlearn.domain.widget

import com.kotlin.flashlearn.data.local.dao.DailyWidgetSessionDao
import com.kotlin.flashlearn.data.local.dao.FlashcardDao
import com.kotlin.flashlearn.data.local.dao.UserProgressDao
import com.kotlin.flashlearn.data.local.dao.UserStreakDao
import com.kotlin.flashlearn.data.local.dao.WidgetWordHistoryDao
import com.kotlin.flashlearn.data.local.entity.DailyWidgetSessionEntity
import com.kotlin.flashlearn.data.local.entity.UserStreakEntity
import com.kotlin.flashlearn.data.local.entity.WidgetWordHistoryEntity
import com.kotlin.flashlearn.domain.model.Flashcard
import com.kotlin.flashlearn.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DailyWidgetEngine @Inject constructor(
    private val authRepository: AuthRepository,
    private val flashcardDao: FlashcardDao,
    private val sessionDao: DailyWidgetSessionDao,
    private val historyDao: WidgetWordHistoryDao,
    private val streakDao: UserStreakDao,
    private val userProgressDao: UserProgressDao
) {

    /**
     * Get today's widget state (assign a word if none assigned today).
     */
    suspend fun getState(): WidgetState {
        val userId = authRepository.getSignedInUser()?.userId ?: return WidgetState.SignedOut
        val today = WidgetDate.today()

        val session = getOrCreateSession(userId, today)

        if (session.isCompleted) {
            val streak = streakDao.get(userId) ?: UserStreakEntity(userId = userId)
            return WidgetState.DoneToday(today, streak.current, streak.best)
        }

        val currentId = session.currentFlashcardId
        if (!currentId.isNullOrBlank()) {
            val card = flashcardDao.getFlashcardById(currentId)?.toDomain()
                ?: return assignNewWord(userId, today, session) // nếu card bị xoá -> assign lại

            return if (session.isRevealed) WidgetState.CardRevealed(today, card)
            else WidgetState.CardHidden(today, card)
        }

        return assignNewWord(userId, today, session)
    }

    /**
     * Reveal current card (only UI state change).
     */
    suspend fun reveal(): WidgetState {
        val userId = authRepository.getSignedInUser()?.userId ?: return WidgetState.SignedOut
        val today = WidgetDate.today()

        val session = getOrCreateSession(userId, today)

        if (session.isCompleted) {
            val streak = streakDao.get(userId) ?: UserStreakEntity(userId = userId)
            return WidgetState.DoneToday(today, streak.current, streak.best)
        }

        val currentId = session.currentFlashcardId
        if (currentId.isNullOrBlank()) return getState()

        val updated = session.copy(
            isRevealed = true,
            updatedAt = System.currentTimeMillis()
        )
        sessionDao.upsert(updated)

        val card = flashcardDao.getFlashcardById(currentId)?.toDomain() ?: return getState()
        return WidgetState.CardRevealed(today, card)
    }

    /**
     * Missed
     */
    suspend fun missed(): WidgetState {
        val userId = authRepository.getSignedInUser()?.userId ?: return WidgetState.SignedOut
        val today = WidgetDate.today()

        val session = getOrCreateSession(userId, today)

        if (session.isCompleted) {
            val streak = streakDao.get(userId) ?: UserStreakEntity(userId = userId)
            return WidgetState.DoneToday(today, streak.current, streak.best)
        }

        val attempted = WidgetJson.decodeStringList(session.attemptedIdsJson)

        // add current to attempted to avoid repeating today
        session.currentFlashcardId?.let { cid ->
            if (cid.isNotBlank() && !attempted.contains(cid)) attempted.add(cid)
        }

        val next = pickWord(userId, attempted)
        if (next == null) {
            val updated = session.copy(
                attemptedIdsJson = WidgetJson.encodeStringList(attempted),
                currentFlashcardId = null,
                isRevealed = false,
                updatedAt = System.currentTimeMillis()
            )
            sessionDao.upsert(updated)
            return WidgetState.Exhausted(today, "You have seen all the suitable words for widgets.")
        }

        val updated = session.copy(
            attemptedIdsJson = WidgetJson.encodeStringList(attempted),
            currentFlashcardId = next.id,
            isRevealed = false,
            updatedAt = System.currentTimeMillis()
        )
        sessionDao.upsert(updated)

        return WidgetState.CardHidden(today, next)
    }

    /**
     * GotIt:
     * - Write widget_word_history.isCorrect = true
     * - Mark completed today
     * - Apply streak
     */
    suspend fun gotIt(): WidgetState {
        val userId = authRepository.getSignedInUser()?.userId ?: return WidgetState.SignedOut
        val today = WidgetDate.today()

        val session = getOrCreateSession(userId, today)

        if (session.isCompleted) {
            val streak = streakDao.get(userId) ?: UserStreakEntity(userId = userId)
            return WidgetState.DoneToday(today, streak.current, streak.best)
        }

        session.currentFlashcardId?.takeIf { it.isNotBlank() }?.let { cid ->
            upsertHistoryCorrect(userId, cid, today)
        }

        val doneSession = session.copy(
            isCompleted = true,
            isRevealed = true,
            completedAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        sessionDao.upsert(doneSession)

        val updatedStreak = applyStreak(userId, today)
        return WidgetState.DoneToday(today, updatedStreak.current, updatedStreak.best)
    }

    private suspend fun getOrCreateSession(userId: String, date: String): DailyWidgetSessionEntity {
        val existing = sessionDao.getByUserAndDate(userId, date)
        if (existing != null) return existing

        val e = DailyWidgetSessionEntity(
            id = "${userId}_${date}",
            userId = userId,
            date = date,
            currentFlashcardId = null,
            attemptedIdsJson = "[]",
            isRevealed = false,
            isCompleted = false,
            completedAt = null
        )
        sessionDao.upsert(e)
        return e
    }

    private suspend fun assignNewWord(
        userId: String,
        today: String,
        session: DailyWidgetSessionEntity
    ): WidgetState {
        val attempted = WidgetJson.decodeStringList(session.attemptedIdsJson)
        val picked = pickWord(userId, attempted)

        if (picked == null) {
            return WidgetState.Exhausted(today, "You have seen all the suitable words for widgets.")
        }

        val updated = session.copy(
            currentFlashcardId = picked.id,
            isRevealed = false,
            updatedAt = System.currentTimeMillis()
        )
        sessionDao.upsert(updated)

        return WidgetState.CardHidden(today, picked)
    }

    /**
     * DB-level exclusion is in FlashcardDao.pickWidgetWord():
     * - ONLY MASTERED (user_progress.status = MASTERED)
     * - EXCLUDE correct widget history (widget_word_history.isCorrect = 1)
     * - EXCLUDE attemptedIdsToday
     */
    private suspend fun pickWord(userId: String, attemptedIds: List<String>): Flashcard? {
        val exclude = attemptedIds.distinct()
        val entity = flashcardDao.pickWidgetWord(
            userId = userId,
            excludeIds = exclude,
            excludeSize = exclude.size
        )
        return entity?.toDomain()
    }

    private suspend fun upsertHistoryCorrect(userId: String, flashcardId: String, date: String) {
        val id = "${userId}_${flashcardId}"

        historyDao.upsert(
            WidgetWordHistoryEntity(
                id = id,
                userId = userId,
                flashcardId = flashcardId,
                firstShownDate = date,
                lastShownDate = date,
                shownCount = 1,
                isCorrect = true,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    private suspend fun applyStreak(userId: String, today: String): UserStreakEntity {
        val current = streakDao.get(userId) ?: UserStreakEntity(userId = userId)

        // already counted today
        if (current.lastActiveDate == today) return current

        val newCurrent = if (current.lastActiveDate == WidgetDate.yesterday(today)) {
            current.current + 1
        } else {
            1
        }
        val newBest = maxOf(current.best, newCurrent)

        val updated = current.copy(
            current = newCurrent,
            best = newBest,
            lastActiveDate = today,
            updatedAt = System.currentTimeMillis()
        )
        streakDao.upsert(updated)
        return updated
    }

    private fun effectiveCurrent(streak: UserStreakEntity, today: String): Int {
        val last = streak.lastActiveDate ?: return 0
        return when (last) {
            today -> streak.current
            WidgetDate.yesterday(today) -> streak.current
            else -> 0
        }
    }
}
