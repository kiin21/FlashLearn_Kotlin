package com.kotlin.flashlearn.domain.repository

import com.kotlin.flashlearn.domain.model.VocabularyWord

interface DictionaryRepository {

    suspend fun getWordExtendedDetails(word: String): Result<VocabularyWord>
}
