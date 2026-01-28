package com.kotlin.flashlearn.data.repository

import com.kotlin.flashlearn.data.local.dao.DailyWordCandidate
import com.kotlin.flashlearn.data.local.dao.DailyWordDao
import com.kotlin.flashlearn.data.local.dao.FlashcardDao
import com.kotlin.flashlearn.data.local.entity.DailyWordHistoryEntity
import com.kotlin.flashlearn.domain.model.DailyWord
import com.kotlin.flashlearn.domain.model.DailyWordArchiveItem
import com.kotlin.flashlearn.domain.repository.DailyWordRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class DailyWordRepositoryImpl @Inject constructor(
    private val dailyWordDao: DailyWordDao,
    private val flashcardDao: FlashcardDao,
) : DailyWordRepository {

    override suspend fun getOrCreateTodayDailyWord(
        userId: String,
        dateKey: String
    ): DailyWord? {

        val all = flashcardDao.getDailyWordCandidates(userId)
        if (all.isEmpty()) return null

        val todayEntry = dailyWordDao.getTodayEntry(userId, dateKey)
        if (todayEntry != null) {
            val candidate = all.firstOrNull { it.id == todayEntry.wordId }
            if (candidate != null) return candidate.toDailyWord(dateKey)
        }

        val shownIds = dailyWordDao.getAllShownWordIds(userId).toHashSet()

        val available = all.filter { it.id !in shownIds }
        val pickFrom = if (available.isNotEmpty()) available else all

        val picked = pickFrom[Random.nextInt(pickFrom.size)]

        dailyWordDao.upsert(
            DailyWordHistoryEntity(
                userId = userId,
                dateKey = dateKey,
                wordId = picked.id
            )
        )

        return picked.toDailyWord(dateKey)
    }

    override suspend fun getArchive(
        userId: String,
        fromDate: String?,
        toDate: String?
    ): List<DailyWordArchiveItem> {

        val entries = dailyWordDao.getArchiveFiltered(userId, fromDate, toDate)
        if (entries.isEmpty()) return emptyList()

        val candidates = flashcardDao.getDailyWordCandidates(userId)
        val map = candidates.associateBy { it.id }

        return entries.mapNotNull { e ->
            val c = map[e.wordId] ?: return@mapNotNull null
            DailyWordArchiveItem(
                dateKey = e.dateKey,
                wordId = c.id,
                word = c.word,
                meaning = c.meaning,
                ipa = c.ipa
            )
        }
    }

    private fun DailyWordCandidate.toDailyWord(dateKey: String): DailyWord {
        return DailyWord(
            wordId = id,
            word = word,
            meaning = meaning,
            ipa = ipa,
            level = level,
            dateKey = dateKey
        )
    }
}
