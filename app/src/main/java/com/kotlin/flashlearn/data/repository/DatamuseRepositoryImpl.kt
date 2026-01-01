package com.kotlin.flashlearn.data.repository

import com.kotlin.flashlearn.data.remote.DatamuseApi
import com.kotlin.flashlearn.data.remote.dto.WordWithDefinitionDto
import com.kotlin.flashlearn.domain.model.VocabularyWord
import com.kotlin.flashlearn.domain.model.WordSuggestion
import com.kotlin.flashlearn.domain.repository.DatamuseRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

@Singleton
class DatamuseRepositoryImpl @Inject constructor(
    private val datamuseApi: DatamuseApi
) : DatamuseRepository {

    override suspend fun getAutocompleteSuggestions(prefix: String): Result<List<WordSuggestion>> {
        if (prefix.isBlank()) return Result.success(emptyList())
        
        return runCatching {
            datamuseApi.getSuggestions(prefix).map { dto ->
                WordSuggestion(word = dto.word, score = dto.score)
            }
        }.onFailure { if (it is CancellationException) throw it }
    }

    override suspend fun getWordsByTopic(topic: String): Result<List<VocabularyWord>> {
        if (topic.isBlank()) return Result.success(emptyList())
        return fetchVocabularyWords { datamuseApi.getWordsByMeaning(topic) }
    }

    override suspend fun searchWords(pattern: String): Result<List<VocabularyWord>> {
        if (pattern.isBlank()) return Result.success(emptyList())
        return fetchVocabularyWords(filterNullDef = true) { datamuseApi.searchWords(pattern) }
    }

    override suspend fun getWordsByMeaning(meaning: String): Result<List<VocabularyWord>> {
        if (meaning.isBlank()) return Result.success(emptyList())
        return fetchVocabularyWords(filterNullDef = true) { datamuseApi.getWordsByMeaning(meaning) }
    }

    private suspend fun fetchVocabularyWords(
        filterNullDef: Boolean = false,
        apiCall: suspend () -> List<WordWithDefinitionDto>
    ): Result<List<VocabularyWord>> {
        return runCatching {
            val response = apiCall()
            if (filterNullDef) {
                response.mapNotNull { dto -> dto.toVocabularyWord() }
            } else {
                response.map { dto -> dto.toVocabularyWordOrDefault() }
            }
        }.onFailure { if (it is CancellationException) throw it }
    }

    private fun WordWithDefinitionDto.toVocabularyWord(): VocabularyWord? {
        val firstDef = getParsedDefinitions().firstOrNull() ?: return null
        return VocabularyWord(
            word = word,
            partOfSpeech = firstDef.partOfSpeech,
            definition = firstDef.definition,
            score = score
        )
    }

    private fun WordWithDefinitionDto.toVocabularyWordOrDefault(): VocabularyWord {
        val firstDef = getParsedDefinitions().firstOrNull()
        return VocabularyWord(
            word = word,
            partOfSpeech = firstDef?.partOfSpeech ?: "",
            definition = firstDef?.definition ?: "",
            score = score
        )
    }
}
