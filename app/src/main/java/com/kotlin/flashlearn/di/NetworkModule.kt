package com.kotlin.flashlearn.di

import com.kotlin.flashlearn.BuildConfig
import com.kotlin.flashlearn.data.remote.DatamuseApi
import com.kotlin.flashlearn.data.remote.NeonSqlApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

/**
 * Hilt Module providing network-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    @Provides
    @Singleton
    @Named("datamuse")
    fun provideDatamuseRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.DATAMUSE_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    @Provides
    @Singleton
    @Named("neon")
    fun provideNeonRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.NEON_SQL_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    @Provides
    @Singleton
    fun provideDatamuseApi(@Named("datamuse") retrofit: Retrofit): DatamuseApi {
        return retrofit.create(DatamuseApi::class.java)
    }
    
    @Provides
    @Singleton
    fun provideNeonSqlApi(@Named("neon") retrofit: Retrofit): NeonSqlApi {
        return retrofit.create(NeonSqlApi::class.java)
    }

    @Provides
    @Singleton
    @Named("dict")
    fun provideDictionaryRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.dictionaryapi.dev/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    @Named("pixabay")
    fun providePixabayRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://pixabay.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun providePixabayApi(@Named("pixabay") retrofit: Retrofit): com.kotlin.flashlearn.data.remote.PixabayApi {
        return retrofit.create(com.kotlin.flashlearn.data.remote.PixabayApi::class.java)
    }

    @Provides
    @Singleton
    fun provideFreeDictionaryApi(@Named("dict") retrofit: Retrofit): com.kotlin.flashlearn.data.remote.FreeDictionaryApi {
        return retrofit.create(com.kotlin.flashlearn.data.remote.FreeDictionaryApi::class.java)
    }
}
