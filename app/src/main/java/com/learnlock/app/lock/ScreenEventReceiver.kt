package com.learnlock.app.lock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ScreenEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_USER_PRESENT || action == Intent.ACTION_SCREEN_ON) {
            LockController.onScreenAwake(context)
        }
    }
}
