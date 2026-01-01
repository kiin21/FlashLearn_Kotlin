package com.kotlin.flashlearn.data.repository

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.kotlin.flashlearn.BuildConfig
import com.kotlin.flashlearn.domain.model.UserData
import com.kotlin.flashlearn.domain.repository.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

@Singleton
class AuthRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val oneTapClient: SignInClient,
    private val auth: FirebaseAuth
) : AuthRepository {

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
            
            val user = authResult.user ?: throw Exception("User is null after sign in")
            
            UserData(
                userId = user.uid,
                username = user.displayName,
                profilePictureUrl = user.photoUrl?.toString(),
                email = user.email
            )
        }.onFailure { 
            if (it is CancellationException) throw it 
        }
    }

    override suspend fun signOut(): Result<Unit> {
        return runCatching {
            oneTapClient.signOut().await()
            auth.signOut()
        }.onFailure { 
            if (it is CancellationException) throw it 
        }
    }

    override fun getSignedInUser(): UserData? {
        val user = auth.currentUser ?: return null
        
        return UserData(
            userId = user.uid,
            username = user.displayName,
            profilePictureUrl = user.photoUrl?.toString(),
            email = user.email
        )
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
