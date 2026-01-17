package com.kotlin.flashlearn.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.kotlin.flashlearn.data.local.dao.FlashcardDao
import com.kotlin.flashlearn.data.local.dao.TopicDao
import com.kotlin.flashlearn.data.local.dao.UserProgressDao
import com.kotlin.flashlearn.data.local.dao.DailyWidgetSessionDao
import com.kotlin.flashlearn.data.local.dao.WidgetWordHistoryDao
import com.kotlin.flashlearn.data.local.dao.UserStreakDao
import com.kotlin.flashlearn.data.local.entity.FlashcardEntity
import com.kotlin.flashlearn.data.local.entity.TopicEntity
import com.kotlin.flashlearn.data.local.entity.UserProgressEntity
import com.kotlin.flashlearn.data.local.entity.DailyWidgetSessionEntity
import com.kotlin.flashlearn.data.local.entity.WidgetWordHistoryEntity
import com.kotlin.flashlearn.data.local.entity.UserStreakEntity

@Database(
    entities = [
        FlashcardEntity::class,
        TopicEntity::class,
        UserProgressEntity::class,
        DailyWidgetSessionEntity::class,
        WidgetWordHistoryEntity::class,
        UserStreakEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class FlashLearnDatabase : RoomDatabase() {
    abstract fun flashcardDao(): FlashcardDao
    abstract fun topicDao(): TopicDao
    abstract fun userProgressDao(): UserProgressDao
    abstract fun dailyWidgetSessionDao(): DailyWidgetSessionDao
    abstract fun widgetWordHistoryDao(): WidgetWordHistoryDao
    abstract fun userStreakDao(): UserStreakDao

    companion object {
        const val DATABASE_NAME = "flashlearn_db"
    }
}
