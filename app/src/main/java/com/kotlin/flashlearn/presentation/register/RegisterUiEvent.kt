package com.kotlin.flashlearn.presentation.register

/**
 * One-time UI events for Register screen.
 */
sealed interface RegisterUiEvent {
    data class ShowError(val message: String) : RegisterUiEvent
    data object NavigateToOnboarding : RegisterUiEvent
    data object NavigateToSignIn : RegisterUiEvent
}
