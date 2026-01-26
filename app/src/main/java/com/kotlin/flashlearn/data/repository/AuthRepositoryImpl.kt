package com.kotlin.flashlearn.data.repository

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import com.fasterxml.uuid.Generators
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.kotlin.flashlearn.BuildConfig
import com.kotlin.flashlearn.data.util.PasswordUtils
import com.kotlin.flashlearn.domain.model.User
import com.kotlin.flashlearn.domain.model.UserData
import com.kotlin.flashlearn.domain.repository.AuthRepository
import com.kotlin.flashlearn.domain.repository.UserRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

@Singleton
class AuthRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val oneTapClient: SignInClient,
    private val auth: FirebaseAuth,
    private val userRepository: UserRepository
) : AuthRepository {

    // In-memory current user for username/password auth (Firebase Auth handles Google users)
    private var currentUserData: UserData? = null

    override suspend fun signIn(): Result<IntentSender?> {
        return runCatching {
            val signInRequest = buildSignInRequest()
            val result = oneTapClient.beginSignIn(signInRequest).await()
            result.pendingIntent.intentSender
        }.onFailure { 
            if (it is CancellationException) throw it 
        }
    }

    override suspend fun signInWithIntent(intent: Intent): Result<UserData> {
        return runCatching {
            val credential = oneTapClient.getSignInCredentialFromIntent(intent)
            val googleCredentials = GoogleAuthProvider.getCredential(credential.googleIdToken, null)
            val authResult = auth.signInWithCredential(googleCredentials).await()
            
            val firebaseUser = authResult.user ?: throw Exception("User is null after sign in")
            android.util.Log.d("GoogleSignIn", "Firebase Google UID: ${firebaseUser.uid}")
            
            // Check if this Google account is linked to an existing user
            android.util.Log.d("GoogleSignIn", "Checking for linked user with googleId: ${firebaseUser.uid}")
            val existingUser = try {
                userRepository.getUserByGoogleId(firebaseUser.uid)
            } catch (e: Exception) {
                android.util.Log.e("GoogleSignIn", "getUserByGoogleId failed: ${e.message}", e)
                null
            }
            android.util.Log.d("GoogleSignIn", "Existing linked user: ${existingUser?.userId}")
            
            if (existingUser != null) {
                android.util.Log.d("GoogleSignIn", "Found linked user! Logging in as: ${existingUser.userId}")
                // Return linked user
                val userData = UserData(
                    userId = existingUser.userId,
                    username = existingUser.displayName,
                    profilePictureUrl = existingUser.photoUrl,
                    email = existingUser.email
                )
                currentUserData = userData
                userData
            } else {
                android.util.Log.d("GoogleSignIn", "No linked user found. Creating new user with UID: ${firebaseUser.uid}")
                // New Google user - create with Firebase UID
                val userData = UserData(
                    userId = firebaseUser.uid,
                    username = firebaseUser.displayName,
                    profilePictureUrl = firebaseUser.photoUrl?.toString(),
                    email = firebaseUser.email
                )
                currentUserData = userData
                userData
            }
        }.onFailure { 
            android.util.Log.e("GoogleSignIn", "signInWithIntent failed: ${it.message}", it)
            if (it is CancellationException) throw it 
        }
    }

    override suspend fun signOut(): Result<Unit> {
        return runCatching {
            oneTapClient.signOut().await()
            auth.signOut()
            currentUserData = null
        }.onFailure { 
            if (it is CancellationException) throw it 
        }
    }

    override fun getSignedInUser(): UserData? {
        // First check in-memory user (for username/password auth)
        currentUserData?.let { return it }
        
        // Fallback to Firebase Auth user (for Google auth)
        val firebaseUser = auth.currentUser ?: return null
        
        return UserData(
            userId = firebaseUser.uid,
            username = firebaseUser.displayName,
            profilePictureUrl = firebaseUser.photoUrl?.toString(),
            email = firebaseUser.email
        )
    }

    override suspend fun registerWithUsername(loginUsername: String, password: String): Result<UserData> {
        return runCatching {
            // Validate password
            val passwordError = PasswordUtils.getPasswordError(password)
            if (passwordError != null) {
                throw Exception(passwordError)
            }
            
            // Check if username is taken
            if (userRepository.isLoginUsernameTaken(loginUsername)) {
                throw Exception("Username already taken")
            }
            
            // Sign in anonymously FIRST to get Firebase UID for security rules
            val anonResult = auth.signInAnonymously().await()
            val anonUid = anonResult.user?.uid
            
            // Generate UUID v7
            val userId = Generators.timeBasedEpochGenerator().generate().toString()
            
            // Create user with hashed password AND anonymous UID
            val user = User(
                userId = userId,
                loginUsername = loginUsername,
                loginPasswordHash = PasswordUtils.hashPassword(password),
                displayName = loginUsername,
                linkedProviders = listOf("password"),
                firebaseUids = if (anonUid != null) listOf(anonUid) else emptyList()
            )
            userRepository.createUser(user)
            
            android.util.Log.d("Auth", "Registered user $userId with Firebase UID: $anonUid")
            
            val userData = UserData(
                userId = userId,
                username = loginUsername,
                profilePictureUrl = null,
                email = null
            )
            currentUserData = userData
            userData
        }.onFailure {
            if (it is CancellationException) throw it
        }
    }

    override suspend fun signInWithUsername(loginUsername: String, password: String): Result<UserData> {
        return runCatching {
            val user = userRepository.getUserByLoginUsername(loginUsername)
                ?: throw Exception("User not found")
            
            if (!PasswordUtils.verifyPassword(password, user.loginPasswordHash ?: "")) {
                throw Exception("Invalid password")
            }
            
            // Sign in anonymously to Firebase for Security Rules
            val anonResult = auth.signInAnonymously().await()
            val anonUid = anonResult.user?.uid
            
            // Store anonymous UID in user document for rule verification
            if (anonUid != null) {
                try {
                    userRepository.addFirebaseUid(user.userId, anonUid)
                    android.util.Log.d("Auth", "Added Firebase anonymous UID: $anonUid to user: ${user.userId}")
                } catch (e: Exception) {
                    android.util.Log.w("Auth", "Failed to store anonymous UID: ${e.message}")
                }
            }
            
            val userData = UserData(
                userId = user.userId,
                username = user.displayName,
                profilePictureUrl = user.photoUrl,
                email = user.email
            )
            currentUserData = userData
            userData
        }.onFailure {
            if (it is CancellationException) throw it
        }
    }

    override suspend fun linkGoogleAccount(): Result<IntentSender?> {
        return signIn() // Reuse the same sign-in flow
    }

    override suspend fun linkGoogleAccountWithIntent(intent: Intent): Result<Unit> {
        return runCatching {
            val currentUser = currentUserData ?: throw Exception("No current user to link")
            
            val credential = oneTapClient.getSignInCredentialFromIntent(intent)
            val googleCredentials = GoogleAuthProvider.getCredential(credential.googleIdToken, null)
            val authResult = auth.signInWithCredential(googleCredentials).await()
            
            val firebaseUser = authResult.user ?: throw Exception("Google sign in failed")
            
            // Check if this Google account is already linked to another user
            // Use new query method that checks the array
            val existingLinkedUser = userRepository.getUserByGoogleId(firebaseUser.uid)
            if (existingLinkedUser != null && existingLinkedUser.userId != currentUser.userId) {
                // If it's the SAME user, we might want to just update info? 
                // Currently throw error if linked to ANOTHER user.
                throw Exception("This Google account is already linked to another user")
            }
            
            // Check if already linked to CURRENT user (prevent duplicates)
            val isAlreadyLinked = currentUserData?.let { 
                // We need to fetch fresh user data to be sure, but for now check repo
                val user = userRepository.getUser(it.userId)
                user?.googleIds?.contains(firebaseUser.uid) == true
            } ?: false
            
            if (isAlreadyLinked) {
                 throw Exception("This Google account is already linked to your account")
            }
            
            // Link Google to current user
            userRepository.linkGoogleAccount(
                userId = currentUser.userId,
                googleId = firebaseUser.uid,
                email = firebaseUser.email ?: ""
            )
            
            // Update currentUserData with new email? 
            // If we have multiple emails, which one is primary? 
            // For now, let's keep the main email as the LAST linked one, or just don't change it if it exists.
            // Let's update it to provide feedback.
            currentUserData = currentUser.copy(
                email = firebaseUser.email
            )
            
            android.util.Log.d("LinkGoogle", "Successfully linked! Email updated to: ${firebaseUser.email}")
            Unit
        }.onFailure {
            android.util.Log.e("LinkGoogle", "Link failed: ${it.message}", it)
            if (it is CancellationException) throw it
        }
    }



    override suspend fun unlinkGoogleAccount(googleId: String): Result<Unit> {
        return runCatching {
            val currentUser = currentUserData ?: throw Exception("Not signed in")
            
            // Unlink in Firestore
            userRepository.unlinkGoogleAccount(currentUser.userId, googleId)
            
            // Sign out from Google/Firebase to clear session IF it matches the current session?
            // If we have multiple accounts, we only sign out if we are currently "using" that credential 
            // or if we just want to be safe.
            // Let's sign out to ensure clean state if the user was using THIS google account.
            // But how do we know?
            // For safety, let's sign out of Firebase Auth if the current Firebase User has this UID.
            if (auth.currentUser?.uid == googleId) {
                try {
                    oneTapClient.signOut().await()
                    auth.signOut()
                } catch (e: Exception) {
                    android.util.Log.w("UnlinkGoogle", "Sign out failed: ${e.message}")
                }
            }
            
            // Update local user data
            // If we removed the email that was set as primary, what do we do?
            // ideally fetch fresh user data.
            // For now, if email matches, clear it? Or leave it? 
            // Let's leave it, ProfileScreen should refresh from Firestore.
            
            Unit
        }
    }

    override fun setCurrentUser(userData: UserData?) {
        currentUserData = userData
    }

    override suspend fun restoreSession(): UserData? {
        // If we already have in-memory user, return it
        currentUserData?.let { return it }
        
        // Check if Firebase Auth has a persisted Google user
        val firebaseUser = auth.currentUser ?: return null
        
        android.util.Log.d("RestoreSession", "Firebase user found: ${firebaseUser.uid}")
        
        // Check if this Google account is linked to a username/password account
        val linkedUser = try {
            userRepository.getUserByGoogleId(firebaseUser.uid)
        } catch (e: Exception) {
            android.util.Log.e("RestoreSession", "Failed to check linked user: ${e.message}")
            null
        }
        
        return if (linkedUser != null) {
            android.util.Log.d("RestoreSession", "Found linked user: ${linkedUser.userId}, username: ${linkedUser.loginUsername}")
            // Use the linked user's data (master username/password account)
            val userData = UserData(
                userId = linkedUser.userId,
                username = linkedUser.loginUsername ?: linkedUser.displayName,
                profilePictureUrl = linkedUser.photoUrl,
                email = linkedUser.email
            )
            currentUserData = userData
            userData
        } else {
            android.util.Log.d("RestoreSession", "No linked user, using Firebase user directly")
            // Pure Google user - use Firebase user data
            val userData = UserData(
                userId = firebaseUser.uid,
                username = firebaseUser.displayName,
                profilePictureUrl = firebaseUser.photoUrl?.toString(),
                email = firebaseUser.email
            )
            currentUserData = userData
            userData
        }
    }

    override suspend fun deleteAccount(): Result<Unit> = runCatching {
        val userId = currentUserData?.userId ?: throw Exception("No user logged in")
        
        // Delete user data from Firestore
        userRepository.deleteUser(userId)
        
        // Try to delete Firebase Auth user if exists
        auth.currentUser?.delete()?.await()
        
        // Clear local session
        currentUserData = null
    }

    override suspend fun changePassword(oldPassword: String, newPassword: String): Result<Unit> = runCatching {
        val userId = currentUserData?.userId ?: throw Exception("No user logged in")
        
        // Get user from Firestore to verify old password
        val user = userRepository.getUser(userId) 
            ?: throw Exception("User not found")
        
        // Verify user has a password (not Google-only account)
        val existingHash = user.loginPasswordHash 
            ?: throw Exception("This account uses Google Sign-In only. No password to change.")
        
        // Verify old password
        if (!com.kotlin.flashlearn.data.util.PasswordUtils.verifyPassword(oldPassword, existingHash)) {
            throw Exception("Current password is incorrect")
        }
        
        // Check new password is different from old
        if (oldPassword == newPassword) {
            throw Exception("New password must be different from current password")
        }
        
        // Validate new password
        if (!com.kotlin.flashlearn.data.util.PasswordUtils.isValidPassword(newPassword)) {
            throw Exception("New password must be 8+ characters with uppercase, lowercase, number, and special character")
        }
        
        // Hash and update new password
        val newHash = com.kotlin.flashlearn.data.util.PasswordUtils.hashPassword(newPassword)
        userRepository.updatePasswordHash(userId, newHash)
    }

    override suspend fun checkPasswordResetEligibility(email: String): Result<String> = runCatching {
        // Query by email
        val user = userRepository.getUserByEmail(email)
            ?: throw Exception("No account found with this email")
            
        // If query succeeded, it means we found a user with this email (either in primary email field)
        // Check if logic requires Google link explicitly? Plan said "if found -> Success".
        // But the previous requirement was "linked to Google".
        // If checking by email, finding the user implies we know the email.
        // But do we need to verify if it's a "Google linked" account?
        // Since we are simulating "Reset link sent", finding the user is enough.
        // BUT, if the user registered with username/password and NO email (which is possible in current system),
        // query by email will fail (User not found), which is correct.
        // If query succeeds, user HAS an email.
        // So we just return the email.
        
        // Confirm the account is actually linked to Google (as per requirement)
        if (user.linkedGoogleAccounts.isEmpty()) {
            throw Exception("This account is not linked to a Google account. Please log in with your password or contact support.")
        }
        
        email
    }

    private fun buildSignInRequest(): BeginSignInRequest {
        return BeginSignInRequest.Builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(BuildConfig.WEB_CLIENT_ID)
                    .build()
            )
            .setAutoSelectEnabled(true)
            .build()
    }
}
