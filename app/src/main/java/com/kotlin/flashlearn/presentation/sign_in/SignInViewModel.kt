package com.kotlin.flashlearn.presentation.sign_in

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlin.flashlearn.domain.model.User
import com.kotlin.flashlearn.domain.repository.AuthRepository
import com.kotlin.flashlearn.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.kotlin.flashlearn.data.sync.SyncRepository
import com.kotlin.flashlearn.workers.SyncScheduler
import kotlinx.coroutines.tasks.await

/**
 * ViewModel for Sign In screen.
 * Follows Google's recommended ViewModel pattern with:
 * - StateFlow for UI state (survives configuration changes)
 * - Channel for one-time events (toasts, navigation)
 * - Business logic handled in ViewModel, not in UI layer
 */
@HiltViewModel
class SignInViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val syncRepository: SyncRepository,
    private val syncScheduler: SyncScheduler,
) : ViewModel() {

    private val _state = MutableStateFlow(SignInState())
    val state = _state.asStateFlow()

    private val _uiEvent = Channel<SignInUiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    /**
     * Initiates the Google Sign-In flow.
     * Returns IntentSender to launch the sign-in UI.
     */
    suspend fun signIn(): android.content.IntentSender? {
        _state.update { it.copy(isLoading = true) }
        
        return authRepository.signIn().fold(
            onSuccess = { intentSender ->
                _state.update { it.copy(isLoading = false) }
                intentSender
            },
            onFailure = { error ->
                _state.update { it.copy(isLoading = false) }
                _uiEvent.send(SignInUiEvent.ShowError(error.message ?: "Sign in failed"))
                null
            }
        )
    }

    /**
     * Handles the sign-in result from the intent.
     */
    fun onSignInResult(intent: Intent) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            authRepository.signInWithIntent(intent).fold(
                onSuccess = { userData ->
                    try {
                        // Check if user is new
                        val isNew = userRepository.isNewUser(userData.userId)
                        
                        if (isNew) {
                            // Create new user with Google account already linked
                            val linkedAccount = com.kotlin.flashlearn.domain.model.LinkedAccount(
                                accountId = userData.userId,
                                email = userData.email ?: ""
                            )
                            val newUser = User(
                                userId = userData.userId,
                                displayName = userData.username,
                                photoUrl = userData.profilePictureUrl,
                                email = userData.email,
                                googleId = userData.userId,
                                googleIds = listOf(userData.userId),
                                linkedGoogleAccounts = listOf(linkedAccount),
                                linkedProviders = listOf("google.com")
                            )
                            userRepository.createUser(newUser)
                            syncRepository.syncAll(userData.userId)
                            syncScheduler.scheduleDailySync(userData.userId)
                            
                            _state.update { 
                                it.copy(
                                    isLoading = false,
                                    isSignInSuccessful = true
                                ) 
                            }
                            _uiEvent.send(SignInUiEvent.NavigateToOnboarding)
                        } else {
                            // Existing user
                            syncRepository.syncAll(userData.userId)
                            syncScheduler.scheduleDailySync(userData.userId)
                            _state.update { 
                                it.copy(
                                    isLoading = false,
                                    isSignInSuccessful = true
                                ) 
                            }
                            _uiEvent.send(SignInUiEvent.NavigateToHome)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        _state.update { 
                            it.copy(
                                isLoading = false,
                                signInError = "Firestore Error: ${e.message}"
                            ) 
                        }
                        _uiEvent.send(SignInUiEvent.ShowError("Database error: ${e.message}"))
                    }
                },
                onFailure = { error ->
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            signInError = error.message
                        ) 
                    }
                }
            )
        }
    }

    /**
     * Resets the state after navigation.
     */
    fun resetState() {
        _state.update { SignInState() }
    }

    // Username/Password handlers
    fun onUsernameChange(username: String) {
        _state.update { it.copy(username = username, usernameError = null) }
    }

    fun onPasswordChange(password: String) {
        _state.update { it.copy(password = password, passwordError = null) }
    }

    fun signInWithUsername() {
        viewModelScope.launch {
            val username = state.value.username.trim()
            val password = state.value.password

            // Validate inputs
            if (username.isBlank()) {
                _state.update { it.copy(usernameError = "Username is required") }
                return@launch
            }
            if (password.isBlank()) {
                _state.update { it.copy(passwordError = "Password is required") }
                return@launch
            }

            _state.update { it.copy(isLoading = true, signInError = null) }

            authRepository.signInWithUsername(username, password).fold(
                onSuccess = { userData ->
                    syncRepository.syncAll(userData.userId)
                    syncScheduler.scheduleDailySync(userData.userId)

                    _state.update { 
                        it.copy(isLoading = false, isSignInSuccessful = true) 
                    }
                    _uiEvent.send(SignInUiEvent.NavigateToHome)
                },
                onFailure = { error ->
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            signInError = error.message
                        ) 
                    }
                    _uiEvent.send(SignInUiEvent.ShowError(error.message ?: "Sign in failed"))
                }
            )
        }
    }

    fun navigateToRegister() {
        viewModelScope.launch {
            _uiEvent.send(SignInUiEvent.NavigateToRegister)
        }
    }
}