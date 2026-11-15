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

object LockController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

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

    fun onScreenAwake(context: Context) {
        val appContext = context.applicationContext
        val app = appContext as? LearnLockApp ?: return
        scope.launch {
            if (app.container.repository.shouldPresentLock()) {
                presentLock(appContext)
            }
        }
    }

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

    fun canDrawOverlays(context: Context): Boolean =
        Settings.canDrawOverlays(context)
}
