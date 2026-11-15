package com.learnlock.app.lock

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.learnlock.app.LearnLockApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Lightweight helper: if lock should be active and another app comes to foreground,
 * re-present the task screen. Does not read window content.
 */
class LearnLockAccessibilityService : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return
        val app = application as? LearnLockApp ?: return
        scope.launch {
            if (app.container.repository.shouldPresentLock()) {
                LockController.presentLock(this@LearnLockAccessibilityService)
            }
        }
    }

    override fun onInterrupt() = Unit
}
