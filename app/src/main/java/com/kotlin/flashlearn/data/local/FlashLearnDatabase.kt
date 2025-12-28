package com.kotlin.flashlearn.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.kotlin.flashlearn.data.local.dao.FlashcardDao
import com.kotlin.flashlearn.data.local.dao.TopicDao
import com.kotlin.flashlearn.data.local.dao.UserProgressDao
import com.kotlin.flashlearn.data.local.entity.FlashcardEntity
import com.kotlin.flashlearn.data.local.entity.TopicEntity
import com.kotlin.flashlearn.data.local.entity.UserProgressEntity

/**
 * Room Database for FlashLearn app.
 * Provides offline-first caching for topics, flashcards, and user progress.
 */
@Database(
    entities = [
        FlashcardEntity::class,
        TopicEntity::class,
        UserProgressEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class FlashLearnDatabase : RoomDatabase() {
    abstract fun flashcardDao(): FlashcardDao
    abstract fun topicDao(): TopicDao
    abstract fun userProgressDao(): UserProgressDao

    companion object {
        const val DATABASE_NAME = "flashlearn_db"
    }
}
