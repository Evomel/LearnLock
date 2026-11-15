package com.learnlock.app.lock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.learnlock.app.LearnLockApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val app = context.applicationContext as? LearnLockApp ?: return
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
