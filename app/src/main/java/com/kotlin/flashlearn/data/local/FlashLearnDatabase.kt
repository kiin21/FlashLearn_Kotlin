package com.kotlin.flashlearn.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.kotlin.flashlearn.data.local.Converters
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
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class FlashLearnDatabase : RoomDatabase() {
    abstract fun flashcardDao(): FlashcardDao
    abstract fun topicDao(): TopicDao
    abstract fun userProgressDao(): UserProgressDao
    abstract fun dailyWidgetSessionDao(): DailyWidgetSessionDao
    abstract fun widgetWordHistoryDao(): WidgetWordHistoryDao
    abstract fun userStreakDao(): UserStreakDao

    companion object {
        const val DATABASE_NAME = "flashlearn_db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE flashcards ADD COLUMN pronunciationUrl TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE flashcards ADD COLUMN synonyms TEXT NOT NULL DEFAULT '[]'")
            }
        }
    }
}
