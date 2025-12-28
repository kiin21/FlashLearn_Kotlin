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
    private val pixabayApi: com.kotlin.flashlearn.data.remote.PixabayApi
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
            // 1. Try to get from database first
            var flashcards = getFlashcardsFromDatabase(topicId)
            
            // 2. Fallback to Datamuse if DB empty (ONLY for System Topics)
            if (flashcards.isEmpty()) {
                // Check if it is a system topic first
                val topicResult = topicRepository.getTopicById(topicId)
                val topic = topicResult.getOrNull()
                
                // Only fetch default words if it is a system topic. 
                // User topics should remain empty if the user deleted everything.
                if (topic != null && topic.isSystemTopic) {
                    flashcards = getFlashcardsFromDatamuse(topicId)
                    // SAVE these immediately to DB to persist IDs and prevent partial data issues later
                    if (flashcards.isNotEmpty()) {
                         saveFlashcardsForTopic(topicId, flashcards)
                    }
                }
            }

            // 3. Update cache and return (Deduplicate by word to fix UI issue)
            val uniqueFlashcards = flashcards.distinctBy { it.word.lowercase() }
            
            if (uniqueFlashcards.isNotEmpty()) {
                flashcardCache[topicId] = uniqueFlashcards
            }
            
            Result.success(uniqueFlashcards)
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
            val enrichedFlashcards = flashcards.map { enrichFlashcardData(it) }

            for (flashcard in enrichedFlashcards) {
                saveFlashcardToDb(flashcard, topicId)
            }
            
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
    suspend fun enrichFlashcard(card: Flashcard): Flashcard {
        return enrichFlashcardData(card).also { enrichedCard ->
            if (enrichedCard != card) {
                saveFlashcardToDb(enrichedCard, card.topicId)
            }
        }
    }

    private suspend fun enrichFlashcardData(card: Flashcard): Flashcard {
        var ipa = card.ipa
        var imageUrl = card.imageUrl
        
        // Fetch IPA if missing
        if (ipa.isBlank()) {
            try {
                println("FlashLearn: Fetching IPA for ${card.word}...")
                val response = freeDictionaryApi.getWordDetails(card.word)
                val entry = response.firstOrNull()
                
                // Try to find IPA in phonetics list first, then fallback to top-level phonetic
                ipa = entry?.phonetics?.firstOrNull { !it.text.isNullOrBlank() }?.text
                    ?: entry?.phonetic
                    ?: ""
                    
                println("FlashLearn: IPA found: $ipa")
            } catch (e: Exception) {
                println("FlashLearn: IPA fetch error: ${e.message}")
                e.printStackTrace()
            }
        }
        
        // Fetch Image if missing (Pixabay)
        if (imageUrl.isBlank()) {
            try {
                println("FlashLearn: Fetching image for ${card.word} from Pixabay...")
                val response = pixabayApi.searchImages(
                    apiKey = BuildConfig.PIXABAY_API_KEY,
                    query = card.word
                )
                println("FlashLearn: Pixabay hits: ${response.hits.size}")
                imageUrl = response.hits.firstOrNull()?.webformatUrl ?: ""
                println("FlashLearn: Image URL found: $imageUrl")
            } catch (e: Exception) {
                println("FlashLearn: Pixabay error: ${e.message}")
                e.printStackTrace()
            }
        }
        
        return card.copy(ipa = ipa, imageUrl = imageUrl)
    }

    private suspend fun saveFlashcardToDb(flashcard: Flashcard, topicId: String) {
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
                flashcard.ipa,
                flashcard.imageUrl
            )
        )
        
        neonSqlApi.executeQuery(
            connectionString = CONNECTION_STRING,
            request = request
        )
    }

    override suspend fun deleteFlashcards(flashcardIds: List<String>): Result<Unit> {
        return try {
            if (flashcardIds.isEmpty()) return Result.success(Unit)

            // Since we are using an HTTP API that might not support array parameters easily in the simple query wrapper,
            // we will construct a query with multiple placeholders or loop.
            // For robustness with list size, let's just loop for now or batch in small groups. 
            // Given typical usage, list size won't be huge.
            // Optimization: Construct "unrolled" IN clause: WHERE id IN ('id1', 'id2', ...)
            
            val idsString = flashcardIds.joinToString(",") { "'$it'" }
            val request = NeonSqlRequest(
                query = "DELETE FROM flashcards WHERE id IN ($idsString)",
                params = emptyList() // Parameters embedded directly for list
            )
            
            val response = neonSqlApi.executeQuery(
                connectionString = CONNECTION_STRING,
                request = request
            )
            
            if (response.error != null) {
                return Result.failure(Exception(response.error.message))
            }
            
            // Clear cache to ensure UI refreshes correctly
            flashcardCache.clear()
            
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }
}
