package com.kotlin.flashlearn.domain.model

/**
 * Domain model representing a user in the system.
 */
data class User(
    val userId: String = "",
    val loginUsername: String? = null,
    val loginPasswordHash: String? = null,
    val googleId: String? = null,
    val displayName: String? = null,
    val email: String? = null,
    val photoUrl: String? = null,
    val linkedProviders: List<String> = emptyList(),
    val streak: Int = 0,
    val examDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
