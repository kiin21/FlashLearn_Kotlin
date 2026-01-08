package com.kotlin.flashlearn.presentation.sign_in

/**
 * UI State for Sign In screen.
 * Follows Unidirectional Data Flow (UDF) pattern.
 */
data class SignInState(
    val isLoading: Boolean = false,
    val isSignInSuccessful: Boolean = false,
    val uid: String? = null,
    val signInError: String? = null
)
