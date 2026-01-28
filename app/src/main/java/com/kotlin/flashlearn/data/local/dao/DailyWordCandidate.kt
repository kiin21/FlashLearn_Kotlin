package com.kotlin.flashlearn.data.local.dao

data class DailyWordCandidate(
    val id: String,
    val word: String,
    val meaning: String?,
    val ipa: String?,
    val level: String?
)
