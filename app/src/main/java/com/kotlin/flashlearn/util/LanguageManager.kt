package com.kotlin.flashlearn.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages app language/locale settings.
 * Uses AppCompatDelegate for per-app language support (API 33+).
 */
@Singleton
class LanguageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val LANGUAGE_ENGLISH = "en"
        const val LANGUAGE_VIETNAMESE = "vi"

        private const val PREFS_NAME = "language_prefs"
        private const val KEY_LANGUAGE = "selected_language"
    }

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Get the currently selected language code.
     * Returns "en" as default if no language is set.
     */
    fun getLanguage(): String {
        return prefs.getString(KEY_LANGUAGE, LANGUAGE_ENGLISH) ?: LANGUAGE_ENGLISH
    }

    /**
     * Set the app language and persist the preference.
     * This will update the app's locale using AppCompatDelegate.
     *
     * @param languageCode The language code (e.g., "en", "vi")
     */
    fun setLanguage(languageCode: String) {
        // Save to SharedPreferences
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply()

        // Apply the locale using AppCompatDelegate (API 33+ per-app language)
        val localeList = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    /**
     * Apply the saved language preference.
     * Call this in Application.onCreate() or MainActivity.onCreate()
     */
    fun applySavedLanguage() {
        val savedLanguage = getLanguage()
        val localeList = LocaleListCompat.forLanguageTags(savedLanguage)
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    /**
     * Check if the current language is Vietnamese
     */
    fun isVietnamese(): Boolean = getLanguage() == LANGUAGE_VIETNAMESE

    /**
     * Check if the current language is English
     */
    fun isEnglish(): Boolean = getLanguage() == LANGUAGE_ENGLISH
}
