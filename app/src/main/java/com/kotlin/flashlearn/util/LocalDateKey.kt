package com.kotlin.flashlearn.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object LocalDateKey {
    private val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun today(): String = fmt.format(Calendar.getInstance().time)

    fun yesterday(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        return fmt.format(cal.time)
    }
}