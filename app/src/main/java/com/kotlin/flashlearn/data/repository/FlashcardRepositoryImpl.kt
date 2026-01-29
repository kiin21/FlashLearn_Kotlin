package com.kotlin.flashlearn.data.repository

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.kotlin.flashlearn.BuildConfig
import com.kotlin.flashlearn.data.local.dao.FlashcardDao
import com.kotlin.flashlearn.data.local.dao.TopicDao
import com.kotlin.flashlearn.data.local.dao.UserProgressDao
import com.kotlin.flashlearn.data.local.entity.FlashcardEntity
import com.kotlin.flashlearn.data.local.entity.ProgressStatus
import com.kotlin.flashlearn.data.local.entity.UserProgressEntity
import com.kotlin.flashlearn.data.remote.DatamuseApi
import com.kotlin.flashlearn.data.remote.FreeDictionaryApi
import com.kotlin.flashlearn.data.remote.PixabayApi
import com.kotlin.flashlearn.domain.model.Flashcard
import com.kotlin.flashlearn.domain.repository.FlashcardRepository
import com.kotlin.flashlearn.domain.repository.TopicRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.tasks.await
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

@Singleton
class FlashcardRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val datamuseApi: DatamuseApi,
    private val topicRepository: TopicRepository,
    private val freeDictionaryApi: FreeDictionaryApi,
    private val pixabayApi: PixabayApi,
    private val userProgressDao: UserProgressDao,
    private val cloudinaryService: com.kotlin.flashlearn.data.remote.CloudinaryService,
    private val flashcardDao: FlashcardDao,
    private val topicDao: TopicDao
) : FlashcardRepository {

    companion object {
        private const val TAG = "FlashcardRepo"
    }

    private val flashcardCache = ConcurrentHashMap<String, List<Flashcard>>()
    private val enrichmentSemaphore = Semaphore(5)

    private val topicsCollection = firestore.collection("topics")

    override suspend fun updateFlashcard(topicId: String, flashcard: Flashcard): Result<Unit> {
        return runCatching {
            saveFlashcardToFirestore(flashcard, topicId)

            runCatching {
                flashcardDao.insertFlashcard(FlashcardEntity.fromDomain(flashcard))
            }.onFailure {
                Log.w(TAG, "Room cache update failed: ${it.message}")
            }

            // Update cache safely
            val currentCache = flashcardCache[topicId]
            if (currentCache != null) {
                val updatedCache = currentCache.map {
                    if (it.id == flashcard.id) flashcard else it
                }
                flashcardCache[topicId] = updatedCache
            }

            Unit
        }.onFailure {
            if (it is CancellationException) throw it
        }
    }

    override suspend fun getFlashcardsByTopicId(topicId: String): Result<List<Flashcard>> {
        return runCatching {
            var flashcards = getFlashcardsFromFirestore(topicId)

            if (flashcards.isEmpty()) {
                val topic = topicRepository.getTopicById(topicId).getOrNull()
                if (topic?.isSystemTopic == true) {
                    flashcards = getFlashcardsFromDatamuse(topicId)
                    if (flashcards.isNotEmpty()) {
                        saveFlashcardsForTopic(topicId, flashcards)
                    }
                }
            }

            val uniqueFlashcards = flashcards.distinctBy { it.word.lowercase() }

            if (uniqueFlashcards.isNotEmpty()) {
                runCatching {
                    flashcardDao.insertFlashcards(uniqueFlashcards.map(FlashcardEntity::fromDomain))
                }.onFailure {
                    Log.w(TAG, "Room cache insert flashcards failed: ${it.message}")
                }
                flashcardCache[topicId] = uniqueFlashcards
            }

            uniqueFlashcards
        }.onFailure {
            if (it is CancellationException) throw it
        }
    }

    private suspend fun getFlashcardsFromFirestore(topicId: String): List<Flashcard> {
        val flashcardsRef = topicsCollection.document(topicId).collection("flashcards")

        val snapshot = flashcardsRef.orderBy("word").get(Source.DEFAULT).await()

        return snapshot.documents.mapNotNull { it.toFlashcard() }
    }

    private suspend fun getFlashcardsFromDatamuse(topicId: String): List<Flashcard> {
        val topic = topicRepository.getTopicById(topicId).getOrNull() ?: return emptyList()

        val keyword = extractKeyword(topic.name)
        val response = datamuseApi.getWordsByMeaning(keyword)

        return response.map { dto ->
            val firstDef = dto.getParsedDefinitions().firstOrNull()
            Flashcard(
                id = UUID.randomUUID().toString(),
                topicId = topicId,
                word = dto.word.replaceFirstChar { it.uppercase() },
                pronunciation = "",
                partOfSpeech = firstDef?.partOfSpeech?.uppercase() ?: "",
                definition = firstDef?.definition ?: "",
                exampleSentence = "",
                synonyms = emptyList()
            )
        }
    }

    override suspend fun saveFlashcardsForTopic(
        topicId: String,
        flashcards: List<Flashcard>
    ): Result<Unit> {
        return runCatching {
            val enrichedFlashcards = flashcards.map { enrichFlashcardData(it) }

            enrichedFlashcards.forEach { flashcard ->
                saveFlashcardToFirestore(flashcard, topicId)
            }

            flashcardCache[topicId] = enrichedFlashcards
        }.onFailure {
            if (it is CancellationException) throw it
        }
    }

    override suspend fun getFlashcardById(cardId: String): Result<Flashcard?> {
        return runCatching {
            flashcardCache.values.flatten().find { it.id == cardId }
        }.onFailure {
            if (it is CancellationException) throw it
        }
    }

    override suspend fun markFlashcardAsMastered(
        flashcardId: String,
        userId: String
    ): Result<Unit> {
        return saveProgress(flashcardId, userId, ProgressStatus.MASTERED)
    }

    override suspend fun markFlashcardForReview(flashcardId: String, userId: String): Result<Unit> {
        return saveProgress(flashcardId, userId, ProgressStatus.REVIEW)
    }

    private suspend fun saveProgress(
        flashcardId: String,
        userId: String,
        status: ProgressStatus
    ): Result<Unit> {
        return runCatching {
            // Set proficiencyScore based on status to ensure consistency
            // MASTERED requires score >= 6, REVIEW requires score >= 3
            val proficiencyScore = when (status) {
                ProgressStatus.MASTERED -> 6
                ProgressStatus.REVIEW -> 3
                ProgressStatus.LEARNING -> 0
            }
            
            val progress = UserProgressEntity(
                id = "${userId}_${flashcardId}",
                userId = userId,
                flashcardId = flashcardId,
                status = status,
                proficiencyScore = proficiencyScore,
                updatedAt = System.currentTimeMillis(),
                syncedToRemote = false
            )
            userProgressDao.upsert(progress)
        }.onFailure { if (it is CancellationException) throw it }
    }

    override suspend fun deleteFlashcards(flashcardIds: List<String>): Result<Unit> {
        return runCatching {
            if (flashcardIds.isEmpty()) return Result.success(Unit)

            val topicId = findTopicIdForFlashcards(flashcardIds)

            if (topicId != null) {
                val flashcardsRef = topicsCollection.document(topicId).collection("flashcards")
                flashcardIds.forEach { id ->
                    flashcardsRef.document(id).delete().await()
                }
            }
            flashcardDao.deleteFlashcards(flashcardIds)

            flashcardCache.clear()
            Unit
        }.onFailure {
            if (it is CancellationException) throw it
        }
    }

    fun clearCache(topicId: String? = null) {
        if (topicId != null) {
            flashcardCache.remove(topicId)
        } else {
            flashcardCache.clear()
        }
    }

    override suspend fun enrichFlashcard(card: Flashcard, force: Boolean): Flashcard {
        return enrichmentSemaphore.withPermit {
            val enriched = enrichFlashcardData(card, force)
            if (enriched != card) {
                runCatching {
                    saveFlashcardToFirestore(enriched, card.topicId)
                }.onFailure {
                    Log.w(
                        TAG,
                        "Failed to persist enriched card (likely permission issue): ${it.message}"
                    )
                }
            }
            enriched
        }
    }

    suspend fun enrichFlashcardsParallel(cards: List<Flashcard>): List<Flashcard> {
        return coroutineScope {
            cards.map { card ->
                async {
                    enrichmentSemaphore.withPermit {
                        runCatching { enrichFlashcardData(card, false) }
                            .onFailure {
                                Log.w(
                                    TAG,
                                    "Enrichment failed for ${card.word}: ${it.message}"
                                )
                            }
                            .getOrDefault(card)
                    }
                }
            }.awaitAll()
        }
    }

    private fun extractKeyword(topicName: String): String {
        val levelPrefixPattern = Regex("^[A-C][1-2]\\s+", RegexOption.IGNORE_CASE)
        val withoutPrefix = topicName.replace(levelPrefixPattern, "")
        return withoutPrefix.ifBlank { topicName }
    }

    private fun findTopicIdForFlashcards(flashcardIds: List<String>): String? {
        return flashcardCache.entries
            .find { (_, cards) -> cards.any { it.id in flashcardIds } }
            ?.key
    }

    private suspend fun enrichFlashcardData(card: Flashcard, force: Boolean = false): Flashcard {
        var ipa = card.ipa
        var imageUrl = card.imageUrl

        if (ipa.isBlank()) {
            ipa = fetchIpa(card.word)
        }

        if (imageUrl.isBlank() || force) {
            val newImage = fetchImage(card.word)
            if (newImage.isNotBlank()) {
                imageUrl = newImage
            }
        }

        return card.copy(ipa = ipa, imageUrl = imageUrl)
    }

    private suspend fun fetchIpa(word: String): String {
        return runCatching {
            val response = freeDictionaryApi.getWordDetails(word)
            val entry = response.firstOrNull()
            entry?.phonetics?.firstOrNull { !it.text.isNullOrBlank() }?.text
                ?: entry?.phonetic
                ?: ""
        }.getOrDefault("")
    }

    private suspend fun fetchImage(word: String): String {
        return runCatching {
            val response = pixabayApi.searchImages(BuildConfig.PIXABAY_API_KEY, word)
            response.hits.firstOrNull()?.webformatUrl ?: ""
        }.getOrDefault("")
    }

    private suspend fun saveFlashcardToFirestore(flashcard: Flashcard, topicId: String) {
        val flashcardMap = flashcard.toMap()

        topicsCollection.document(topicId)
            .collection("flashcards")
            .document(flashcard.id)
            .set(flashcardMap)
            .await()
    }

    private fun Flashcard.toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "topicId" to topicId,
            "word" to word,
            "pronunciation" to pronunciation,
            "partOfSpeech" to partOfSpeech,
            "definition" to definition,
            "exampleSentence" to exampleSentence,
            "ipa" to ipa,
            "imageUrl" to imageUrl,
            "synonyms" to synonyms,
            "level" to level
        )
    }

    private fun DocumentSnapshot.toFlashcard(): Flashcard? {
        return runCatching {
            Flashcard(
                id = id,
                topicId = getString("topicId") ?: return null,
                word = getString("word") ?: return null,
                pronunciation = getString("pronunciation") ?: "",
                partOfSpeech = getString("partOfSpeech") ?: "",
                definition = getString("definition") ?: "",
                exampleSentence = getString("exampleSentence") ?: "",
                ipa = getString("ipa") ?: "",
                imageUrl = getString("imageUrl") ?: "",
                synonyms = (get("synonyms") as? List<*>)?.mapNotNull { it as? String }
                    ?: emptyList(),
                level = getString("level") ?: ""
            )
        }.getOrNull()
    }

    override suspend fun getProficiencyScore(flashcardId: String, userId: String): Result<Int> {
        return runCatching {
            val progress = userProgressDao.getProgress(userId, flashcardId)
            progress?.proficiencyScore ?: 0
        }
    }

    override suspend fun updateProficiencyScore(
        flashcardId: String,
        userId: String,
        newScore: Int
    ): Result<Unit> {
        return runCatching {
            val currentProgress = userProgressDao.getProgress(userId, flashcardId)
            val newStatus = when {
                newScore >= 6 -> ProgressStatus.MASTERED
                newScore >= 3 -> ProgressStatus.REVIEW
                else -> ProgressStatus.LEARNING
            }

            val progress = currentProgress?.copy(
                proficiencyScore = newScore,
                status = newStatus,
                updatedAt = System.currentTimeMillis(),
                syncedToRemote = false
            ) ?: UserProgressEntity(
                id = "${userId}_${flashcardId}",
                userId = userId,
                flashcardId = flashcardId,
                status = newStatus,
                proficiencyScore = newScore,
                updatedAt = System.currentTimeMillis(),
                syncedToRemote = false
            )

            userProgressDao.upsert(progress)
        }
    }

    override suspend fun uploadImage(uriString: String, flashcardId: String): Result<String> {
        return runCatching {
            val uri = android.net.Uri.parse(uriString)
            cloudinaryService.uploadFlashcardImage(uri, flashcardId)
        }.onFailure { if (it is CancellationException) throw it }
    }

    override suspend fun getUnmasteredFlashcardsByTopicId(
        topicId: String,
        userId: String
    ): Result<List<Flashcard>> {
        return runCatching {
            // Get all flashcards for the topic
            val allFlashcards = getFlashcardsByTopicId(topicId).getOrThrow()

            if (allFlashcards.isEmpty()) {
                return@runCatching emptyList()
            }

            // Get IDs of mastered flashcards
            val allFlashcardIds = allFlashcards.map { it.id }
            val masteredIds =
                userProgressDao.getMasteredFlashcardIdsFromList(userId, allFlashcardIds).toSet()

            // Filter out mastered flashcards
            allFlashcards.filter { it.id !in masteredIds }
        }.onFailure {
            if (it is CancellationException) throw it
            Log.e(TAG, "Error getting unmastered flashcards for topic $topicId", it)
        }
    }

    override suspend fun getTopicProgress(
        topicId: String,
        userId: String
    ): Result<Pair<Int, Int>> {
        return runCatching {
            // Get all flashcards for the topic
            val allFlashcards = getFlashcardsByTopicId(topicId).getOrThrow()
            val totalCount = allFlashcards.size

            if (totalCount == 0) {
                return@runCatching Pair(0, 0)
            }

            // Get count of mastered flashcards
            val allFlashcardIds = allFlashcards.map { it.id }
            val masteredCount = userProgressDao.getMasteredCountForTopic(userId, allFlashcardIds)

            Pair(masteredCount, totalCount)
        }.onFailure {
            if (it is CancellationException) throw it
            Log.e(TAG, "Error getting topic progress for topic $topicId", it)
        }
    }

    override suspend fun getMasteredFlashcardIdsFromList(
        userId: String,
        flashcardIds: List<String>
    ): Result<List<String>> {
        return runCatching {
            if (flashcardIds.isEmpty()) {
                return@runCatching emptyList()
            }
            userProgressDao.getMasteredFlashcardIdsFromList(userId, flashcardIds)
        }.onFailure {
            if (it is CancellationException) throw it
            Log.e(TAG, "Error getting mastered flashcard IDs", it)
        }
    }

    override suspend fun resetTopicProgress(
        topicId: String,
        userId: String
    ): Result<Unit> {
        return runCatching {
            // Get all flashcards for this topic
            val flashcardsResult = getFlashcardsByTopicId(topicId)
            val flashcards = flashcardsResult.getOrNull() ?: emptyList()

            // Delete progress for each flashcard
            flashcards.forEach { flashcard ->
                userProgressDao.deleteProgress(userId, flashcard.id)
            }

            Log.d(TAG, "Reset progress for topic $topicId (${flashcards.size} cards)")
            Unit
        }.onFailure {
            if (it is CancellationException) throw it
            Log.e(TAG, "Error resetting topic progress for topic $topicId", it)
        }
    }
}


