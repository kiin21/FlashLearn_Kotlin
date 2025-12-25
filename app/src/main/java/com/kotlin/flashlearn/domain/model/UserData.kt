package com.kotlin.flashlearn.domain.model

/**
 * Domain model representing authenticated user data.
 * This is a pure domain model with no dependencies on external frameworks.
 */
data class UserData(
    val userId: String,
    val username: String?,
    val profilePictureUrl: String?,
    val email: String?
)
