package com.kotlin.flashlearn.presentation.noti

import android.content.Context

object ExamReminderPrefs {
    private const val PREFS = "flashlearn_prefs"
    private const val KEY_SCHEDULED = "exam_reminder_scheduled"

    fun wasScheduled(context: Context): Boolean {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return sp.getBoolean(KEY_SCHEDULED, false)
    }

    fun markScheduled(context: Context) {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        sp.edit().putBoolean(KEY_SCHEDULED, true).apply()
    }
}
