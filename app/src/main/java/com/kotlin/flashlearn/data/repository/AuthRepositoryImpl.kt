package com.kotlin.flashlearn.data.repository

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.kotlin.flashlearn.R
import com.kotlin.flashlearn.domain.model.UserData
import com.kotlin.flashlearn.domain.repository.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/**
 * Implementation of AuthRepository using Google One Tap Sign-In and Firebase Auth.
 * Following Repository pattern from Clean Architecture.
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val oneTapClient: SignInClient,
    private val auth: FirebaseAuth
) : AuthRepository {

    override suspend fun signIn(): Result<IntentSender?> {
        return try {
            val result = oneTapClient.beginSignIn(buildSignInRequest()).await()
            Result.success(result.pendingIntent.intentSender)
        } catch (e: Exception) {
            e.printStackTrace()
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun signInWithIntent(intent: Intent): Result<UserData> {
        return try {
            val credential = oneTapClient.getSignInCredentialFromIntent(intent)
            val googleIdToken = credential.googleIdToken
            val googleCredentials = GoogleAuthProvider.getCredential(googleIdToken, null)
            val user = auth.signInWithCredential(googleCredentials).await().user

            user?.let {
                Result.success(
                    UserData(
                        userId = it.uid,
                        username = it.displayName,
                        profilePictureUrl = it.photoUrl?.toString(),
                        email = it.email
                    )
                )
            } ?: Result.failure(Exception("User is null after sign in"))
        } catch (e: Exception) {
            e.printStackTrace()
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun signOut(): Result<Unit> {
        return try {
            oneTapClient.signOut().await()
            auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override fun getSignedInUser(): UserData? {
        return auth.currentUser?.let { user ->
            UserData(
                userId = user.uid,
                username = user.displayName,
                profilePictureUrl = user.photoUrl?.toString(),
                email = user.email
            )
        }
    }

    private fun buildSignInRequest(): BeginSignInRequest {
        return BeginSignInRequest.Builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(com.kotlin.flashlearn.BuildConfig.WEB_CLIENT_ID)
                    .build()
            )
            .setAutoSelectEnabled(true)
            .build()
    }
}
