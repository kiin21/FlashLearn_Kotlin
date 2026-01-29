package com.kotlin.flashlearn.domain.model

/**
 * Domain model representing a user in the system.
 */
data class User(
    val userId: String = "",
    val loginUsername: String? = null,
    val loginPasswordHash: String? = null,

    val displayName: String? = null,
    val email: String? = null,
    val photoUrl: String? = null,

    val googleIds: List<String> = emptyList(), // For backend lookup
    val linkedGoogleAccounts: List<LinkedAccount> = emptyList(), // For UI info
    val firebaseUids: List<String> = emptyList(), // Anonymous Firebase Auth UIDs for security rules
    val streak: Int = 0,
    val examDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val likedTopicIds: List<String> = emptyList() // IDs of favorite topics for quick access
)

data class LinkedAccount(
    val providerId: String = "google.com",
    val accountId: String = "", // Google UID
    val email: String = "",
    val displayName: String? = null
)
