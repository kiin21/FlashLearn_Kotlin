package com.kotlin.flashlearn.presentation.reset_password

import androidx.lifecycle.SavedStateHandle
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

data class ResetPasswordState(
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val isTokenVerifying: Boolean = true,
    val isTokenValid: Boolean = false,
    val error: String? = null,
    val passwordError: String? = null
)

sealed class ResetPasswordUiEvent {
    object NavigateToSignIn : ResetPasswordUiEvent()
    object ShowSuccess : ResetPasswordUiEvent()
}

@HiltViewModel
class ResetPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val token: String = checkNotNull(savedStateHandle["token"])

    private val _state = MutableStateFlow(ResetPasswordState())
    val state = _state.asStateFlow()

    private val _uiEvent = Channel<ResetPasswordUiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        verifyToken()
    }

    private fun verifyToken() {
        viewModelScope.launch {
            _state.update { it.copy(isTokenVerifying = true, error = null) }
            authRepository.verifyPasswordResetToken(token).fold(
                onSuccess = {
                    _state.update { it.copy(isTokenVerifying = false, isTokenValid = true) }
                },
                onFailure = { error ->
                    _state.update { 
                        it.copy(
                            isTokenVerifying = false, 
                            isTokenValid = false,
                            error = error.message ?: "Invalid or expired token"
                        ) 
                    }
                }
            )
        }
    }

    fun onPasswordChange(password: String) {
        _state.update { it.copy(password = password, passwordError = null, error = null) }
    }

    fun onConfirmPasswordChange(confirmPassword: String) {
        _state.update { it.copy(confirmPassword = confirmPassword, error = null) }
    }

    fun onSubmit() {
        if (state.value.password != state.value.confirmPassword) {
            _state.update { it.copy(passwordError = "Passwords do not match") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            authRepository.resetPasswordWithToken(token, state.value.password).fold(
                onSuccess = {
                    _state.update { it.copy(isLoading = false) }
                    _uiEvent.send(ResetPasswordUiEvent.ShowSuccess)
                    // Delay slightly or wait for user to click button? 
                    // Let's send event and let UI handle it
                    _uiEvent.send(ResetPasswordUiEvent.NavigateToSignIn)
                },
                onFailure = { error ->
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to reset password"
                        ) 
                    }
                }
            )
        }
    }
}
