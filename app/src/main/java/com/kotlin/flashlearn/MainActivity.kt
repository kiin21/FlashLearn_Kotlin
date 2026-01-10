package com.kotlin.flashlearn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.kotlin.flashlearn.domain.repository.AuthRepository
import com.kotlin.flashlearn.domain.repository.UserRepository
import com.kotlin.flashlearn.presentation.navigation.FlashlearnNavHost
import com.kotlin.flashlearn.ui.theme.FlashlearnTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

import androidx.appcompat.app.AppCompatActivity

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var authRepository: AuthRepository
    
    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var languageManager: com.kotlin.flashlearn.util.LanguageManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        languageManager.applySavedLanguage()
        enableEdgeToEdge()
        
        setContent {    
            FlashlearnTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    FlashlearnNavHost(
                        navController = navController,
                        authRepository = authRepository,
                        userRepository = userRepository,
                        languageManager = languageManager
                    )
                }
            }
        }
    }
}