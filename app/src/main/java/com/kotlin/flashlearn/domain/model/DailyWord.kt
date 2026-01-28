package com.kotlin.flashlearn.domain.model

data class DailyWord(
    val wordId: String,
    val word: String,
    val level: String?,
    val meaning: String?,
    val ipa: String?,
    val dateKey: String
)
