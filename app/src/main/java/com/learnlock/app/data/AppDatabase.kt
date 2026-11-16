package com.learnlock.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

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
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun parentAuthDao(): ParentAuthDao
    abstract fun statsDao(): StatsDao

    companion object {
        /** Создаёт singleton-подобную БД `learnlock.db` в application context. */
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "learnlock.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}
