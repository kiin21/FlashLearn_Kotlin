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
    
    // Google linking
    suspend fun getUserByGoogleId(googleId: String): User?
    suspend fun linkGoogleAccount(userId: String, googleId: String, email: String?)
}
