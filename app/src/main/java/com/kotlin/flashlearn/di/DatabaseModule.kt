package com.kotlin.flashlearn.di

import android.content.Context
import androidx.room.Room
import com.kotlin.flashlearn.data.local.FlashLearnDatabase
import com.kotlin.flashlearn.data.local.dao.FlashcardDao
import com.kotlin.flashlearn.data.local.dao.TopicDao
import com.kotlin.flashlearn.data.local.dao.UserProgressDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.kotlin.flashlearn.data.local.dao.DailyWidgetSessionDao
import com.kotlin.flashlearn.data.local.dao.UserStreakDao
import com.kotlin.flashlearn.data.local.dao.WidgetWordHistoryDao

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
    fun provideDailyWidgetSessionDao(database: FlashLearnDatabase): DailyWidgetSessionDao =
        database.dailyWidgetSessionDao()

    @Provides
    @Singleton
    fun provideWidgetWordHistoryDao(database: FlashLearnDatabase): WidgetWordHistoryDao =
        database.widgetWordHistoryDao()

    @Provides
    @Singleton
    fun provideUserStreakDao(database: FlashLearnDatabase): UserStreakDao =
        database.userStreakDao()
}
