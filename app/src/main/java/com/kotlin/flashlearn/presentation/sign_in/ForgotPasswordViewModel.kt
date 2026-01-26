package com.kotlin.flashlearn.presentation.sign_in

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

data class ForgotPasswordState(
    val email: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

sealed class ForgotPasswordUiEvent {
    object NavigateBack : ForgotPasswordUiEvent()
}

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ForgotPasswordState())
    val state = _state.asStateFlow()

    private val _uiEvent = Channel<ForgotPasswordUiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    fun onEmailChange(email: String) {
        _state.update { it.copy(email = email, error = null, successMessage = null) }
    }

    fun onSubmit() {
        val email = state.value.email.trim()
        if (email.isBlank()) {
            _state.update { it.copy(error = "Please enter your email") }
            return
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
             _state.update { it.copy(error = "Please enter a valid email address") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, successMessage = null) }
            
            authRepository.checkPasswordResetEligibility(email).fold(
                onSuccess = { returnedEmail ->
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            successMessage = "Reset instructions have been sent to $returnedEmail. Please check your inbox (simulated)."
                        ) 
                    }
                },
                onFailure = { error ->
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to verify account"
                        ) 
                    }
                }
            )
        }
    }
    
    private fun maskEmail(email: String): String {
        val atIndex = email.indexOf('@')
        if (atIndex <= 1) return email
        
        val name = email.substring(0, atIndex)
        val domain = email.substring(atIndex)
        
        val visibleChars = if (name.length > 2) 2 else 0
        val maskedName = name.take(visibleChars) + "***" + name.takeLast(1)
        
        return maskedName + domain
    }

    fun onBackClick() {
        viewModelScope.launch {
            _uiEvent.send(ForgotPasswordUiEvent.NavigateBack)
        }
    }
}
