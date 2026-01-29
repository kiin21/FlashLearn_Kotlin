package com.kotlin.flashlearn.data.repository

import com.kotlin.flashlearn.data.remote.FreeDictionaryApi
import com.kotlin.flashlearn.domain.model.VocabularyWord
import com.kotlin.flashlearn.domain.repository.DictionaryRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

@Singleton
class DictionaryRepositoryImpl @Inject constructor(
    private val freeDictionaryApi: FreeDictionaryApi
) : DictionaryRepository {

    override suspend fun getWordExtendedDetails(word: String): Result<VocabularyWord> {
        return runCatching {
            val response = freeDictionaryApi.getWordDetails(word)
            val entry = response.firstOrNull() ?: throw NoSuchElementException("Word not found")
            
            val phonetic = entry.phonetics?.firstOrNull { !it.text.isNullOrBlank() }?.text
                ?: entry.phonetic
                ?: ""
            
            // Find a meaning that has at least one definition
            val meaningWithDef = entry.meanings?.firstOrNull { it.definitions.isNotEmpty() }
            val firstDef = meaningWithDef?.definitions?.firstOrNull()
            
            VocabularyWord(
                word = entry.word,
                partOfSpeech = meaningWithDef?.partOfSpeech ?: "",
                definition = firstDef?.definition ?: "",
                ipa = phonetic,
                example = firstDef?.example ?: ""
            )
        }.onFailure {
            if (it is CancellationException) throw it
        }
    }
}
