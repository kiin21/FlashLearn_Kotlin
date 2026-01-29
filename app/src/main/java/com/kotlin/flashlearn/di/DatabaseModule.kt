package com.kotlin.flashlearn.di

import android.content.Context
import androidx.room.Room
import com.kotlin.flashlearn.data.local.FlashLearnDatabase
import com.kotlin.flashlearn.data.local.dao.FlashcardDao
import com.kotlin.flashlearn.data.local.dao.TopicDao
import com.kotlin.flashlearn.data.local.dao.UserProgressDao
import com.kotlin.flashlearn.data.local.dao.UserStreakDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): FlashLearnDatabase = Room.databaseBuilder(
        context,
        FlashLearnDatabase::class.java,
        FlashLearnDatabase.DATABASE_NAME
    )
        .addMigrations(FlashLearnDatabase.MIGRATION_1_2)
        .fallbackToDestructiveMigration()
        .build()

    @Provides
    @Singleton
    fun provideFlashcardDao(database: FlashLearnDatabase): FlashcardDao =
        database.flashcardDao()

    @Provides
    @Singleton
    fun provideTopicDao(database: FlashLearnDatabase): TopicDao =
        database.topicDao()

    @Provides
    @Singleton
    fun provideUserProgressDao(database: FlashLearnDatabase): UserProgressDao =
        database.userProgressDao()

    @Provides
    @Singleton
    fun provideUserStreakDao(database: FlashLearnDatabase): UserStreakDao =
        database.userStreakDao()

    @Provides
    @Singleton
    fun provideDailyWordDao(database: FlashLearnDatabase) = database.dailyWordDao()
}
