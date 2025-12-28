package com.kotlin.flashlearn.data.repository

import com.kotlin.flashlearn.BuildConfig
import com.kotlin.flashlearn.data.remote.DatamuseApi
import com.kotlin.flashlearn.data.remote.NeonSqlApi
import com.kotlin.flashlearn.data.remote.dto.FlashcardDto
import com.kotlin.flashlearn.data.remote.dto.NeonSqlRequest
import com.kotlin.flashlearn.domain.model.Flashcard
import com.kotlin.flashlearn.domain.repository.FlashcardRepository
import com.kotlin.flashlearn.domain.repository.TopicRepository
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/**
 * Implementation of FlashcardRepository using PostgreSQL database for persistence
 * and Datamuse API as fallback for system topics.
 * 
 * Data flow:
 * 1. Check database for flashcards
 * 2. If not found, fetch from Datamuse API (for system topics)
 * 3. User-created flashcards are saved to database
 */
@Singleton
class FlashcardRepositoryImpl @Inject constructor(
    private val neonSqlApi: NeonSqlApi,
    private val datamuseApi: DatamuseApi,
    private val topicRepository: TopicRepository,
    private val freeDictionaryApi: com.kotlin.flashlearn.data.remote.FreeDictionaryApi,
    private val unsplashApi: com.kotlin.flashlearn.data.remote.UnsplashApi
) : FlashcardRepository {
    
    companion object {
        private val CONNECTION_STRING = BuildConfig.NEON_CONNECTION_STRING
        private const val SELECT_COLUMNS = "id, topic_id, word, pronunciation, part_of_speech, definition, example_sentence, ipa, image_url"
    }
    
    // In-memory cache for loaded flashcards (by topicId)
    private val flashcardCache = ConcurrentHashMap<String, List<Flashcard>>()
    
    // Track mastered/review status in memory
    private val masteredCards = ConcurrentHashMap<String, MutableSet<String>>()
    private val reviewCards = ConcurrentHashMap<String, MutableSet<String>>()
    
    override suspend fun getFlashcardsByTopicId(topicId: String): Result<List<Flashcard>> {
        return try {
            // Check cache first
            flashcardCache[topicId]?.let { 
                return Result.success(it) 
            }
            
            // Try to get from database first
            val dbFlashcards = getFlashcardsFromDatabase(topicId)
            if (dbFlashcards.isNotEmpty()) {
                flashcardCache[topicId] = dbFlashcards
                return Result.success(dbFlashcards)
            }
            
            // Fallback to Datamuse API for system topics
            val flashcards = getFlashcardsFromDatamuse(topicId)
            if (flashcards.isNotEmpty()) {
                flashcardCache[topicId] = flashcards
            }
            
            Result.success(flashcards)
        } catch (e: Exception) {
            e.printStackTrace()
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }
    
    private suspend fun getFlashcardsFromDatabase(topicId: String): List<Flashcard> {
        return try {
            val request = NeonSqlRequest(
                query = "SELECT $SELECT_COLUMNS FROM flashcards WHERE topic_id = \$1 ORDER BY word ASC",
                params = listOf(topicId)
            )
            
            val response = neonSqlApi.executeQuery(
                connectionString = CONNECTION_STRING,
                request = request
            )
            
            if (response.error != null) {
                return emptyList()
            }
            
            response.rows?.map { row ->
                FlashcardDto.fromRow(row).toDomain()
            } ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    private suspend fun getFlashcardsFromDatamuse(topicId: String): List<Flashcard> {
        // Get topic to retrieve its name
        val topicResult = topicRepository.getTopicById(topicId)
        val topic = topicResult.getOrNull() ?: return emptyList()
        
        // Extract keyword from topic name
        val keyword = extractKeyword(topic.name)
        
        // Fetch vocabulary from Datamuse API
        val response = datamuseApi.getWordsByMeaning(keyword)
        
        return response.mapNotNull { dto ->
            val definitions = dto.getParsedDefinitions()
            if (definitions.isNotEmpty()) {
                val firstDef = definitions.first()
                Flashcard(
                    id = UUID.randomUUID().toString(),
                    topicId = topicId,
                    word = dto.word.replaceFirstChar { it.uppercase() },
                    pronunciation = "",
                    partOfSpeech = firstDef.partOfSpeech.uppercase(),
                    definition = firstDef.definition,
                    exampleSentence = ""
                )
            } else {
                Flashcard(
                    id = UUID.randomUUID().toString(),
                    topicId = topicId,
                    word = dto.word.replaceFirstChar { it.uppercase() },
                    pronunciation = "",
                    partOfSpeech = "",
                    definition = "",
                    exampleSentence = ""
                )
            }
        }
    }
    
    override suspend fun saveFlashcardsForTopic(topicId: String, flashcards: List<Flashcard>): Result<Unit> {
        return try {
            // Save each flashcard to database
            // Process each flashcard to enrich data if needed
            val enrichedFlashcards = flashcards.map { card ->
                var ipa = card.ipa
                var imageUrl = card.imageUrl
                
                // Fetch IPA if missing
                if (ipa.isBlank()) {
                    try {
                        val response = freeDictionaryApi.getWordDetails(card.word)
                        ipa = response.firstOrNull()?.phonetics?.firstOrNull { !it.text.isNullOrBlank() }?.text ?: ""
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                
                // Fetch Image if missing
                if (imageUrl.isBlank()) {
                    try {
                        val response = unsplashApi.searchPhotos(
                            query = card.word,
                            authorization = "Client-ID ${BuildConfig.UNSPLASH_ACCESS_KEY}"
                        )
                        imageUrl = response.results.firstOrNull()?.urls?.regular ?: ""
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                
                card.copy(ipa = ipa, imageUrl = imageUrl)
            }

            for (flashcard in enrichedFlashcards) {
                val finalIpa = flashcard.ipa
                val finalImageUrl = flashcard.imageUrl
                
                val request = NeonSqlRequest(
                    query = """
                        INSERT INTO flashcards (id, topic_id, word, pronunciation, part_of_speech, definition, example_sentence, ipa, image_url)
                        VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
                        ON CONFLICT (id) DO UPDATE SET
                            word = EXCLUDED.word,
                            pronunciation = EXCLUDED.pronunciation,
                            part_of_speech = EXCLUDED.part_of_speech,
                            definition = EXCLUDED.definition,
                            example_sentence = EXCLUDED.example_sentence,
                            ipa = EXCLUDED.ipa,
                            image_url = EXCLUDED.image_url
                    """.trimIndent(),
                    params = listOf(
                        flashcard.id,
                        topicId,
                        flashcard.word,
                        flashcard.pronunciation,
                        flashcard.partOfSpeech,
                        flashcard.definition,
                        flashcard.exampleSentence,
                        finalIpa,
                        finalImageUrl
                    )
                )
                
                val response = neonSqlApi.executeQuery(
                    connectionString = CONNECTION_STRING,
                    request = request
                )
                
                if (response.error != null) {
                    return Result.failure(Exception(response.error.message))
                }
            }
            
            // Update cache
            flashcardCache[topicId] = enrichedFlashcards
            
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }
    
    override suspend fun getFlashcardById(cardId: String): Result<Flashcard?> {
        return try {
            // Check cache first
            for ((_, flashcards) in flashcardCache) {
                val card = flashcards.find { it.id == cardId }
                if (card != null) {
                    return Result.success(card)
                }
            }
            
            // Try database
            val request = NeonSqlRequest(
                query = "SELECT $SELECT_COLUMNS FROM flashcards WHERE id = \$1",
                params = listOf(cardId)
            )
            
            val response = neonSqlApi.executeQuery(
                connectionString = CONNECTION_STRING,
                request = request
            )
            
            if (response.error != null) {
                return Result.success(null)
            }
            
            val flashcard = response.rows?.firstOrNull()?.let { row ->
                FlashcardDto.fromRow(row).toDomain()
            }
            
            Result.success(flashcard)
        } catch (e: Exception) {
            e.printStackTrace()
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }
    
    override suspend fun markFlashcardAsMastered(flashcardId: String, userId: String): Result<Unit> {
        return try {
            val userMastered = masteredCards.getOrPut(userId) { mutableSetOf() }
            userMastered.add(flashcardId)
            reviewCards[userId]?.remove(flashcardId)
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }
    
    override suspend fun markFlashcardForReview(flashcardId: String, userId: String): Result<Unit> {
        return try {
            val userReview = reviewCards.getOrPut(userId) { mutableSetOf() }
            userReview.add(flashcardId)
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }
    
    /**
     * Extracts the main keyword from topic name by removing common prefixes.
     */
    private fun extractKeyword(topicName: String): String {
        val levelPrefixPattern = Regex("^[A-C][1-2]\\s+", RegexOption.IGNORE_CASE)
        val withoutPrefix = topicName.replace(levelPrefixPattern, "")
        return withoutPrefix.ifBlank { topicName }
    }
    
    /**
     * Clears the cache for a specific topic or all topics.
     */
    fun clearCache(topicId: String? = null) {
        if (topicId != null) {
            flashcardCache.remove(topicId)
        } else {
            flashcardCache.clear()
        }
    }
}
