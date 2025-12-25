package com.kotlin.flashlearn.data.repository

import com.kotlin.flashlearn.data.remote.DatamuseApi
import com.kotlin.flashlearn.domain.model.VocabularyWord
import com.kotlin.flashlearn.domain.model.WordSuggestion
import com.kotlin.flashlearn.domain.repository.DatamuseRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/**
 * Implementation of DatamuseRepository using Datamuse API.
 */
@Singleton
class DatamuseRepositoryImpl @Inject constructor(
    private val datamuseApi: DatamuseApi
) : DatamuseRepository {
    
    override suspend fun getAutocompleteSuggestions(prefix: String): Result<List<WordSuggestion>> {
        return try {
            if (prefix.isBlank()) {
                return Result.success(emptyList())
            }
            val response = datamuseApi.getSuggestions(prefix)
            val suggestions = response.map { dto ->
                WordSuggestion(word = dto.word, score = dto.score)
            }
            Result.success(suggestions)
        } catch (e: Exception) {
            e.printStackTrace()
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }
    
    override suspend fun getWordsByTopic(topic: String): Result<List<VocabularyWord>> {
        return try {
            if (topic.isBlank()) {
                return Result.success(emptyList())
            }
            // Use 'ml=' (meaning like) instead of 'topics=' as it returns more results
            val response = datamuseApi.getWordsByMeaning(topic)
            val words = response.mapNotNull { dto ->
                val definitions = dto.getParsedDefinitions()
                if (definitions.isNotEmpty()) {
                    val firstDef = definitions.first()
                    VocabularyWord(
                        word = dto.word,
                        partOfSpeech = firstDef.partOfSpeech,
                        definition = firstDef.definition,
                        score = dto.score
                    )
                } else {
                    // Include word even without definition
                    VocabularyWord(
                        word = dto.word,
                        partOfSpeech = "",
                        definition = "",
                        score = dto.score
                    )
                }
            }
            Result.success(words)
        } catch (e: Exception) {
            e.printStackTrace()
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }
    
    override suspend fun searchWords(pattern: String): Result<List<VocabularyWord>> {
        return try {
            if (pattern.isBlank()) {
                return Result.success(emptyList())
            }
            val response = datamuseApi.searchWords(pattern)
            val words = response.mapNotNull { dto ->
                val definitions = dto.getParsedDefinitions()
                if (definitions.isNotEmpty()) {
                    val firstDef = definitions.first()
                    VocabularyWord(
                        word = dto.word,
                        partOfSpeech = firstDef.partOfSpeech,
                        definition = firstDef.definition,
                        score = dto.score
                    )
                } else null
            }
            Result.success(words)
        } catch (e: Exception) {
            e.printStackTrace()
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }
    
    override suspend fun getWordsByMeaning(meaning: String): Result<List<VocabularyWord>> {
        return try {
            if (meaning.isBlank()) {
                return Result.success(emptyList())
            }
            val response = datamuseApi.getWordsByMeaning(meaning)
            val words = response.mapNotNull { dto ->
                val definitions = dto.getParsedDefinitions()
                if (definitions.isNotEmpty()) {
                    val firstDef = definitions.first()
                    VocabularyWord(
                        word = dto.word,
                        partOfSpeech = firstDef.partOfSpeech,
                        definition = firstDef.definition,
                        score = dto.score
                    )
                } else null
            }
            Result.success(words)
        } catch (e: Exception) {
            e.printStackTrace()
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }
}
