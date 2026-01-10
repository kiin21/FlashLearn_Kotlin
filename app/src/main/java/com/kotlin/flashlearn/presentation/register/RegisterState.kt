package com.kotlin.flashlearn.presentation.register

/**
 * UI State for Register screen.
 */
data class RegisterState(
    val isLoading: Boolean = false,
    val username: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val usernameError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val generalError: String? = null
)
