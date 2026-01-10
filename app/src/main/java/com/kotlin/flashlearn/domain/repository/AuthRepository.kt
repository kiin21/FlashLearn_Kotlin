package com.kotlin.flashlearn.domain.repository

import android.content.Intent
import android.content.IntentSender
import com.kotlin.flashlearn.domain.model.UserData

/**
 * Repository interface for authentication operations.
 * Follows Clean Architecture - domain layer defines the contract,
 * data layer provides the implementation.
 */
interface AuthRepository {
    /**
     * Initiates the Google Sign-In flow.
     * @return IntentSender to launch the sign-in UI, or null if failed.
     */
    suspend fun signIn(): Result<IntentSender?>

    /**
     * Completes the sign-in process with the result intent.
     * @param intent The result intent from the sign-in UI.
     * @return UserData on success, or error on failure.
     */
    suspend fun signInWithIntent(intent: Intent): Result<UserData>

    /**
     * Signs out the current user.
     */
    suspend fun signOut(): Result<Unit>

    /**
     * Gets the currently signed-in user, if any.
     * @return UserData if signed in, null otherwise.
     */
    fun getSignedInUser(): UserData?
    
    // Username/Password Authentication
    suspend fun registerWithUsername(loginUsername: String, password: String): Result<UserData>
    suspend fun signInWithUsername(loginUsername: String, password: String): Result<UserData>
    
    // Account Linking
    suspend fun linkGoogleAccount(): Result<IntentSender?>
    suspend fun linkGoogleAccountWithIntent(intent: Intent): Result<Unit>
    suspend fun unlinkGoogleAccount(googleId: String): Result<Unit>
    fun getLinkedProviders(): List<String>
    
    // Session management for custom auth
    fun setCurrentUser(userData: UserData?)
    
    /**
     * Restores session on app start. Checks if Firebase Google user has a linked account.
     * @return UserData if session restored, null otherwise.
     */
    suspend fun restoreSession(): UserData?
    
    /**
     * Deletes the current user's account and all associated data.
     */
    suspend fun deleteAccount(): Result<Unit>
    
    /**
     * Changes the password for the current user (custom auth only).
     * @param oldPassword The current password for verification.
     * @param newPassword The new password to set.
     */
    suspend fun changePassword(oldPassword: String, newPassword: String): Result<Unit>
}
