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
    private val userRepository: UserRepository
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
                            // Create new user
                            val newUser = User(
                                userId = userData.userId,
                                displayName = userData.username,
                                photoUrl = userData.profilePictureUrl
                            )
                            userRepository.createUser(newUser)
                            
                            _state.update { 
                                it.copy(
                                    isLoading = false,
                                    isSignInSuccessful = true
                                ) 
                            }
                            _uiEvent.send(SignInUiEvent.NavigateToOnboarding)
                        } else {
                            // Existing user
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
}