package com.kotlin.flashlearn.domain.widget

import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object WidgetDate {

    private val FMT = DateTimeFormatter.ISO_LOCAL_DATE

    private fun todayDate(): LocalDate =
        LocalDate.now(ZoneId.systemDefault())

    fun today(): String =
        todayDate().format(FMT)

    fun yesterday(date: String?): String {
        val parsed = try {
            LocalDate.parse(date, FMT)
        } catch (e: DateTimeParseException) {
            todayDate()
        }

        return parsed
            .minusDays(1)
            .format(FMT)
    }
}
