package com.learnlock.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Локальная Room-БД LearnLock.
 * Хранит профиль ребёнка, родительский PIN и историю попыток.
 *
 * При смене схемы используется destructive migration (MVP).
 */
@Database(
    entities = [
        ChildProfileEntity::class,
        ParentAuthEntity::class,
        TaskAttemptEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun parentAuthDao(): ParentAuthDao
    abstract fun statsDao(): StatsDao

    companion object {
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE child_profile ADD COLUMN backgroundColor INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE child_profile ADD COLUMN launchIntervalMinutes INTEGER NOT NULL DEFAULT 5")
                database.execSQL("ALTER TABLE child_profile ADD COLUMN accessDurationMinutes INTEGER NOT NULL DEFAULT 5")
                database.execSQL("UPDATE child_profile SET launchIntervalMinutes = sessionMinutes, accessDurationMinutes = sessionMinutes")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE parent_auth ADD COLUMN failedAttempts INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE parent_auth ADD COLUMN lockedUntilMillis INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** Создаёт singleton-подобную БД `learnlock.db` в application context. */
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "learnlock.db")
                .addMigrations(MIGRATION_2_3)
                .addMigrations(MIGRATION_3_4)
                .addMigrations(MIGRATION_4_5)
                .fallbackToDestructiveMigration()
                .build()
    }
}
