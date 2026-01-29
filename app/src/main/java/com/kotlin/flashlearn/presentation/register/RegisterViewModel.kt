package com.kotlin.flashlearn.presentation.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlin.flashlearn.data.util.PasswordUtils
import com.kotlin.flashlearn.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RegisterState())
    val state = _state.asStateFlow()

    private val _uiEvent = Channel<RegisterUiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    fun onUsernameChange(username: String) {
        _state.update { it.copy(username = username, usernameError = null) }
    }

    fun onPasswordChange(password: String) {
        _state.update { it.copy(password = password, passwordError = null) }
    }

    fun onConfirmPasswordChange(confirmPassword: String) {
        _state.update { it.copy(confirmPassword = confirmPassword, confirmPasswordError = null) }
    }

    fun register() {
        viewModelScope.launch {
            val username = state.value.username.trim()
            val password = state.value.password
            val confirmPassword = state.value.confirmPassword

            // Validation
            var hasError = false

            if (username.isBlank()) {
                _state.update { it.copy(usernameError = "Username is required") }
                hasError = true
            } else if (username.length < 3) {
                _state.update { it.copy(usernameError = "Username must be at least 3 characters") }
                hasError = true
            }

            val passwordError = PasswordUtils.getPasswordError(password)
            if (passwordError != null) {
                _state.update { it.copy(passwordError = passwordError) }
                hasError = true
            }

            if (password != confirmPassword) {
                _state.update { it.copy(confirmPasswordError = "Passwords do not match") }
                hasError = true
            }

            if (hasError) return@launch

            _state.update { it.copy(isLoading = true, generalError = null) }

            authRepository.registerWithUsername(username, password).fold(
                onSuccess = { userData ->
                    _state.update { it.copy(isLoading = false) }
                    _uiEvent.send(RegisterUiEvent.NavigateToOnboarding)
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            generalError = error.message
                        )
                    }
                    _uiEvent.send(RegisterUiEvent.ShowError(error.message ?: "Registration failed"))
                }
            )
        }
    }

    fun navigateToSignIn() {
        viewModelScope.launch {
            _uiEvent.send(RegisterUiEvent.NavigateToSignIn)
        }
    }
}
