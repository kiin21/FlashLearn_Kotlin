package com.kotlin.flashlearn.domain.repository

import com.kotlin.flashlearn.domain.model.User

/**
 * Repository interface for managing user data.
 */
interface UserRepository {
    suspend fun isNewUser(userId: String): Boolean
    suspend fun createUser(user: User)
    suspend fun getUser(userId: String): User?

    // Username/Password auth
    suspend fun getUserByLoginUsername(loginUsername: String): User?
    suspend fun isLoginUsernameTaken(loginUsername: String): Boolean
    suspend fun getUserByEmail(email: String): User?

    // Google linking
    suspend fun getUserByGoogleId(googleId: String): User?
    suspend fun linkGoogleAccount(userId: String, googleId: String, email: String)
    suspend fun unlinkGoogleAccount(userId: String, googleId: String)
    suspend fun updateEmail(userId: String, email: String)
    suspend fun uploadProfilePicture(userId: String, uriString: String): String
    suspend fun deleteUser(userId: String)
    suspend fun updatePasswordHash(userId: String, newPasswordHash: String)

    // Firebase Anonymous Auth UID storage
    suspend fun addFirebaseUid(userId: String, firebaseUid: String)

    // Topic Likes/Favorites (Home Screen bookmarks)
    suspend fun toggleTopicLike(userId: String, topicId: String, isLiked: Boolean)
}
