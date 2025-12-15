package com.kotlin.flashlearn.presentation.sign_in

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlin.flashlearn.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Sign In screen.
 * Follows Google's recommended ViewModel pattern with:
 * - StateFlow for UI state (survives configuration changes)
 * - Channel for one-time events (toasts, navigation)
 * - Business logic handled in ViewModel, not in UI layer
 */
@HiltViewModel
class SignInViewModel @Inject constructor(
    private val authRepository: AuthRepository
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
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            isSignInSuccessful = true
                        ) 
                    }
                    _uiEvent.send(SignInUiEvent.NavigateToProfile)
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