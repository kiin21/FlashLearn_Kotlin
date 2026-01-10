package com.kotlin.flashlearn.data.util

import org.mindrot.jbcrypt.BCrypt

/**
 * Utility object for password hashing and validation.
 */
object PasswordUtils {
    
    /**
     * Hash a password using BCrypt.
     */
    fun hashPassword(password: String): String {
        return BCrypt.hashpw(password, BCrypt.gensalt())
    }
    
    /**
     * Verify a password against a BCrypt hash.
     */
    fun verifyPassword(password: String, hash: String): Boolean {
        return try {
            BCrypt.checkpw(password, hash)
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Validate password requirements:
     * - At least 8 characters
     * - At least one uppercase letter
     * - At least one lowercase letter
     * - At least one digit
     * - At least one special character
     */
    fun isValidPassword(password: String): Boolean {
        if (password.length < 8) return false
        if (!password.any { it.isUpperCase() }) return false
        if (!password.any { it.isLowerCase() }) return false
        if (!password.any { it.isDigit() }) return false
        if (!password.any { !it.isLetterOrDigit() }) return false
        return true
    }
    
    /**
     * Get password validation error message, or null if valid.
     */
    fun getPasswordError(password: String): String? {
        return when {
            password.length < 8 -> "Password must be at least 8 characters"
            !password.any { it.isUpperCase() } -> "Password must contain an uppercase letter"
            !password.any { it.isLowerCase() } -> "Password must contain a lowercase letter"
            !password.any { it.isDigit() } -> "Password must contain a number"
            !password.any { !it.isLetterOrDigit() } -> "Password must contain a special character"
            else -> null
        }
    }
}
