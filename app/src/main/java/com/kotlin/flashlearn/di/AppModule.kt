package com.kotlin.flashlearn.di

import android.content.Context
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.kotlin.flashlearn.data.repository.AuthRepositoryImpl
import com.kotlin.flashlearn.domain.repository.AuthRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt Module providing application-level dependencies.
 * Following Dependency Injection best practices from Google MAD.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSignInClient(
        @ApplicationContext context: Context
    ): SignInClient = Identity.getSignInClient(context)

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = Firebase.auth

    @Provides
    @Singleton
    fun provideAuthRepository(
        @ApplicationContext context: Context,
        oneTapClient: SignInClient,
        auth: FirebaseAuth
    ): AuthRepository = AuthRepositoryImpl(context, oneTapClient, auth)
}
