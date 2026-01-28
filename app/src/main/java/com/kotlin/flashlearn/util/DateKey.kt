package com.kotlin.flashlearn.utils

import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DateKey {
    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun today(): String = LocalDate.now(ZoneId.systemDefault()).format(fmt)

    fun fromMillis(millis: Long): String {
        val d = java.time.Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
        return d.format(fmt)
    }
}
