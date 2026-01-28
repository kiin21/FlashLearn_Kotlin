package com.kotlin.flashlearn.domain.model

data class DailyWordArchiveItem(
    val dateKey: String,
    val wordId: String,
    val word: String,
    val meaning: String?,
    val ipa: String?
)
