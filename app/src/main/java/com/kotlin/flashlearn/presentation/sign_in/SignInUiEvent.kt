package com.kotlin.flashlearn.presentation.sign_in

/**
 * One-time UI events for Sign In screen.
 * Using sealed interface for events that should be consumed once (toasts, navigation).
 */
sealed interface SignInUiEvent {
    data class ShowError(val message: String) : SignInUiEvent
    data object NavigateToProfile : SignInUiEvent
}
