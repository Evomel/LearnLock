package com.learnlock.app.lock

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

/**
 * Заготовка Device Admin (опциональное усиление защиты).
 * В MVP callbacks пустые — админ не обязателен для работы блокировки.
 */
class LearnLockDeviceAdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) = Unit
    override fun onDisabled(context: Context, intent: Intent) = Unit
}
