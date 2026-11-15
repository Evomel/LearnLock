package com.learnlock.app.data

import android.content.Context
import com.learnlock.taskengine.TaskEngine

interface AppContainer {
    val database: AppDatabase
    val sessionStore: SessionStore
    val repository: LearnLockRepository
    val taskEngine: TaskEngine
}

class DefaultAppContainer(context: Context) : AppContainer {
    override val database: AppDatabase = AppDatabase.create(context.applicationContext)
    override val sessionStore: SessionStore = SessionStore(context.applicationContext)
    override val taskEngine: TaskEngine = TaskEngine()
    override val repository: LearnLockRepository = LearnLockRepository(
        profileDao = database.profileDao(),
        parentAuthDao = database.parentAuthDao(),
        statsDao = database.statsDao(),
        sessionStore = sessionStore,
        taskEngine = taskEngine
    )
}
