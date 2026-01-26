package com.kotlin.flashlearn.presentation.noti

import android.content.Context

object ExamDatePrefs {
    private const val PREFS = "flashlearn_prefs"
    private const val KEY_EXAM_DATE = "vstep_exam_date_millis"

    fun get(context: Context): Long? {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val v = sp.getLong(KEY_EXAM_DATE, -1L)
        return if (v > 0) v else null
    }

    fun set(context: Context, millis: Long) {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        sp.edit().putLong(KEY_EXAM_DATE, millis).apply()
    }

    fun clear(context: Context) {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        sp.edit().remove(KEY_EXAM_DATE).apply()
    }
}
