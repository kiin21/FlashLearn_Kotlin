package com.kotlin.flashlearn.di

import android.content.Context
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.kotlin.flashlearn.data.remote.DatamuseApi
import com.kotlin.flashlearn.data.repository.AuthRepositoryImpl
import com.kotlin.flashlearn.data.repository.DatamuseRepositoryImpl
import com.kotlin.flashlearn.data.repository.FlashcardRepositoryImpl
import com.kotlin.flashlearn.data.repository.TopicRepositoryImpl
import com.kotlin.flashlearn.data.repository.UserRepositoryImpl
import com.kotlin.flashlearn.data.repository.FavoriteRepositoryImpl
import com.kotlin.flashlearn.domain.repository.AuthRepository
import com.kotlin.flashlearn.domain.repository.DatamuseRepository
import com.kotlin.flashlearn.domain.repository.FavoriteRepository
import com.kotlin.flashlearn.domain.repository.FlashcardRepository
import com.kotlin.flashlearn.domain.repository.TopicRepository
import com.kotlin.flashlearn.domain.repository.UserRepository
import com.kotlin.flashlearn.data.local.dao.UserProgressDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

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
        auth: FirebaseAuth,
        userRepository: UserRepository
    ): AuthRepository = AuthRepositoryImpl(context, oneTapClient, auth, userRepository)


    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        val firestore = Firebase.firestore
        val settings = com.google.firebase.firestore.firestoreSettings {
            setLocalCacheSettings(
                com.google.firebase.firestore.persistentCacheSettings {
                    setSizeBytes(100 * 1024 * 1024L)
                }
            )
        }
        firestore.firestoreSettings = settings
        return firestore
    }

    @Provides
    @Singleton
    fun provideCloudinaryService(): com.kotlin.flashlearn.data.remote.CloudinaryService = 
        com.kotlin.flashlearn.data.remote.CloudinaryService()

    @Provides
    @Singleton
    fun provideUserRepository(
        firestore: FirebaseFirestore,
        cloudinaryService: com.kotlin.flashlearn.data.remote.CloudinaryService
    ): UserRepository = UserRepositoryImpl(firestore, cloudinaryService)

    @Provides
    @Singleton
    fun provideFlashcardRepository(
        firestore: FirebaseFirestore,
        datamuseApi: DatamuseApi,
        topicRepository: TopicRepository,
        freeDictionaryApi: com.kotlin.flashlearn.data.remote.FreeDictionaryApi,
        pixabayApi: com.kotlin.flashlearn.data.remote.PixabayApi,
        userProgressDao: UserProgressDao
    ): FlashcardRepository = FlashcardRepositoryImpl(
        firestore,
        datamuseApi,
        topicRepository,
        freeDictionaryApi,
        pixabayApi,
        userProgressDao
    )

    @Provides
    @Singleton
    fun provideTopicRepository(
        firestore: FirebaseFirestore,
        pixabayApi: com.kotlin.flashlearn.data.remote.PixabayApi
    ): TopicRepository = TopicRepositoryImpl(firestore, pixabayApi)

    @Provides
    @Singleton
    fun provideDatamuseRepository(
        datamuseApi: DatamuseApi
    ): DatamuseRepository = DatamuseRepositoryImpl(datamuseApi)

    @Provides
    @Singleton
    fun provideFavoriteRepository(
        firestore: FirebaseFirestore
    ): FavoriteRepository = FavoriteRepositoryImpl(firestore)
    @Provides
    @Singleton
    fun provideLanguageManager(
        @ApplicationContext context: Context
    ): com.kotlin.flashlearn.util.LanguageManager = com.kotlin.flashlearn.util.LanguageManager(context)
}
