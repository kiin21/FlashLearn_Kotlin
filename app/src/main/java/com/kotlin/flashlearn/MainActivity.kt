package com.kotlin.flashlearn

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.kotlin.flashlearn.domain.repository.AuthRepository
import com.kotlin.flashlearn.domain.repository.UserRepository
import com.kotlin.flashlearn.notification.DailyReminderPermissionGate
import com.kotlin.flashlearn.presentation.navigation.FlashlearnNavHost
import com.kotlin.flashlearn.ui.theme.FlashlearnTheme
import com.kotlin.flashlearn.util.ThemeManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var languageManager: com.kotlin.flashlearn.util.LanguageManager

    @Inject
    lateinit var themeManager: ThemeManager

    private lateinit var reminderGate: DailyReminderPermissionGate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        languageManager.applySavedLanguage()
        enableEdgeToEdge()

        reminderGate = DailyReminderPermissionGate(this)
        reminderGate.apply {
            ensureDailyReminder(
                hour = 9,
                minute = 0,
                title = "FlashLearn",
                body = "It's time for class!"
            )
            ensureExamReminder(
                hour = 7,
                minute = 0,
                title = "FlashLearn"
            )
        }


        setContent {
            val themeMode by themeManager.themeMode.collectAsState()
            val isSystemDark = isSystemInDarkTheme()
            val isDarkTheme = when (themeMode) {
                ThemeManager.MODE_DARK -> true
                ThemeManager.MODE_LIGHT -> false
                else -> isSystemDark
            }

            FlashlearnTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    FlashlearnNavHost(
                        navController = navController,
                        authRepository = authRepository,
                        userRepository = userRepository,
                        languageManager = languageManager,
                        themeManager = themeManager
                    )
                }
            }
        }
    }
}