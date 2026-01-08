package com.kotlin.flashlearn.presentation.noti

import android.content.Context

object AutoReminderPrefs {
    private const val PREF = "flashlearn_prefs"
    private const val KEY = "auto_daily_reminder_scheduled"

    fun wasScheduled(context: Context): Boolean {
        return context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getBoolean(KEY, false)
    }

    fun markScheduled(context: Context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY, true)
            .apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY)
            .apply()
    }
}
