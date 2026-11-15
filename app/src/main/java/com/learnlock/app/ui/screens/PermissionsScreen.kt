package com.learnlock.app.ui.screens

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.learnlock.app.lock.LearnLockAccessibilityService
import com.learnlock.app.lock.LockController
import com.learnlock.app.ui.components.LlBackground
import com.learnlock.app.ui.components.LlGhostButton
import com.learnlock.app.ui.components.LlPanel
import com.learnlock.app.ui.components.LlPrimaryButton
import com.learnlock.app.ui.theme.LlColors

@Composable
fun PermissionsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var overlay by remember { mutableStateOf(LockController.canDrawOverlays(context)) }
    var accessibility by remember { mutableStateOf(isAccessibilityEnabled(context)) }
    var batteryIgnored by remember { mutableStateOf(isIgnoringBattery(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                overlay = LockController.canDrawOverlays(context)
                accessibility = isAccessibilityEnabled(context)
                batteryIgnored = isIgnoringBattery(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LlBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Права Android", style = MaterialTheme.typography.displayMedium, color = LlColors.MintDeep)
            Text(
                "Нужны, чтобы задача появлялась после сна и поверх приложений.",
                style = MaterialTheme.typography.bodyMedium,
                color = LlColors.TextSecondary
            )

            LlPanel {
                Text("Поверх окон: ${status(overlay)}", style = MaterialTheme.typography.titleMedium)
                LlPrimaryButton(
                    text = "Открыть overlay",
                    onClick = {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                        )
                    }
                )

                Text("Accessibility: ${status(accessibility)}", style = MaterialTheme.typography.titleMedium)
                LlPrimaryButton(
                    text = "Открыть Accessibility",
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                )

                Text("Батарея: ${status(batteryIgnored)}", style = MaterialTheme.typography.titleMedium)
                LlPrimaryButton(
                    text = "Открыть батарею",
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            context.startActivity(
                                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                            )
                        }
                    }
                )
            }

            Text(
                "На Xiaomi/Huawei/Samsung автозапуск часто нужно включить вручную.",
                style = MaterialTheme.typography.bodySmall,
                color = LlColors.TextSecondary
            )
            LlGhostButton("Назад", onClick = onBack)
        }
    }
}

private fun status(ok: Boolean): String = if (ok) "ок" else "нужно включить"

private fun isAccessibilityEnabled(context: Context): Boolean {
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    if (!am.isEnabled) return false
    val expected = ComponentName(context, LearnLockAccessibilityService::class.java).flattenToString()
    val enabled = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
}

private fun isIgnoringBattery(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}
