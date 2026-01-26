package com.kotlin.flashlearn.utils

import java.util.Calendar
import java.util.concurrent.TimeUnit

object DateUtils {

    fun todayAtStartOfDay(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun startOfDay(millis: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun daysBetweenTodayAnd(targetMillis: Long): Int {
        val today = todayAtStartOfDay()
        val target = startOfDay(targetMillis)
        val diff = target - today
        return TimeUnit.MILLISECONDS.toDays(diff).toInt()
    }
}
