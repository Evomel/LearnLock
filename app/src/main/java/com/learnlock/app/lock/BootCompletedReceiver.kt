package com.learnlock.app.lock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.learnlock.app.LearnLockApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * После перезагрузки телефона восстанавливает мониторинг,
 * если в профиле включена блокировка, и сразу проверяет,
 * не нужно ли показать задачу.
 */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val app = context.applicationContext as? LearnLockApp ?: return
        // Продлеваем жизнь receiver, пока идёт suspend-чтение профиля.
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val profile = app.container.repository.getProfileOrDefault()
                if (profile.lockEnabled) {
                    LockController.startMonitoring(context)
                    LockController.onScreenAwake(context)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
