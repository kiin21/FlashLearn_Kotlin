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
            
            // Check if this Google account is linked to an existing user
            val existingUser = userRepository.getUserByGoogleId(firebaseUser.uid)
            
            if (existingUser != null) {
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
            
            // Generate UUID v7
            val userId = Generators.timeBasedEpochGenerator().generate().toString()
            
            // Create user with hashed password
            val user = User(
                userId = userId,
                loginUsername = loginUsername,
                loginPasswordHash = PasswordUtils.hashPassword(password),
                displayName = loginUsername,
                linkedProviders = listOf("password")
            )
            userRepository.createUser(user)
            
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
            val existingLinkedUser = userRepository.getUserByGoogleId(firebaseUser.uid)
            if (existingLinkedUser != null && existingLinkedUser.userId != currentUser.userId) {
                throw Exception("This Google account is already linked to another user")
            }
            
            // Link Google to current user
            userRepository.linkGoogleAccount(
                userId = currentUser.userId,
                googleId = firebaseUser.uid,
                email = firebaseUser.email
            )
        }.onFailure {
            if (it is CancellationException) throw it
        }
    }

    override fun getLinkedProviders(): List<String> {
        // For Firebase Auth users
        val firebaseProviders = auth.currentUser?.providerData?.map { it.providerId } ?: emptyList()
        if (firebaseProviders.isNotEmpty()) {
            return firebaseProviders.filter { it != "firebase" }
        }
        
        // For username/password users, we'd need to check Firestore
        // This would require making it a suspend function or caching the user
        return emptyList()
    }

    override fun setCurrentUser(userData: UserData?) {
        currentUserData = userData
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
