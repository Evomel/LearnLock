package com.learnlock.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.learnlock.app.data.AppContainer
import com.learnlock.app.data.DefaultAppContainer

/**
 * Точка входа приложения.
 * Держит [AppContainer] с зависимостями (БД, репозиторий, движок задач)
 * и канал уведомлений для foreground-сервиса блокировки.
 */
class LearnLockApp : Application() {
    /** DI-контейнер, доступен из Activity/Service через `application as LearnLockApp`. */
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
        createNotificationChannel()
    }

    /** Канал для ongoing-уведомления [com.learnlock.app.lock.LockMonitorService]. */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            LOCK_CHANNEL_ID,
            getString(R.string.lock_notification_channel),
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
        manager.createNotificationChannel(
            NotificationChannel(
                LAUNCH_CHANNEL_ID,
                "Запуск задания",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { setSound(null, null) }
        )
    }

    companion object {
        const val LOCK_CHANNEL_ID = "learnlock_session"
        const val LAUNCH_CHANNEL_ID = "learnlock_launch"
    }
}
