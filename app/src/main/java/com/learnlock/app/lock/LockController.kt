package com.learnlock.app.lock

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.learnlock.app.LearnLockApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Центральная точка управления блокировкой.
 * Стартует/останавливает мониторинг и поднимает [LockActivity].
 */
object LockController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Запускает foreground-сервис, слушающий пробуждение экрана. */
    fun startMonitoring(context: Context) {
        val app = context.applicationContext
        val intent = Intent(app, LockMonitorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            app.startForegroundService(intent)
        } else {
            app.startService(intent)
        }
    }

    fun stopMonitoring(context: Context) {
        context.stopService(Intent(context, LockMonitorService::class.java))
    }

    /**
     * Вызывается при SCREEN_ON / USER_PRESENT.
     * Показывает задачу только если [LearnLockRepository.shouldPresentLock] = true.
     */
    fun onScreenAwake(context: Context) {
        val appContext = context.applicationContext
        val app = appContext as? LearnLockApp ?: return
        scope.launch {
            if (app.container.repository.shouldPresentLock()) {
                presentLock(appContext)
            }
        }
    }

    /**
     * Открывает экран задачи поверх приложений.
     * @param forcePreview true — показать задачу даже при активной сессии (кнопка проверки)
     */
    fun presentLock(context: Context, forcePreview: Boolean = false) {
        val intent = Intent(context, LockActivity::class.java).apply {
            putExtra(LockActivity.EXTRA_FORCE_PREVIEW, forcePreview)
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_NO_USER_ACTION
            )
        }
        context.startActivity(intent)
    }

    /** Есть ли разрешение «поверх других окон» (задел; MVP стартует Activity). */
    fun canDrawOverlays(context: Context): Boolean =
        Settings.canDrawOverlays(context)
}
