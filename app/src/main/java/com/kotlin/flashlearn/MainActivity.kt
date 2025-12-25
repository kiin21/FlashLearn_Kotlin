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
import com.kotlin.flashlearn.presentation.navigation.FlashlearnNavHost
import com.kotlin.flashlearn.ui.theme.FlashlearnTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Main Activity following Google best practices:
 * - @AndroidEntryPoint for Hilt injection
 * - Minimal logic - delegates to composables
 * - Uses edge-to-edge display
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                        authRepository = authRepository
                    )
                }
            }
        }
    }
}