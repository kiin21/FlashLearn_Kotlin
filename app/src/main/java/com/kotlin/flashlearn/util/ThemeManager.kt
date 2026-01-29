package com.kotlin.flashlearn.util

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages app theme (dark/light mode) settings.
 * Uses SharedPreferences for persistence and StateFlow for reactive updates.
 */
@Singleton
class ThemeManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "theme_prefs"
        private const val KEY_THEME_MODE = "theme_mode"

        // Theme mode constants
        const val MODE_SYSTEM = 0  // Follow system setting
        const val MODE_LIGHT = 1   // Always light
        const val MODE_DARK = 2    // Always dark
    }

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val _themeMode = MutableStateFlow(getSavedThemeMode())
    val themeMode: StateFlow<Int> = _themeMode.asStateFlow()

    /**
     * Get the saved theme mode from SharedPreferences.
     * Returns MODE_SYSTEM as default.
     */
    private fun getSavedThemeMode(): Int {
        return prefs.getInt(KEY_THEME_MODE, MODE_SYSTEM)
    }

    /**
     * Set the theme mode and persist to SharedPreferences.
     *
     * @param mode One of MODE_SYSTEM, MODE_LIGHT, or MODE_DARK
     */
    fun setThemeMode(mode: Int) {
        prefs.edit().putInt(KEY_THEME_MODE, mode).apply()
        _themeMode.value = mode
    }

    /**
     * Toggle between dark and light mode.
     * If currently following system, will switch to the opposite of current appearance.
     */
    fun toggleDarkMode(isCurrentlyDark: Boolean) {
        val newMode = if (isCurrentlyDark) MODE_LIGHT else MODE_DARK
        setThemeMode(newMode)
    }

    /**
     * Check if dark mode is enabled based on current theme mode and system setting.
     *
     * @param isSystemDark Whether the system is currently in dark mode
     * @return true if app should use dark theme
     */
    fun isDarkMode(isSystemDark: Boolean): Boolean {
        return when (_themeMode.value) {
            MODE_DARK -> true
            MODE_LIGHT -> false
            else -> isSystemDark // MODE_SYSTEM
        }
    }

    /**
     * Check if currently following system theme
     */
    fun isFollowingSystem(): Boolean = _themeMode.value == MODE_SYSTEM
}
