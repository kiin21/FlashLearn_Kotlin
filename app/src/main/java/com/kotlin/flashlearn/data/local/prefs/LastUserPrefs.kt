package com.kotlin.flashlearn.data.local.prefs

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LastUserPrefs @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val sp = context.getSharedPreferences("flashlearn_prefs", Context.MODE_PRIVATE)

    fun setLastUserId(userId: String) {
        sp.edit { putString("last_user_id", userId) }
    }

    fun getLastUserId(): String? = sp.getString("last_user_id", null)
}
